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
package zk.rgw.gateway.env;

import java.util.Objects;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import zk.rgw.common.event.RgwEvent;
import zk.rgw.common.event.RgwEventListener;
import zk.rgw.common.event.impl.EnvironmentChangedEvent;
import zk.rgw.common.heartbeat.Notification;
import zk.rgw.gateway.event.NotificationEvent;
import zk.rgw.http.route.Route;
import zk.rgw.http.utils.RouteUtil;
import zk.rgw.plugin.api.Exchange;
import zk.rgw.plugin.api.filter.Filter;
import zk.rgw.plugin.api.filter.FilterChain;
import zk.rgw.plugin.util.ExchangeUtil;

@Slf4j
public class EnvironmentPrepareFilter implements Filter, RgwEventListener<RgwEvent> {

    private final EnvironmentManager environmentManager = new EnvironmentManager();

    @Override
    public Mono<Void> filter(Exchange exchange, FilterChain chain) {
        Route route = RouteUtil.getRoute(exchange);
        if (Objects.nonNull(route) && Objects.nonNull(route.getEnvKey())) {
            String envKey = route.getEnvKey();
            exchange.getAttributes().put(ExchangeUtil.ENVIRONMENT_VARS, environmentManager.getEnvForOrg(envKey));
        }
        return chain.filter(exchange);
    }

    @Override
    public void onEvent(RgwEvent event) {
        if (event instanceof NotificationEvent notificationEvent) {
            Notification notification = notificationEvent.getNotification();
            if (notification.isEnvironmentUpdated()) {
                EnvironmentChangedEvent environmentChangedEvent = notification.getEnvironmentChangedEvent();
                if (Objects.nonNull(environmentChangedEvent)) {
                    log.info("Received environment updated notification from dashboard, orgId = {}", environmentChangedEvent.getOrgId());
                    environmentManager.setEnvForOrg(environmentChangedEvent.getOrgId(), environmentChangedEvent.getVariables());
                }
            }
        }
    }

}
