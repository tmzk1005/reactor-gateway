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

package zk.rgw.http.route.locator;

import reactor.core.publisher.Flux;

import zk.rgw.http.route.Route;

public class CompositeRouteLocator implements RouteLocator {

    private final Flux<RouteLocator> delegates;

    public CompositeRouteLocator(Flux<RouteLocator> delegates) {
        this.delegates = delegates;
    }

    public CompositeRouteLocator(RouteLocator... routeLocators) {
        this(Flux.fromArray(routeLocators));
    }

    @Override
    public Flux<Route> getRoutes(String path) {
        return this.delegates.flatMapSequential(routeLocator -> routeLocator.getRoutes(path));
    }

}
