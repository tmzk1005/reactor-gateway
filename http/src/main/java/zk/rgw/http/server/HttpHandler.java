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

package zk.rgw.http.server;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import zk.rgw.http.context.ReactiveRequestContextHolder;
import zk.rgw.http.context.RequestContext;
import zk.rgw.http.context.RequestContextImpl;
import zk.rgw.http.exchange.ChainBasedExchangeHandler;
import zk.rgw.http.exchange.ExchangeImpl;
import zk.rgw.http.route.Route;
import zk.rgw.http.route.locator.RouteLocator;
import zk.rgw.plugin.api.Exchange;
import zk.rgw.plugin.api.filter.Filter;
import zk.rgw.plugin.api.filter.FilterChain;
import zk.rgw.plugin.util.ResponseUtil;

@Slf4j
public class HttpHandler implements BiFunction<HttpServerRequest, HttpServerResponse, Mono<Void>> {

    private final RouteLocator routeLocator;

    private static final Route ROUTE_404;

    static {
        ROUTE_404 = new Route();
        ROUTE_404.setId("404");
        ROUTE_404.setPath("/");
        ROUTE_404.setFilters(List.of(new Filter404()));
    }

    public HttpHandler(RouteLocator routeLocator) {
        this.routeLocator = routeLocator;
    }

    @Override
    public Mono<Void> apply(HttpServerRequest request, HttpServerResponse response) {
        Exchange exchange = new ExchangeImpl(request, response);
        return routeLocator.getRoutes(request.fullPath())
                .filter(
                        route -> Objects.nonNull(route.getMethods())
                                && route.getMethods().contains(request.method())
                                && Objects.nonNull(route.getPredicate())
                                && route.getPredicate().test(exchange)
                )
                .switchIfEmpty(Mono.just(ROUTE_404))
                .next()
                .flatMap(route -> {
                    Mono<RequestContext> requestContext = Mono.just(new RequestContextImpl(exchange));
                    return new ChainBasedExchangeHandler(route.getFilters())
                            .handle(exchange)
                            .contextWrite(ReactiveRequestContextHolder.withRequestContext(requestContext));
                })
                .onErrorResume(throwable -> {
                    log.error("Request handle failed.", throwable);
                    return ResponseUtil.sendError(response);
                });
    }

    private static class Filter404 implements Filter {
        @Override
        public Mono<Void> filter(Exchange exchange, FilterChain chain) {
            return ResponseUtil.sendNotFound(exchange.getResponse());
        }
    }

}
