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

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.Getter;
import lombok.Setter;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import zk.rgw.plugin.api.Exchange;
import zk.rgw.plugin.api.filter.FilterChain;
import zk.rgw.plugin.api.filter.JsonConfFilterPlugin;
import zk.rgw.plugin.exception.PluginConfException;
import zk.rgw.plugin.util.ResponseUtil;

public class ProxyHttpFilter implements JsonConfFilterPlugin {

    private static final HttpClient HTTP_CLIENT = HttpClient.create();

    @Getter
    @Setter
    private String upstreamEndpoint;

    @JsonIgnore
    @Setter
    @Getter
    private HttpClient httpClient = HTTP_CLIENT;

    @Getter
    @Setter
    private int timeout = Integer.MAX_VALUE;

    @JsonIgnore
    private URI upstreamUri;

    @Override
    public void configure(String conf) throws PluginConfException {
        JsonConfFilterPlugin.super.configure(conf);
        try {
            this.upstreamUri = new URI(upstreamEndpoint);
        } catch (URISyntaxException exception) {
            String message = "Failed to parse " + upstreamEndpoint + " to URI";
            throw new PluginConfException(message, exception);
        }
        if (timeout <= 0) {
            timeout = Integer.MAX_VALUE;
        }
    }

    @Override
    public Mono<Void> filter(Exchange exchange, FilterChain chain) {
        HttpServerRequest request = exchange.getRequest();
        return this.httpClient.headers(headers -> {
            headers.add(request.requestHeaders());
            headers.remove(HttpHeaderNames.HOST);
        }).request(request.method()).uri(upstreamUri).send(request.receive()).responseConnection((httpClientResponse, connection) -> {
            HttpServerResponse serverResponse = exchange.getResponse();
            return serverResponse.status(httpClientResponse.status())
                    .headers(httpClientResponse.responseHeaders())
                    .send(connection.inbound().receive().retain())
                    .then()
                    .thenReturn("");
        }).timeout(
                Duration.ofSeconds(timeout),
                Mono.defer(() -> ResponseUtil.sendStatus(exchange.getResponse(), HttpResponseStatus.GATEWAY_TIMEOUT).thenReturn(""))
        ).then();
    }

}
