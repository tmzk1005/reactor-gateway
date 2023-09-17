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

package zk.rgw.plugin.filter.circuitbreaker;

import java.io.IOException;

import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Mono;

import zk.rgw.plugin.api.Exchange;
import zk.rgw.plugin.api.filter.FilterChain;
import zk.rgw.plugin.api.filter.JsonConfFilterPlugin;
import zk.rgw.plugin.exception.PluginConfException;
import zk.rgw.plugin.util.ExchangeUtil;
import zk.rgw.plugin.util.ResponseUtil;

public class CircuitBreakerFilter implements JsonConfFilterPlugin {

    private static final String TAG = "熔断";

    private CircuitBreakerConf circuitBreakerConf;

    private CircuitBreaker circuitBreaker;

    @Override
    public void configure(String conf) throws PluginConfException {
        this.circuitBreakerConf = new CircuitBreakerConf();
        try {
            OM.readerForUpdating(circuitBreakerConf).readValue(conf);
        } catch (IOException ioException) {
            throw new PluginConfException(ioException.getMessage(), ioException);
        }
        this.circuitBreaker = new CircuitBreakerImpl(circuitBreakerConf);
    }

    @Override
    public Mono<Void> filter(Exchange exchange, FilterChain chain) {
        return circuitBreaker.run(
                chain.filter(exchange).doOnSuccess(ignore -> {
                    HttpResponseStatus respStatus = exchange.getResponse().status();
                    if (circuitBreakerConf.getFailureCodes().contains(respStatus.code())) {
                        throw new CircuitBreakerException(CircuitBreakerState.CLOSED);
                    }
                }),
                throwable -> this.handleException(throwable, exchange)
        );
    }

    private Mono<Void> handleException(Throwable exception, Exchange exchange) {
        if (exception instanceof CircuitBreakerException cbException) {
            CircuitBreakerState state = cbException.getCircuitBreakerState();
            if (CircuitBreakerState.OPEN == state) {
                ExchangeUtil.addAuditTag(exchange, TAG);
                return ResponseUtil.sendStatus(exchange.getResponse(), HttpResponseStatus.SERVICE_UNAVAILABLE);
            } else {
                // CLOSED和HALF_OPEN状态下有错误的响应，在网关层面相当于是正常的，要把上游的响应原样仍回， 因此返回Mono.empty()即可
                return Mono.empty();
            }
        } else {
            return Mono.error(exception);
        }
    }

}
