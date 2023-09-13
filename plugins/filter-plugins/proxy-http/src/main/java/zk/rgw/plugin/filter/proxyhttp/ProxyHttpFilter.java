/*
 * Copyright 2023 zoukang, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package zk.rgw.plugin.filter.proxyhttp;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import zk.rgw.common.exception.RgwException;
import zk.rgw.common.exception.RgwRuntimeException;
import zk.rgw.common.util.EnvNameExtractUtil;
import zk.rgw.common.util.ObjectUtil;
import zk.rgw.plugin.api.Exchange;
import zk.rgw.plugin.api.filter.FilterChain;
import zk.rgw.plugin.api.filter.JsonConfFilterPlugin;
import zk.rgw.plugin.exception.PluginConfException;
import zk.rgw.plugin.util.ExchangeUtil;
import zk.rgw.plugin.util.ResponseUtil;

@Slf4j
public class ProxyHttpFilter implements JsonConfFilterPlugin {

    private static final String ENGINE_NAME = "groovy";

    private static final String FUNC_NAME = "decideUri";

    private static final HttpClient HTTP_CLIENT = HttpClient.create();

    @Getter
    @Setter
    private HttpClient httpClient = HTTP_CLIENT;

    private final ProxyConf proxyConf = new ProxyConf();

    private URI uri;

    private List<String> envNames;

    private ScriptEngine scriptEngine;

    @Override
    public void configure(String conf) throws PluginConfException {
        try {
            OM.readerForUpdating(this.proxyConf).readValue(conf);
        } catch (IOException ioException) {
            throw new PluginConfException(ioException.getMessage(), ioException);
        }

        if (proxyConf.timeout <= 0) {
            proxyConf.timeout = Integer.MAX_VALUE;
        }

        if (!ObjectUtil.isEmpty(proxyConf.upstreamEndpoint)) {
            envNames = EnvNameExtractUtil.extract(proxyConf.upstreamEndpoint);

            if (envNames.isEmpty() && Objects.nonNull(proxyConf.getUpstreamEndpoint())) {
                try {
                    uri = new URI(proxyConf.getUpstreamEndpoint());
                } catch (URISyntaxException exception) {
                    String message = "Failed to parse " + proxyConf.getUpstreamEndpoint() + " to URI";
                    throw new PluginConfException(message, exception);
                }
            }
        } else if (!ObjectUtil.isEmpty(proxyConf.upstreamEndpointDecideFuncDef)) {
            this.scriptEngine = new ScriptEngineManager().getEngineByName(ENGINE_NAME);
            try {
                scriptEngine.eval(proxyConf.upstreamEndpointDecideFuncDef);
            } catch (ScriptException exception) {
                String msg = "HTTP协议代理插件配置失败：解析动态设置上游uri的groovy方法定义失败";
                log.error("{}", msg, exception);
                throw new PluginConfException(msg, exception);
            }
        } else {
            throw new PluginConfException("未定义合适的上游URI");
        }
    }

    @Override
    public Mono<Void> filter(Exchange exchange, FilterChain chain) {
        HttpServerRequest request = exchange.getRequest();

        URI uriToUse;
        try {
            uriToUse = decideUpstreamUri(exchange);
        } catch (RgwException rgwException) {
            return ResponseUtil.send(exchange.getResponse(), rgwException.getMessage());
        }

        return this.httpClient.headers(headers -> {
            headers.add(request.requestHeaders());
            headers.remove(HttpHeaderNames.HOST);
        }).request(request.method()).uri(uriToUse).send(request.receive().retain()).responseConnection((httpClientResponse, connection) -> {
            HttpServerResponse serverResponse = exchange.getResponse();
            return serverResponse.status(httpClientResponse.status())
                    .headers(httpClientResponse.responseHeaders())
                    .send(connection.inbound().receive().retain())
                    .then()
                    .thenReturn("");
        }).timeout(
                Duration.ofSeconds(proxyConf.timeout),
                Mono.defer(() -> ResponseUtil.sendStatus(exchange.getResponse(), HttpResponseStatus.GATEWAY_TIMEOUT).thenReturn(""))
        ).then();
    }

    private URI decideUpstreamUri(Exchange exchange) throws RgwException {
        if (Objects.nonNull(uri)) {
            // 固定的uri
            return uri;
        } else if (ObjectUtil.isEmpty(proxyConf.upstreamEndpointDecideFuncDef)) {
            // 含有环境变量的uri
            return decideUpstreamUriByEnv(exchange);
        } else {
            // 由用户配置的groovy脚本动态决定uri, 用户可以自定义
            return decideUpstreamUriByScript(exchange);
        }
    }

    private URI decideUpstreamUriByEnv(Exchange exchange) throws RgwException {
        String uriStr = proxyConf.upstreamEndpoint;
        Map<String, String> environment = ExchangeUtil.getEnvironment(exchange);
        for (String envName : envNames) {
            String value = environment.get(envName);
            if (Objects.isNull(value)) {
                throw new RgwException("ProxyHttpFilter error: required environment variable named " + envName + " not found.");
            }
            uriStr = uriStr.replace("{" + envName + "}", value);
        }

        try {
            return new URI(uriStr);
        } catch (URISyntaxException exception) {
            String message = "ProxyHttpFilter error: failed to parse " + uriStr + " to URI";
            throw new RgwException(message);
        }
    }

    private URI decideUpstreamUriByScript(Exchange exchange) throws RgwException {
        Map<String, String> requestHeaders = new HashMap<>();
        exchange.getRequest().requestHeaders().forEach(entry -> requestHeaders.put(entry.getKey(), entry.getValue()));
        scriptEngine.put("requestHeaders", requestHeaders);

        scriptEngine.put("clientRequestUriPath", exchange.getRequest().uri());

        scriptEngine.put("clientRequestUriParameters", ExchangeUtil.getQueryParams(exchange));

        Map<String, String> environment = ExchangeUtil.getEnvironment(exchange);
        scriptEngine.put("environment", Map.copyOf(environment));

        Object result;

        try {
            result = ((Invocable) scriptEngine).invokeFunction(FUNC_NAME);
        } catch (Exception exception) {
            String msg = "HTTP协议代理异常：执行动态设置上游uri的groovy方法异常";
            log.error("{}", msg, exception);
            throw new RgwRuntimeException(msg);
        }

        if (result instanceof URI resultUri) {
            return resultUri;
        } else {
            String msg = "HTTP协议代理异常：动态设置上游uri的groovy方法返回结果不是一个URI对象";
            throw new RgwException(msg);
        }
    }

    @Getter
    @Setter
    public static class ProxyConf {

        private String upstreamEndpoint;

        private String upstreamEndpointDecideFuncDef;

        private int timeout = Integer.MAX_VALUE;

    }

}
