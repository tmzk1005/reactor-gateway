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

import java.util.function.Function;

import reactor.core.publisher.Mono;

public abstract class AbstractCircuitBreaker implements CircuitBreaker {

    @Override
    public <T> Mono<T> run(Mono<T> toRun, Function<Throwable, Mono<T>> fallback) {
        return getState().flatMap(state -> {
            if (CircuitBreakerState.CLOSED == state) {
                return toRun;
            } else if (CircuitBreakerState.OPEN == state) {
                return Mono.error(new CircuitBreakerException(state));
            } else {
                return toRun.onErrorMap(CircuitBreakerException.class::isInstance, throwable -> new CircuitBreakerException(state));
            }
        }).doOnSuccess(ignore -> incrementSuccessCount()).doOnError(CircuitBreakerException.class, exception -> {
            CircuitBreakerState state = exception.getCircuitBreakerState();
            if (state == CircuitBreakerState.CLOSED || state == CircuitBreakerState.HALF_OPEN) {
                incrementErrorCount();
            }
        }).onErrorResume(fallback);
    }

    protected abstract Mono<CircuitBreakerState> getState();

    protected abstract void incrementErrorCount();

    protected abstract void incrementSuccessCount();

}
