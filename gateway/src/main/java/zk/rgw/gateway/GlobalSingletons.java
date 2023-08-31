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

import zk.rgw.gateway.env.EnvironmentManager;
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

        EnvironmentManager environmentManager = new EnvironmentManager();
        INSTANCES.put(EnvironmentManager.class, environmentManager);

        EnvironmentPrepareFilter environmentPrepareFilter = new EnvironmentPrepareFilter(environmentManager);
        INSTANCES.put(EnvironmentPrepareFilter.class, environmentPrepareFilter);

        PullFromDashboardRouteLocator pullFromDashboardRouteLocator = new PullFromDashboardRouteLocator(
                configuration.getDashboardAddress(),
                configuration.getDashboardApiContextPath(),
                configuration.getDashboardAuthKey(),
                configuration.getEnvironmentId()
        );
        INSTANCES.put(PullFromDashboardRouteLocator.class, pullFromDashboardRouteLocator);

        GatewayInternalEndpoint gatewayInternalEndpoint = new GatewayInternalEndpoint(
                GatewayInternalRouteLocator.INTERNAL_CONTEXT_PATH,
                pullFromDashboardRouteLocator,
                environmentManager
        );
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
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(Class<T> clazz) {
        return (T) INSTANCES.get(clazz);
    }

}
