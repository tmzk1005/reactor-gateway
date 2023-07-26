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

package zk.rgw.dashboard;

import java.util.List;

import reactor.core.publisher.Flux;

import zk.rgw.dashboard.framework.filter.auth.JwtAuthenticationFilter;
import zk.rgw.dashboard.framework.filter.mvc.ControllerMethodInvokeFilter;
import zk.rgw.http.path.PathUtil;
import zk.rgw.http.route.Route;
import zk.rgw.http.route.locator.RouteLocator;

public class DashboardRoutes implements RouteLocator {

    private static final List<String> NO_NEED_LOGIN_PATHS = List.of("/user/_login");

    private final String apiContextPath;

    private final String apiContextPathWithEndSlash;

    private final Flux<Route> internalRoutes;

    public DashboardRoutes(String apiContextPath) {
        this.apiContextPath = PathUtil.normalize(apiContextPath);
        this.apiContextPathWithEndSlash = this.apiContextPath + PathUtil.SLASH;

        Route route = new Route();
        route.setId("__dashboard_internal");
        route.setPath(apiContextPath);

        List<String> finalNoNeedLoginPaths = NO_NEED_LOGIN_PATHS.stream().map(path -> apiContextPath + path).toList();

        route.setFilters(
                List.of(
                        new JwtAuthenticationFilter(finalNoNeedLoginPaths),
                        new ControllerMethodInvokeFilter(this.apiContextPath)
                )
        );
        this.internalRoutes = Flux.just(route);
    }

    @Override
    public Flux<Route> getRoutes(String path) {
        String normalizePath = PathUtil.normalize(path);
        if (normalizePath.startsWith(apiContextPathWithEndSlash) || normalizePath.equals(apiContextPath)) {
            return internalRoutes;
        }
        return Flux.empty();
    }

}
