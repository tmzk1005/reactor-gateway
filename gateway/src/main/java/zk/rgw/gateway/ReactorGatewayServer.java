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
import zk.rgw.gateway.accesslog.AccessLogFilter;
import zk.rgw.gateway.accesslog.AccessLogKafkaWriter;
import zk.rgw.gateway.heartbeat.HeartbeatReporter;
import zk.rgw.gateway.internal.GatewayInternalRouteLocator;
import zk.rgw.gateway.route.PullFromDashboardRouteLocator;
import zk.rgw.gateway.route.RouteConverter;
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
        initRouteLocator();
        HeartbeatReporter heartbeatReporter = new HeartbeatReporter(
                configuration.getDashboardAddress(),
                configuration.getDashboardApiContextPath(),
                configuration.getEnvironmentId(),
                configuration.getHeartbeatInterval(),
                configuration.getServerSchema(),
                configuration.getServerPort()
        );
        heartbeatReporter.start();
        this.lifeCycles.add(heartbeatReporter);
    }

    private void initRouteLocator() {
        AccessLogKafkaWriter accessLogKafkaWriter = new AccessLogKafkaWriter(configuration.getKafkaBootstrapServers(), configuration.getEnvironmentId());

        accessLogKafkaWriter.start();
        this.lifeCycles.add(accessLogKafkaWriter);

        AccessLogFilter accessLogFilter = new AccessLogFilter(accessLogKafkaWriter);

        RouteConverter.setAccessLogFilter(accessLogFilter);

        PullFromDashboardRouteLocator pullFromDashboardRouteLocator = new PullFromDashboardRouteLocator(
                configuration.getDashboardAddress(),
                configuration.getDashboardApiContextPath(),
                configuration.getDashboardAuthKey(),
                configuration.getEnvironmentId()
        );
        pullFromDashboardRouteLocator.start();
        this.lifeCycles.add(pullFromDashboardRouteLocator);

        this.routeLocator = new CompositeRouteLocator(
                new GatewayInternalRouteLocator(pullFromDashboardRouteLocator),
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
