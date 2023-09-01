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

package zk.rgw.gateway.route;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import zk.rgw.common.definition.IdRouteDefinition;
import zk.rgw.common.event.RgwEvent;
import zk.rgw.common.event.RgwEventListener;
import zk.rgw.common.exception.RgwRuntimeException;
import zk.rgw.common.heartbeat.Notification;
import zk.rgw.common.util.JsonUtil;
import zk.rgw.gateway.event.NotificationEvent;
import zk.rgw.http.route.Route;
import zk.rgw.http.route.RouteEvent;
import zk.rgw.http.route.locator.AsyncUpdatableRouteLocator;
import zk.rgw.http.utils.UriBuilder;
import zk.rgw.plugin.util.Shuck;

@Slf4j
public class PullFromDashboardRouteLocator extends AsyncUpdatableRouteLocator implements RgwEventListener<RgwEvent> {

    private static final String PARAMETER_ENV_ID = "envId";

    private static final String PARAMETER_SEQ = "seq";

    @SuppressWarnings("java:S1075")
    private static final String SYNC_ROUTE_PATH = "/gateway/_sync";

    private final HttpClient httpClient;

    @Getter
    private final String dashboardAddress;

    @Getter
    private final String dashboardRouteSyncEndpoint;

    @Getter
    private final String dashboardAuthKey;

    @Getter
    private final String environmentId;

    private long latestSequenceNum = 0L;

    private final UriBuilder uriBuilder;

    public PullFromDashboardRouteLocator(String dashboardAddress, String dashboardApiContextPath, String dashboardAuthKey, String environmentId) {
        this.dashboardAddress = dashboardAddress;
        this.dashboardRouteSyncEndpoint = dashboardApiContextPath + SYNC_ROUTE_PATH;
        this.dashboardAuthKey = dashboardAuthKey;
        this.environmentId = environmentId;
        httpClient = HttpClient.create();
        try {
            uriBuilder = new UriBuilder(dashboardAddress);
            uriBuilder.path(dashboardRouteSyncEndpoint);
            uriBuilder.queryParam(PARAMETER_ENV_ID, this.environmentId);
            httpClient.baseUrl(uriBuilder.build().toString());
        } catch (URISyntaxException exception) {
            throw new RgwRuntimeException("Dashboard address is not validated uri.", exception);
        }
    }

    @Override
    protected Flux<RouteEvent> fetchRouteChange() {
        return fetchRouteDefinitions()
                .doOnNext(idRouteDefinition -> latestSequenceNum = Math.max(latestSequenceNum, idRouteDefinition.getSeqNum()))
                .map(idRouteDefinition -> {
                    RouteEvent routeEvent = new RouteEvent();
                    routeEvent.setRouteId(idRouteDefinition.getId());

                    if (Objects.isNull(idRouteDefinition.getRouteDefinition())) {
                        // 没有routeDefinition说明是下线了
                        routeEvent.setDelete(true);
                    } else {
                        routeEvent.setDelete(false);
                        try {
                            Route route = RouteConverter.convertRouteDefinition(idRouteDefinition);
                            routeEvent.setRoute(route);
                        } catch (RouteConvertException exception) {
                            log.error("Failed to convert a route definition to route.", exception);
                            routeEvent.setRouteId(null);
                        }
                    }
                    return routeEvent;
                });
    }

    private Flux<IdRouteDefinition> fetchRouteDefinitions() {
        URI uri;
        try {
            uri = uriBuilder.clearQueryParam(PARAMETER_SEQ).queryParam(PARAMETER_SEQ, this.latestSequenceNum + "").build();
        } catch (URISyntaxException exception) {
            throw new RgwRuntimeException(exception);
        }
        return httpClient.get().uri(uri).responseContent().aggregate().asInputStream()
                .onErrorResume(throwable -> {
                    log.error("Failed to fetch route changes from uri: {}", uri);
                    return Mono.empty();
                }).map(inputStream -> {
                    try {
                        SyncRouteResp syncRouteResp = JsonUtil.readValue(inputStream, SyncRouteResp.class);
                        List<IdRouteDefinition> list = syncRouteResp.getData();
                        if (!list.isEmpty()) {
                            log.info("Fetched {} route definitions from {}", list.size(), uri);
                        }
                        return list;
                    } catch (IOException ioException) {
                        throw new RgwRuntimeException("Failed to deserialize response of sync route definitions request sent to " + uri);
                    }
                }).flatMapMany((Function<List<IdRouteDefinition>, Flux<IdRouteDefinition>>) Flux::fromIterable);
    }

    @Override
    public void onEvent(RgwEvent event) {
        if (event instanceof NotificationEvent notificationEvent) {
            Notification notification = notificationEvent.getNotification();
            if (notification.isApiUpdated()) {
                log.info("Received api updated notification.");
                update();
            }
        }
    }

    static class SyncRouteResp extends Shuck<List<IdRouteDefinition>> {

        @Override
        public List<IdRouteDefinition> getData() {
            if (getCode() != Shuck.CODE_OK) {
                log.error("Sync route definitions request returned not ok status, something maybe wrong.");
                return List.of();
            }
            // 保证不能返回null
            List<IdRouteDefinition> data = super.getData();
            if (Objects.isNull(data)) {
                log.error("Sync route definitions request returned empty data, something maybe wrong.");
                return List.of();
            }
            return data;
        }

    }

}
