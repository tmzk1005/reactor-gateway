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
package zk.rgw.plugin.filter.cors;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.Getter;
import lombok.Setter;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;

import zk.rgw.common.util.ObjectUtil;
import zk.rgw.plugin.api.Exchange;
import zk.rgw.plugin.api.filter.FilterChain;
import zk.rgw.plugin.api.filter.JsonConfFilterPlugin;
import zk.rgw.plugin.util.ResponseUtil;

public class CorsFilter implements JsonConfFilterPlugin {

    private static final List<String> REQUIRED_VARY_VALUES = List.of(
            HttpHeaderNames.ORIGIN.toString(),
            HttpHeaderNames.ACCESS_CONTROL_REQUEST_METHOD.toString(),
            HttpHeaderNames.ACCESS_CONTROL_REQUEST_HEADERS.toString()
    );

    @Getter
    @Setter
    private CorsStrategy strategy = new CorsStrategy();

    @Override
    public CorsStrategy getConf() {
        if (Objects.isNull(strategy)) {
            strategy = new CorsStrategy();
        }
        return strategy;
    }

    @Override
    public Mono<Void> filter(Exchange exchange, FilterChain chain) {
        if (!CorsUtil.isCorsRequest(exchange.getRequest())) {
            return chain.filter(exchange);
        }
        return handleCorsRequest(exchange, chain);
    }

    private Mono<Void> handleCorsRequest(Exchange exchange, FilterChain chain) {
        Objects.requireNonNull(strategy);

        HttpHeaders responseHeaders = exchange.getResponse().responseHeaders();
        setHeaderVary(responseHeaders);

        boolean isPreFlightRequest = CorsUtil.isCorsPreFlightRequest(exchange.getRequest());

        HttpServerRequest request = exchange.getRequest();

        boolean isValid = checkOrigin(request, strategy)
                && checkRequestMethod(request, strategy, isPreFlightRequest)
                && checkRequestHeaders(request, strategy, isPreFlightRequest);

        if (!isValid) {
            return ResponseUtil.sendStatus(exchange.getResponse(), HttpResponseStatus.FORBIDDEN);
        }

        responseHeaders.set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, strategy.getAllowedOrigins());

        List<String> exposedHeaders = strategy.getExposedHeaders();
        if (!ObjectUtil.isEmpty(exposedHeaders)) {
            responseHeaders.set(HttpHeaderNames.ACCESS_CONTROL_EXPOSE_HEADERS, exposedHeaders);
        }

        boolean allowCredentials = strategy.isAllowCredentials();
        if (allowCredentials) {
            responseHeaders.set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS, allowCredentials);
        }

        Long maxAge = strategy.getMaxAge();
        if (Objects.nonNull(maxAge)) {
            responseHeaders.set(HttpHeaderNames.ACCESS_CONTROL_MAX_AGE, maxAge);
        }

        if (isPreFlightRequest) {
            responseHeaders.set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, strategy.getAllowedMethods());
            List<String> allowedHeaders = strategy.getAllowedHeaders();
            if (!ObjectUtil.isEmpty(allowedHeaders)) {
                responseHeaders.set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, allowedHeaders);
            }
            return ResponseUtil.sendOk(exchange.getResponse());
        } else {
            return chain.filter(exchange);
        }
    }

    private boolean checkOrigin(HttpServerRequest request, CorsStrategy corsStrategy) {
        return corsStrategy.isAllowedOrigin(request.requestHeaders().get(HttpHeaderNames.ORIGIN));
    }

    private boolean checkRequestMethod(HttpServerRequest request, CorsStrategy corsStrategy, boolean isPreFlightRequest) {
        String methodStr;
        if (isPreFlightRequest) {
            methodStr = request.requestHeaders().get(HttpHeaderNames.ACCESS_CONTROL_REQUEST_METHOD);
        } else {
            methodStr = request.method().name();
        }
        return corsStrategy.isAllowedMethod(methodStr.toUpperCase());
    }

    private boolean checkRequestHeaders(HttpServerRequest request, CorsStrategy corsStrategy, boolean isPreFlightRequest) {
        if (corsStrategy.isAllowAnyHeaders()) {
            return true;
        }
        HttpHeaders requestHeaders = request.requestHeaders();
        List<String> headerNames;
        if (isPreFlightRequest) {
            headerNames = requestHeaders.getAll(HttpHeaderNames.ACCESS_CONTROL_REQUEST_HEADERS);
        } else {
            headerNames = requestHeaders.entries().stream().map(Map.Entry::getKey).toList();
        }
        for (String headerName : headerNames) {
            if (!corsStrategy.isAllowedHeader(headerName)) {
                return false;
            }
        }
        return true;
    }

    private void setHeaderVary(HttpHeaders httpHeaders) {
        if (!httpHeaders.contains(HttpHeaderNames.VARY)) {
            httpHeaders.add(HttpHeaderNames.VARY, REQUIRED_VARY_VALUES);
        } else {
            List<String> curVary = httpHeaders.getAll(HttpHeaderNames.VARY);
            for (String value : REQUIRED_VARY_VALUES) {
                if (!curVary.contains(value)) {
                    httpHeaders.add(HttpHeaderNames.VARY, value);
                }
            }
        }
    }

}
