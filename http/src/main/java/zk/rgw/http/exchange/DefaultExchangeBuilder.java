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

import java.util.Objects;

import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import zk.rgw.plugin.api.Exchange;

public class DefaultExchangeBuilder implements Exchange.Builder {

    private final Exchange delegator;

    private HttpServerRequest request;

    private HttpServerResponse response;

    public DefaultExchangeBuilder(Exchange exchange) {
        this.delegator = exchange;
    }

    @Override
    public Exchange.Builder request(HttpServerRequest request) {
        this.request = request;
        return this;
    }

    @Override
    public Exchange.Builder response(HttpServerResponse response) {
        this.response = response;
        return this;
    }

    @Override
    public Exchange build() {
        return new MutativeDecorator(delegator).withRequest(request).withResponse(response);
    }

    private static class MutativeDecorator extends ExchangeDecorator {

        private HttpServerRequest request;

        private HttpServerResponse response;

        private MutativeDecorator(Exchange delegator) {
            super(delegator);
        }

        private MutativeDecorator withRequest(HttpServerRequest request) {
            this.request = request;
            return this;
        }

        private MutativeDecorator withResponse(HttpServerResponse response) {
            this.response = response;
            return this;
        }

        @Override
        public HttpServerRequest getRequest() {
            return Objects.nonNull(request) ? request : super.getRequest();
        }

        @Override
        public HttpServerResponse getResponse() {
            return Objects.nonNull(response) ? response : super.getResponse();
        }

    }

}
