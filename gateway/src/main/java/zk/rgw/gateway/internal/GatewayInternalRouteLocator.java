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
package zk.rgw.gateway.internal;

import java.util.List;

import reactor.core.publisher.Flux;

import zk.rgw.http.route.Route;
import zk.rgw.http.route.locator.RouteLocator;

public class GatewayInternalRouteLocator implements RouteLocator {

    private static final Flux<Route> INTERNAL_SERVE_ROUTE;

    static {
        Route route = new Route();
        route.setId("__rgw_gateway_internal");
        route.setPath(GatewayInternalEndpoint.CONTEXT_PATH);
        route.setFilters(List.of(new GatewayInternalEndpoint()));
        INTERNAL_SERVE_ROUTE = Flux.just(route);
    }

    @Override
    public Flux<Route> getRoutes(String path) {
        if (path.startsWith(GatewayInternalEndpoint.CONTEXT_PATH)) {
            return INTERNAL_SERVE_ROUTE;
        }
        return Flux.empty();
    }

}
