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

package zk.rgw.http.server;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.extern.slf4j.Slf4j;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import zk.rgw.common.bootstrap.Server;
import zk.rgw.http.route.locator.RouteLocator;

@Slf4j
public abstract class ReactorHttpServer implements Server {

    protected final String host;

    protected final int port;

    protected DisposableServer httpd;

    private final AtomicBoolean running = new AtomicBoolean(false);

    protected ReactorHttpServer(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        beforeStart();
        HttpHandler httpHandler = new HttpHandler(this.getRouteLocator());
        try {
            this.httpd = HttpServer.create().host(host).port(port).handle(httpHandler).bindNow();
        } catch (Exception exception) {
            running.set(false);
            log.error(this.getClass().getSimpleName() + " start failed.", exception);
            return;
        }
        afterStart();
        this.httpd.onDispose().block();
    }

    @Override
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        try {
            this.httpd.disposeNow(Duration.ofSeconds(5));
        } catch (Exception exception) {
            log.error("Exception happened while try to stop {}.", this.getClass().getSimpleName(), exception);
        }
        this.httpd = null;
        afterStop();
        log.info("{} stopped.", this.getClass().getSimpleName());
    }

    protected abstract RouteLocator getRouteLocator();

    protected void afterStart() {
    }

    protected void beforeStart() {
    }

    protected void afterStop() {
    }

}
