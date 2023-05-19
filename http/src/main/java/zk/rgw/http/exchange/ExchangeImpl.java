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

package zk.rgw.http.exchange;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import zk.rgw.plugin.api.Exchange;

public class ExchangeImpl implements Exchange {

    private final HttpServerRequest request;

    private final HttpServerResponse response;

    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    public ExchangeImpl(HttpServerRequest request, HttpServerResponse response) {
        this.request = request;
        this.response = response;
    }

    @Override
    public HttpServerRequest getRequest() {
        return request;
    }

    @Override
    public HttpServerResponse getResponse() {
        return response;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

}
