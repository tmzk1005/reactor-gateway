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

import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import zk.rgw.plugin.api.Exchange;

public class ExchangeDecorator implements Exchange {

    private final Exchange delegator;

    public ExchangeDecorator(Exchange exchange) {
        this.delegator = exchange;
    }

    @Override
    public HttpServerRequest getRequest() {
        return this.delegator.getRequest();
    }

    @Override
    public HttpServerResponse getResponse() {
        return this.delegator.getResponse();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return this.delegator.getAttributes();
    }

    @Override
    public Builder mutate() {
        return new DefaultExchangeBuilder(this);
    }

}
