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

import java.util.HashMap;
import java.util.Map;

import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import zk.rgw.common.heartbeat.Notification;
import zk.rgw.common.util.JsonUtil;
import zk.rgw.gateway.route.PullFromDashboardRouteLocator;
import zk.rgw.http.path.PathUtil;
import zk.rgw.plugin.api.Exchange;
import zk.rgw.plugin.api.filter.Filter;
import zk.rgw.plugin.api.filter.FilterChain;
import zk.rgw.plugin.util.ResponseUtil;

@Slf4j
public class GatewayInternalEndpoint implements Filter {

    private final Map<String, Endpoint> endpoints = new HashMap<>();

    private final PullFromDashboardRouteLocator pullFromDashboardRouteLocator;

    private final String contextPath;

    public GatewayInternalEndpoint(String contextPath, PullFromDashboardRouteLocator pullFromDashboardRouteLocator) {
        this.contextPath = contextPath;
        this.pullFromDashboardRouteLocator = pullFromDashboardRouteLocator;
        endpoints.put("/notification", new NotificationReceiverEndpoint());
    }

    @Override
    public Mono<Void> filter(Exchange exchange, FilterChain chain) {
        String fullPath = exchange.getRequest().fullPath();
        String path = fullPath.substring(contextPath.length());
        String normalizePath = PathUtil.normalize(path);
        if (!endpoints.containsKey(normalizePath)) {
            return ResponseUtil.sendNotFound(exchange.getResponse());
        }
        return endpoints.get(normalizePath).handle(exchange);
    }

    interface Endpoint {
        Mono<Void> handle(Exchange exchange);
    }

    class NotificationReceiverEndpoint implements Endpoint {

        @Override
        public Mono<Void> handle(Exchange exchange) {
            return exchange.getRequest().receive().aggregate().asString().flatMap(jsonStr -> {
                Notification notification;
                try {
                    notification = JsonUtil.readValue(jsonStr, Notification.class);
                } catch (Exception exception) {
                    log.error("Failed to deserialize to Notification.class", exception);
                    return ResponseUtil.sendStatus(exchange.getResponse(), HttpResponseStatus.BAD_REQUEST);
                }
                if (notification.isApiUpdated()) {
                    pullFromDashboardRouteLocator.update();
                }
                return ResponseUtil.sendOk(exchange.getResponse());
            });
        }

    }

}
