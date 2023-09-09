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
package zk.rgw.plugin.filter.modifyresponseheader;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.netty.handler.codec.http.HttpHeaders;
import lombok.Getter;
import lombok.Setter;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerResponse;

import zk.rgw.plugin.api.Exchange;
import zk.rgw.plugin.api.HttpServerResponseDecorator;
import zk.rgw.plugin.api.filter.FilterChain;
import zk.rgw.plugin.api.filter.JsonConfFilterPlugin;

public class ModifyResponseHeaderFilter implements JsonConfFilterPlugin {

    @Getter
    @Setter
    private List<String> removeHeaderNames;

    @Getter
    @Setter
    private Map<String, String> addHeaders;

    @Override
    public Mono<Void> filter(Exchange exchange, FilterChain chain) {
        Exchange modifiedExchange = exchange.mutate().response(new MyResponseDecorator(exchange.getResponse())).build();
        return chain.filter(modifiedExchange);
    }

    private class MyResponseDecorator extends HttpServerResponseDecorator {

        public MyResponseDecorator(HttpServerResponse delegator) {
            super(delegator);
        }

        @Override
        protected void doBeforeSend() {
            if (hasSentHeaders()) {
                return;
            }
            modifyResponseHeader(responseHeaders());
        }

        private void modifyResponseHeader(HttpHeaders headers) {
            if (Objects.nonNull(removeHeaderNames) && !removeHeaderNames.isEmpty()) {
                for (String headerName : removeHeaderNames) {
                    headers.remove(headerName);
                }
            }

            if (Objects.nonNull(addHeaders) && !addHeaders.isEmpty()) {
                for (Map.Entry<String, String> entry : addHeaders.entrySet()) {
                    headers.add(entry.getKey(), entry.getValue());
                }
            }
        }

    }

}
