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
package zk.rgw.gateway;

import java.util.HashMap;
import java.util.Map;

import zk.rgw.common.event.EventPublisher;
import zk.rgw.common.event.EventPublisherImpl;
import zk.rgw.common.event.RgwEvent;
import zk.rgw.gateway.app.AppAuthFilter;
import zk.rgw.gateway.app.AppSubscribeRouteManager;
import zk.rgw.gateway.env.EnvironmentPrepareFilter;
import zk.rgw.gateway.heartbeat.HeartbeatReporter;
import zk.rgw.gateway.internal.GatewayInternalEndpoint;
import zk.rgw.gateway.internal.GatewayInternalRouteLocator;
import zk.rgw.gateway.route.PullFromDashboardRouteLocator;

public class GlobalSingletons {

    private GlobalSingletons() {
    }

    private static final Map<Class<?>, Object> INSTANCES = new HashMap<>();

    static void init(GatewayConfiguration configuration) {
        INSTANCES.put(GatewayConfiguration.class, configuration);

        EnvironmentPrepareFilter environmentPrepareFilter = new EnvironmentPrepareFilter();
        INSTANCES.put(EnvironmentPrepareFilter.class, environmentPrepareFilter);

        PullFromDashboardRouteLocator pullFromDashboardRouteLocator = new PullFromDashboardRouteLocator(
                configuration.getDashboardAddress(),
                configuration.getDashboardApiContextPath(),
                configuration.getDashboardAuthKey(),
                configuration.getEnvironmentId()
        );
        INSTANCES.put(PullFromDashboardRouteLocator.class, pullFromDashboardRouteLocator);

        GatewayInternalEndpoint gatewayInternalEndpoint = new GatewayInternalEndpoint(GatewayInternalRouteLocator.INTERNAL_CONTEXT_PATH);
        INSTANCES.put(GatewayInternalEndpoint.class, gatewayInternalEndpoint);

        GatewayInternalRouteLocator gatewayInternalRouteLocator = new GatewayInternalRouteLocator(gatewayInternalEndpoint);
        INSTANCES.put(GatewayInternalRouteLocator.class, gatewayInternalRouteLocator);

        HeartbeatReporter heartbeatReporter = new HeartbeatReporter(
                configuration.getDashboardAddress(),
                configuration.getDashboardApiContextPath(),
                configuration.getEnvironmentId(),
                configuration.getHeartbeatInterval(),
                configuration.getServerSchema(),
                configuration.getServerPort()
        );
        INSTANCES.put(HeartbeatReporter.class, heartbeatReporter);

        EventPublisher<RgwEvent> eventPublisher = new EventPublisherImpl<>();
        INSTANCES.put(EventPublisher.class, eventPublisher);

        AppSubscribeRouteManager appSubscribeRouteManager = new AppSubscribeRouteManager(
                configuration.getDashboardAddress(),
                configuration.getDashboardApiContextPath(),
                configuration.getDashboardAuthKey()
        );
        INSTANCES.put(AppSubscribeRouteManager.class, appSubscribeRouteManager);

        AppAuthFilter appAuthFilter = new AppAuthFilter(appSubscribeRouteManager);
        INSTANCES.put(AppAuthFilter.class, appAuthFilter);

        eventPublisher.registerListener(environmentPrepareFilter);
        eventPublisher.registerListener(pullFromDashboardRouteLocator);
        eventPublisher.registerListener(heartbeatReporter);
        eventPublisher.registerListener(appSubscribeRouteManager);

        gatewayInternalEndpoint.setEventPublisher(eventPublisher);
        pullFromDashboardRouteLocator.setEventPublisher(eventPublisher);
        heartbeatReporter.setEventPublisher(eventPublisher);
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(Class<T> clazz) {
        return (T) INSTANCES.get(clazz);
    }

}
