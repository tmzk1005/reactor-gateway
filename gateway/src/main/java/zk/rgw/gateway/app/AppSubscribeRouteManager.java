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

import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import zk.rgw.common.definition.AppDefinition;
import zk.rgw.common.event.RgwEvent;
import zk.rgw.common.event.RgwEventListener;
import zk.rgw.common.event.impl.AppSubRouteEvent;
import zk.rgw.common.heartbeat.Notification;
import zk.rgw.gateway.event.NotificationEvent;

@Slf4j
public class AppSubscribeRouteManager implements RgwEventListener<RgwEvent> {

    public final Map<String, Map<String, AppDefinition>> subscriptions = new HashMap<>();

    @Override
    public void onEvent(RgwEvent event) {
        if (event instanceof NotificationEvent notificationEvent) {
            Notification notification = notificationEvent.getNotification();
            if (notification.isSubscriptionUpdated()) {
                handleAppSubRouteEvent(notification.getAppSubRouteEvent());
            }
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
    }

    public Map<String, AppDefinition> getForRouteId(String routeId) {
        Map<String, AppDefinition> map = subscriptions.getOrDefault(routeId, Map.of());
        return Map.copyOf(map);
    }

}
