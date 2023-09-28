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

import java.util.Objects;
import java.util.concurrent.CountDownLatch;

import lombok.Setter;
import reactor.core.publisher.Flux;

import zk.rgw.http.route.Route;
import zk.rgw.http.route.locator.RouteLocator;
import zk.rgw.http.server.ReactorHttpServer;

public class TestGatewayServer extends ReactorHttpServer {

    public static final String HOST = "127.0.0.1";

    public static final int PORT = 8000;

    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    @Setter
    private Route route;

    public TestGatewayServer() {
        super(HOST, PORT);
    }

    @Override
    protected RouteLocator getRouteLocator() {
        return new TestRouteLocator();
    }

    class TestRouteLocator implements RouteLocator {

        @Override
        public Flux<Route> getRoutes(String path) {
            if (Objects.isNull(route)) {
                return Flux.empty();
            } else {
                return Flux.just(route);
            }
        }

    }

    @Override
    protected void afterStart() {
        countDownLatch.countDown();
    }

    public void waitStarted() throws InterruptedException {
        countDownLatch.await();
    }

}
