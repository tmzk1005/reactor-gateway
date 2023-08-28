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

import zk.rgw.gateway.accesslog.AccessLogEnabledProvider;
import zk.rgw.gateway.route.PullFromDashboardRouteLocator;
import zk.rgw.http.path.PathUtil;
import zk.rgw.http.route.Route;
import zk.rgw.http.route.locator.RouteLocator;

@SuppressWarnings("java:S1075")
public class GatewayInternalRouteLocator implements RouteLocator {

    private static final String INTERNAL_CONTEXT_PATH = "/__rgw_internal";

    private static final String INTERNAL_CONTEXT_PATH_SLASH = INTERNAL_CONTEXT_PATH + "/";

    private final Flux<Route> internalRoutes;

    public GatewayInternalRouteLocator(
            PullFromDashboardRouteLocator pullFromDashboardRouteLocator,
            AccessLogEnabledProvider accessLogEnabledProvider
    ) {
        Route route = new Route();
        route.setId("__rgw_internal");
        route.setPath(INTERNAL_CONTEXT_PATH);
        route.setFilters(List.of(new GatewayInternalEndpoint(INTERNAL_CONTEXT_PATH, pullFromDashboardRouteLocator, accessLogEnabledProvider)));
        this.internalRoutes = Flux.just(route);
    }

    @Override
    public Flux<Route> getRoutes(String path) {
        String normalizePath = PathUtil.normalize(path);
        if (normalizePath.startsWith(INTERNAL_CONTEXT_PATH_SLASH) || normalizePath.equals(INTERNAL_CONTEXT_PATH)) {
            return internalRoutes;
        }
        return Flux.empty();
    }

}
