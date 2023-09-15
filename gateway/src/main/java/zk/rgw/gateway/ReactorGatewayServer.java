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

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import zk.rgw.common.bootstrap.LifeCycle;
import zk.rgw.gateway.accesslog.AccessLogKafkaWriter;
import zk.rgw.gateway.heartbeat.HeartbeatReporter;
import zk.rgw.gateway.internal.GatewayInternalRouteLocator;
import zk.rgw.gateway.route.PullFromDashboardRouteLocator;
import zk.rgw.http.route.locator.CompositeRouteLocator;
import zk.rgw.http.route.locator.RouteLocator;
import zk.rgw.http.server.ReactorHttpServer;

@Slf4j
public class ReactorGatewayServer extends ReactorHttpServer {

    private final GatewayConfiguration configuration;

    private RouteLocator routeLocator;

    private final List<LifeCycle> lifeCycles = new ArrayList<>();

    protected ReactorGatewayServer(GatewayConfiguration configuration) {
        super(configuration.getServerHost(), configuration.getServerPort());
        this.configuration = configuration;
    }

    @Override
    protected RouteLocator getRouteLocator() {
        return routeLocator;
    }

    @Override
    protected void beforeStart() {
        GlobalSingletons.init(this.configuration);
        initRouteLocator();
        HeartbeatReporter heartbeatReporter = GlobalSingletons.get(HeartbeatReporter.class);
        heartbeatReporter.start();
        this.lifeCycles.add(heartbeatReporter);
    }

    private void initRouteLocator() {
        if (configuration.isAccessLogEnabled()) {
            AccessLogKafkaWriter accessLogKafkaWriter = new AccessLogKafkaWriter(configuration.getKafkaBootstrapServers(), configuration.getEnvironmentId());

            accessLogKafkaWriter.start();
            this.lifeCycles.add(accessLogKafkaWriter);
        }

        PullFromDashboardRouteLocator pullFromDashboardRouteLocator = GlobalSingletons.get(PullFromDashboardRouteLocator.class);
        pullFromDashboardRouteLocator.start();
        this.lifeCycles.add(pullFromDashboardRouteLocator);

        GatewayInternalRouteLocator gatewayInternalRouteLocator = GlobalSingletons.get(GatewayInternalRouteLocator.class);

        this.routeLocator = new CompositeRouteLocator(
                gatewayInternalRouteLocator,
                pullFromDashboardRouteLocator
        );
    }

    @Override
    protected void afterStart() {
        log.info(
                "{} Started, RGW_HOME is {}, configuration file is {}, http service listening on {}:{}",
                this.getClass().getSimpleName(),
                configuration.getRgwHome(), configuration.getConfFile(),
                host, port
        );
    }

    @Override
    protected void afterStop() {
        for (int i = lifeCycles.size() - 1; i >= 0; i--) {
            lifeCycles.get(i).stop();
        }
    }
}
