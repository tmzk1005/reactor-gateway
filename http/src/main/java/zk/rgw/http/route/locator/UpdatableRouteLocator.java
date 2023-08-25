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

import java.util.Objects;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import zk.rgw.http.route.Route;
import zk.rgw.http.route.RouteEvent;

@Slf4j
public abstract class UpdatableRouteLocator extends ManageableRouteLocator {

    protected abstract Flux<RouteEvent> fetchRouteChange();

    public synchronized void update() {
        doUpdate();
    }

    protected void doUpdate() {
        fetchRouteChange().doOnNext(routeEvent -> {
            if (Objects.isNull(routeEvent.getRouteId())) {
                // 可以通过设置routeId为null来标识是一个无效事件
                return;
            }
            if (routeEvent.isDelete()) {
                Route route = removeRouteById(routeEvent.getRouteId());
                log.info("{} remove route with id = {}", this.getClass().getSimpleName(), route.getId());
            } else {
                log.info("Add or update a route with id = {}", routeEvent.getRouteId());
                addRoute(routeEvent.getRoute());
            }
        }).subscribe();
    }

}
