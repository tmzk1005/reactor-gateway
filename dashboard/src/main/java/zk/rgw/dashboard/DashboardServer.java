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

package zk.rgw.dashboard;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import zk.rgw.common.bootstrap.LifeCycle;
import zk.rgw.common.util.StringUtil;
import zk.rgw.dashboard.framework.mongodb.MongodbContext;
import zk.rgw.dashboard.framework.security.UserJwtUtil;
import zk.rgw.dashboard.web.schedule.AccessLogArchiveScheduler;
import zk.rgw.http.route.locator.RouteLocator;
import zk.rgw.http.server.HttpHandler;
import zk.rgw.http.server.ReactorHttpServer;

@Slf4j
public class DashboardServer extends ReactorHttpServer {

    private final DashboardConfiguration configuration;

    private RouteLocator routeLocator;

    @Getter
    private MongodbContext mongodbContext;

    private final List<LifeCycle> lifeCycles = new ArrayList<>();

    public DashboardServer(DashboardConfiguration configuration) {
        super(configuration.getServerHost(), configuration.getServerPort());
        this.configuration = configuration;
    }

    @Override
    protected HttpHandler getHttpHandler() {
        Path staticResourceRoot = configuration.getRgwHome().resolve("static");
        return new DashboardHttpHandler(getRouteLocator(), configuration.getApiContextPath(), staticResourceRoot);
    }

    @Override
    protected RouteLocator getRouteLocator() {
        return routeLocator;
    }

    @Override
    protected void beforeStart() {
        String jwtHmac256Secret = configuration.getJwtHmac256Secret();
        if (Objects.isNull(jwtHmac256Secret) || !StringUtil.hasText(jwtHmac256Secret)) {
            jwtHmac256Secret = UUID.randomUUID().toString();
            configuration.setJwtHmac256Secret(jwtHmac256Secret);
        }
        UserJwtUtil.init(configuration.getJwtHmac256Secret(), configuration.getJwtExpireSeconds());
        // 先初始化mongodb，背后会初始化Repository相关类，然后在初始化RouteLocator时背后会初始化Controller和Service时才能引用到非null的Repository
        initMongodb();
        initRouteLocator();
    }

    private void initRouteLocator() {
        this.routeLocator = new DashboardRoutes(configuration.getApiContextPath());
    }

    private void initMongodb() {
        mongodbContext = new MongodbContext(configuration.getMongodbConnectString(), configuration.getMongodbDatabaseName());
        mongodbContext.init();
    }

    @Override
    protected void afterStart() {
        log.info(
                "{} Started, RGW_HOME is {}, configuration file is {}, http service listening on {}:{}",
                this.getClass().getSimpleName(),
                configuration.getRgwHome(), configuration.getConfFile(),
                host, port
        );
        AccessLogArchiveScheduler accessLogArchiveScheduler = new AccessLogArchiveScheduler();
        lifeCycles.add(accessLogArchiveScheduler);
        accessLogArchiveScheduler.start();
    }

    @Override
    protected void afterStop() {
        for (int i = lifeCycles.size() - 1; i >= 0; i--) {
            lifeCycles.get(i).stop();
        }
    }

}
