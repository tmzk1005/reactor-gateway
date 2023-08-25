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

package zk.rgw.http.route.locator;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AsyncUpdatableRouteLocator extends UpdatableRouteLocator {

    public static final int DEFAULT_UPDATE_INTERVAL = 10;

    private final AtomicBoolean updated = new AtomicBoolean(false);

    private final int periodInSeconds;

    private final ScheduledExecutorService scheduledExecutorService;

    protected AsyncUpdatableRouteLocator() {
        this(DEFAULT_UPDATE_INTERVAL);
    }

    protected AsyncUpdatableRouteLocator(int updatePeriodInSeconds) {
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        this.periodInSeconds = updatePeriodInSeconds;
    }

    @Override
    public synchronized void update() {
        updated.set(true);
    }

    @Override
    public void start() {
        if (scheduledExecutorService.isShutdown() || scheduledExecutorService.isTerminated()) {
            log.error("Can not start {}} because it's already started.", this.getClass().getSimpleName());
            return;
        }
        log.info("Start {}.", this.getClass().getSimpleName());
        updated.set(true);
        scheduledExecutorService.scheduleAtFixedRate(this::doUpdate, 0, periodInSeconds, TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        if (!scheduledExecutorService.isShutdown()) {
            log.info("Stop {}.", this.getClass().getSimpleName());
            scheduledExecutorService.shutdownNow();
        }
    }

    @Override
    protected void doUpdate() {
        if (!updated.compareAndExchange(true, false)) {
            return;
        }

        super.doUpdate();
    }

}
