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

package zk.rgw.gateway.app;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import zk.rgw.common.definition.AppDefinition;
import zk.rgw.common.definition.SubscriptionRelationship;
import zk.rgw.common.event.RgwEvent;
import zk.rgw.common.event.RgwEventListener;
import zk.rgw.common.event.impl.AppSubRouteEvent;
import zk.rgw.common.exception.RgwRuntimeException;
import zk.rgw.common.heartbeat.Notification;
import zk.rgw.common.util.JsonUtil;
import zk.rgw.common.util.ObjectUtil;
import zk.rgw.gateway.event.AppOpSeqBehindEvent;
import zk.rgw.gateway.event.NotificationEvent;
import zk.rgw.http.utils.UriBuilder;
import zk.rgw.plugin.util.Shuck;

@Slf4j
public class AppSubscribeRouteManager implements RgwEventListener<RgwEvent> {

    @SuppressWarnings("java:S1075")
    private static final String SYNC_APP_PATH = "/gateway/_sync-app";

    private static final String PARAMETER_SEQ = "seq";

    public final Map<String, Map<String, AppDefinition>> subscriptions = new HashMap<>();

    private final HttpClient httpClient;

    private long opSeq = 0L;

    @Getter
    private final String dashboardAddress;

    @Getter
    private final String dashboardSyncEndpoint;

    @Getter
    private final String dashboardAuthKey;

    private final UriBuilder uriBuilder;

    public AppSubscribeRouteManager(String dashboardAddress, String dashboardApiContextPath, String dashboardAuthKey) {
        this.dashboardAddress = dashboardAddress;
        this.dashboardSyncEndpoint = dashboardApiContextPath + SYNC_APP_PATH;
        this.dashboardAuthKey = dashboardAuthKey;
        httpClient = HttpClient.create();
        try {
            uriBuilder = new UriBuilder(dashboardAddress);
            uriBuilder.path(dashboardSyncEndpoint);
            httpClient.baseUrl(uriBuilder.build().toString());
        } catch (URISyntaxException exception) {
            throw new RgwRuntimeException("Dashboard address is not validated uri.", exception);
        }
    }

    @Override
    public void onEvent(RgwEvent event) {
        if (event instanceof NotificationEvent notificationEvent) {
            Notification notification = notificationEvent.getNotification();
            if (notification.isSubscriptionUpdated()) {
                handleAppSubRouteEvent(notification.getAppSubRouteEvent());
            }
        } else if (event instanceof AppOpSeqBehindEvent) {
            log.info("Subscription relationship is behind, going to sync");
            Mono<List<SubscriptionRelationship>> listMono = sync();
            listMono.doOnSuccess(this::handleSyncData).subscribe();
        }
    }

    private void handleAppSubRouteEvent(AppSubRouteEvent appSubRouteEvent) {
        String routeId = appSubRouteEvent.getRouteId();
        Map<String, AppDefinition> map = subscriptions.computeIfAbsent(routeId, key -> new HashMap<>());
        AppDefinition appDefinition = appSubRouteEvent.getAppDefinition();
        if (appSubRouteEvent.isSub()) {
            log.debug("app with key = {} subscribe route with id = {}", appDefinition.getKey(), routeId);
            map.put(appDefinition.getKey(), appDefinition);
        } else {
            log.debug("app with key = {} cancel subscribe route with id = {}", appDefinition.getKey(), routeId);
            map.remove(appDefinition.getKey());
        }
        if (appSubRouteEvent.getOpSeq() > this.opSeq) {
            this.opSeq = appSubRouteEvent.getOpSeq();
        }
    }

    private void handleSyncData(List<SubscriptionRelationship> list) {
        if (ObjectUtil.isEmpty(list)) {
            return;
        }
        for (SubscriptionRelationship relationship : list) {
            List<AppDefinition> appDefinitions = relationship.getAppDefinitions();
            Map<String, AppDefinition> map = new HashMap<>(appDefinitions.size() * 2);
            for (AppDefinition appDefinition : appDefinitions) {
                map.put(appDefinition.getKey(), appDefinition);
            }
            subscriptions.put(relationship.getRouteId(), map);
            if (relationship.getOpSeq() > opSeq) {
                opSeq = relationship.getOpSeq();
            }
        }
    }

    public Map<String, AppDefinition> getForRouteId(String routeId) {
        Map<String, AppDefinition> map = subscriptions.getOrDefault(routeId, Map.of());
        return Map.copyOf(map);
    }

    private Mono<List<SubscriptionRelationship>> sync() {
        URI uri;
        try {
            uri = uriBuilder.clearQueryParam(PARAMETER_SEQ).queryParam(PARAMETER_SEQ, this.opSeq + "").build();
        } catch (URISyntaxException exception) {
            throw new RgwRuntimeException(exception);
        }
        return httpClient.get().uri(uri).responseContent().aggregate().asInputStream()
                .onErrorResume(throwable -> {
                    log.error("Failed to fetch subscription relationship from uri: {}", uri);
                    return Mono.empty();
                }).map(inputStream -> {
                    try {
                        SyncResp syncResp = JsonUtil.readValue(inputStream, SyncResp.class);
                        List<SubscriptionRelationship> list = syncResp.getData();
                        if (!list.isEmpty()) {
                            log.info("Fetched {} subscription relationship from {}", list.size(), uri);
                        }
                        return list;
                    } catch (IOException ioException) {
                        throw new RgwRuntimeException("Failed to deserialize response of sync subscription relationship request sent to " + uri);
                    }
                });
    }

    static class SyncResp extends Shuck<List<SubscriptionRelationship>> {

        @Override
        public List<SubscriptionRelationship> getData() {
            if (getCode() != Shuck.CODE_OK) {
                log.error("Sync subscription relationship request returned not ok status, something maybe wrong.");
                return List.of();
            }
            // 保证不能返回null
            List<SubscriptionRelationship> data = super.getData();
            if (Objects.isNull(data)) {
                log.error("Sync subscription relationship request returned empty data, something maybe wrong.");
                return List.of();
            }
            return data;
        }

    }

}
