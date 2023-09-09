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

package zk.rgw.plugin.filter.mock.http;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.Getter;
import lombok.Setter;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerResponse;

import zk.rgw.plugin.api.Exchange;
import zk.rgw.plugin.api.filter.FilterChain;
import zk.rgw.plugin.api.filter.JsonConfFilterPlugin;

@Getter
@Setter
public class HttpMockFilter implements JsonConfFilterPlugin {

    private int statusCode = 200;

    private String content = "mock";

    private Map<String, List<String>> headers;

    @Override
    public Mono<Void> filter(Exchange exchange, FilterChain chain) {
        HttpServerResponse response = exchange.getResponse();
        response.status(HttpResponseStatus.valueOf(statusCode));
        if (Objects.nonNull(headers) && !headers.isEmpty()) {
            headers.forEach((key, values) -> {
                for (String value : values) {
                    response.header(key, value);
                }
            });
        }
        response.header(HttpHeaderNames.CONTENT_LENGTH, content.getBytes(StandardCharsets.UTF_8).length + "");
        // 虽然这里暂时没有用到请求体内容，但是仍然读取后才发送响应，以便可以审计到请求体
        return exchange.getRequest().receive().then(response.sendString(Mono.just(content)).then());
    }

}
