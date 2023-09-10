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
package zk.rgw.plugin.filter.websocket;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakeException;
import lombok.Getter;
import lombok.Setter;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.http.websocket.WebsocketInbound;
import reactor.netty.http.websocket.WebsocketOutbound;

import zk.rgw.common.util.EnvNameExtractUtil;
import zk.rgw.plugin.api.Exchange;
import zk.rgw.plugin.api.filter.FilterChain;
import zk.rgw.plugin.api.filter.JsonConfFilterPlugin;
import zk.rgw.plugin.exception.PluginConfException;
import zk.rgw.plugin.util.ExchangeUtil;
import zk.rgw.plugin.util.ResponseUtil;

public class WebSocketFilter implements JsonConfFilterPlugin {

    private static final HttpClient HTTP_CLIENT = HttpClient.create();

    @Getter
    @Setter
    private HttpClient httpClient = HTTP_CLIENT;

    private final ProxyConf proxyConf = new ProxyConf();

    private URI uri;

    private List<String> envNames;

    @Override
    public ProxyConf getConf() {
        return proxyConf;
    }

    @Override
    public void afterConfigured() throws PluginConfException {
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

        // step-1: check

        HttpMethod method = request.method();
        HttpHeaders requestHeaders = request.requestHeaders();
        if (!HttpMethod.GET.equals(method)) {
            return ResponseUtil.sendStatus(exchange.getResponse(), HttpResponseStatus.METHOD_NOT_ALLOWED);
        }

        if (!"WebSocket".equalsIgnoreCase(requestHeaders.get(HttpHeaderNames.UPGRADE))) {
            return ResponseUtil.sendStatus(exchange.getResponse(), HttpResponseStatus.BAD_REQUEST, "Invalid Upgrade header.");
        }

        List<String> connectionValues = requestHeaders.getAll(HttpHeaderNames.CONNECTION);
        if (!connectionValues.contains("Upgrade") && !connectionValues.contains("upgrade")) {
            return ResponseUtil.sendStatus(exchange.getResponse(), HttpResponseStatus.BAD_REQUEST, "Invalid Connection header.");
        }

        String secWebsocketKey = requestHeaders.get(HttpHeaderNames.SEC_WEBSOCKET_KEY);
        if (Objects.isNull(secWebsocketKey)) {
            return ResponseUtil.sendStatus(exchange.getResponse(), HttpResponseStatus.BAD_REQUEST, "Missing Sec-WebSocket-Key header");
        }

        // step-2: then prepare uri

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

        // step-3: finally proxy request

        return httpClient.headers(headers -> {
            HttpHeaders rawRequestHeaders = request.requestHeaders();
            rawRequestHeaders.remove(HttpHeaderNames.HOST);
            rawRequestHeaders.forEach(entry -> headers.add(entry.getKey(), entry.getValue()));
        })
                .websocket()
                .uri(uriToUse)
                .handle(new UpstreamWebSocketHandler(exchange.getResponse()))
                .onErrorResume(WebSocketClientHandshakeException.class, exception -> {
                    HttpResponse clientResponse = exception.response();
                    HttpServerResponse serverResponse = exchange.getResponse();
                    serverResponse.status(clientResponse.status());
                    HttpHeaders responseHeaders = serverResponse.responseHeaders();
                    responseHeaders.clear();
                    responseHeaders.setAll(clientResponse.headers());
                    return serverResponse.send();
                })
                .then();
    }

    private record UpstreamWebSocketHandler(HttpServerResponse httpServerResponse)
            implements BiFunction<WebsocketInbound, WebsocketOutbound, Mono<Void>> {

        @Override
        public Mono<Void> apply(WebsocketInbound proxyWsInbound, WebsocketOutbound proxyWsOutbound) {
            HttpHeaders responseHeaders = httpServerResponse.responseHeaders();
            responseHeaders.clear();
            responseHeaders.setAll(proxyWsInbound.headers());
            return httpServerResponse.sendWebsocket((serverWsInbound, serverWsOutbound) -> {
                return proxy(serverWsInbound, serverWsOutbound, proxyWsInbound, proxyWsOutbound);
            });
        }

        private Mono<Void> proxy(
                WebsocketInbound serverWsInbound, WebsocketOutbound serverWsOutbound,
                WebsocketInbound proxyWsInbound, WebsocketOutbound proxyWsOutbound
        ) {
            Mono.when(
                    serverWsInbound.receiveCloseStatus().map(status -> proxyWsOutbound.sendClose(status.code(), status.reasonText())),
                    proxyWsInbound.receiveCloseStatus().map(status -> serverWsOutbound.sendClose(status.code(), status.reasonText()))
            ).subscribe();
            return Mono.zip(
                    proxyWsOutbound.send(serverWsInbound.receive().retain()).then(),
                    serverWsOutbound.send(proxyWsInbound.receive().retain()).then()
            ).then();
        }

    }

    @Getter
    @Setter
    public static class ProxyConf {

        private String upstreamEndpoint;

    }

}
