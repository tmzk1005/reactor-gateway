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

import zk.rgw.http.path.PathUtil;
import zk.rgw.http.route.Route;
import zk.rgw.http.route.locator.RouteLocator;

public class GatewayInternalRouteLocator implements RouteLocator {

    private final String internalContextPath;

    private final String internalContextPathWithEndSlash;

    private final Flux<Route> internalRoutes;

    public GatewayInternalRouteLocator(String internalContextPath) {
        this.internalContextPath = PathUtil.normalize(internalContextPath);
        this.internalContextPathWithEndSlash = this.internalContextPath + PathUtil.SLASH;
        Route route = new Route();
        route.setId("__rgw_internal");
        route.setPath(internalContextPath);
        route.setFilters(List.of(new GatewayInternalEndpoint()));
        this.internalRoutes = Flux.just(route);
    }

    @Override
    public Flux<Route> getRoutes(String path) {
        String normalizePath = PathUtil.normalize(path);
        if (normalizePath.startsWith(internalContextPathWithEndSlash) || normalizePath.equals(internalContextPath)) {
            return internalRoutes;
        }
        return Flux.empty();
    }

}
