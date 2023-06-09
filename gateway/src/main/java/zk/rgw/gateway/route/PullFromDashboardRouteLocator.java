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

import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;

import zk.rgw.http.route.RouteEvent;
import zk.rgw.http.route.locator.AsyncUpdatableRouteLocator;

public class PullFromDashboardRouteLocator extends AsyncUpdatableRouteLocator {

    private final HttpClient httpClient = HttpClient.create();

    private final String dashboardAddress;

    private final String dashboardRouteSyncEndpoint;

    private final String dashboardAuthKey;

    private long latestTimestamp = 0L;

    public PullFromDashboardRouteLocator(String dashboardAddress, String dashboardRouteSyncEndpoint, String dashboardAuthKey) {
        this.dashboardAddress = dashboardAddress;
        this.dashboardRouteSyncEndpoint = dashboardRouteSyncEndpoint;
        this.dashboardAuthKey = dashboardAuthKey;
    }

    @Override
    protected Flux<RouteEvent> fetchRouteChange() {
        // TODO
        System.out.println(httpClient);
        System.out.println(dashboardAddress);
        System.out.println(dashboardRouteSyncEndpoint);
        System.out.println(dashboardAuthKey);
        latestTimestamp += 1;
        System.out.println(latestTimestamp);
        return Flux.empty();
    }

}
