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
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.Getter;
import lombok.Setter;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import zk.rgw.common.util.EnvNameExtractUtil;
import zk.rgw.plugin.api.Exchange;
import zk.rgw.plugin.api.filter.FilterChain;
import zk.rgw.plugin.api.filter.JsonConfFilterPlugin;
import zk.rgw.plugin.exception.PluginConfException;
import zk.rgw.plugin.util.ExchangeUtil;
import zk.rgw.plugin.util.ResponseUtil;

public class ProxyHttpFilter implements JsonConfFilterPlugin {

    private static final HttpClient HTTP_CLIENT = HttpClient.create();

    @Getter
    @Setter
    private HttpClient httpClient = HTTP_CLIENT;

    private final ProxyConf proxyConf = new ProxyConf();

    private URI uri;

    private List<String> envNames;

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

        envNames = EnvNameExtractUtil.extract(proxyConf.upstreamEndpoint);

        if (envNames.isEmpty()) {
            try {
                uri = new URI(proxyConf.getUpstreamEndpoint());
            } catch (URISyntaxException exception) {
                String message = "Failed to parse " + proxyConf.getUpstreamEndpoint() + " to URI";
                throw new PluginConfException(message, exception);
            }
        }
    }

    @Override
    public Mono<Void> filter(Exchange exchange, FilterChain chain) {
        HttpServerRequest request = exchange.getRequest();
        URI uriToUse = uri;

        if (Objects.isNull(uriToUse)) {
            String uriStr = proxyConf.upstreamEndpoint;
            Map<String, String> environment = ExchangeUtil.getEnvironment(exchange);
            for (String envName : envNames) {
                String value = environment.get(envName);
                if (Objects.isNull(value)) {
                    return ResponseUtil.sendError(
                            exchange.getResponse(),
                            "ProxyHttpFilter error: required environment variable named " + envName + " not found."
                    );
                }
                uriStr = uriStr.replace("{" + envName + "}", value);
            }

            try {
                uriToUse = new URI(uriStr);
            } catch (URISyntaxException exception) {
                String message = "ProxyHttpFilter error: failed to parse " + uriStr + " to URI";
                return ResponseUtil.sendError(exchange.getResponse(), message);
            }
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

    @Getter
    @Setter
    public static class ProxyConf {

        private String upstreamEndpoint;

        private int timeout = Integer.MAX_VALUE;

    }

}
