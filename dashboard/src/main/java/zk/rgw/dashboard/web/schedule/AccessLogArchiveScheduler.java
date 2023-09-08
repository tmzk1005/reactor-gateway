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
package zk.rgw.dashboard.web.schedule;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import zk.rgw.common.bootstrap.LifeCycle;
import zk.rgw.common.util.TimeUtil;
import zk.rgw.dashboard.web.bean.AccessLogStatisticsArchiveLevel;
import zk.rgw.dashboard.web.repository.ArchiveProgressRepository;
import zk.rgw.dashboard.web.repository.EnvironmentRepository;
import zk.rgw.dashboard.web.repository.factory.RepositoryFactory;
import zk.rgw.dashboard.web.service.AccessLogArchiveService;
import zk.rgw.dashboard.web.service.impl.AccessLogArchiveServiceImpl;

@Slf4j
public class AccessLogArchiveScheduler implements LifeCycle {

    private ScheduledExecutorService scheduledExecutorService;

    // 直接new, 不从Factory获取代理，这样不需要校验角色
    private final AccessLogArchiveService accessLogArchiveService = new AccessLogArchiveServiceImpl();

    private final EnvironmentRepository environmentRepository = RepositoryFactory.get(EnvironmentRepository.class);

    private final ArchiveProgressRepository archiveProgressRepository = RepositoryFactory.get(ArchiveProgressRepository.class);

    @Override
    public void start() {
        scheduledExecutorService = Executors.newScheduledThreadPool(4, new ArchiveThreadFactory());
        scheduledExecutorService.scheduleAtFixedRate(this::runArchive, 0, 1, TimeUnit.MINUTES);
    }

    @Override
    public void stop() {
        if (Objects.nonNull(scheduledExecutorService)) {
            scheduledExecutorService.shutdown();
        }
    }

    private void runArchive() {
        final Instant now = Instant.now();
        environmentRepository.findAll().doOnNext(
                environment -> scheduledExecutorService.submit(() -> archive(environment.getId(), now).subscribe())
        ).subscribe();
    }

    private Mono<Void> archive(String envId, Instant instant) {
        // 对2分钟之前的数据进行归档
        long twoMinutesAgo = TimeUtil.minutesAgo(instant, 2);

        return archiveProgressRepository.save(envId, Instant.ofEpochMilli(twoMinutesAgo)).flatMap(succeed -> {
            if (Boolean.TRUE.equals(succeed)) {
                log.debug("Going to archive access logs for environment {} and time {}", envId, twoMinutesAgo);
                return doArchive(envId, instant);
            } else {
                log.debug("Failed to write archive progress for environment {} and time {} , another node got the chance", envId, twoMinutesAgo);
                return Mono.empty();
            }
        });
    }

    private Mono<Void> doArchive(String envId, Instant instant) {
        long twoMinutesAgo = TimeUtil.minutesAgo(instant, 2);

        Mono<Void> mono = accessLogArchiveService.archiveAccessLog(
                envId, twoMinutesAgo, twoMinutesAgo + TimeUtil.MINUTE_IN_MILLS, AccessLogStatisticsArchiveLevel.MINUTES
        );

        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, TimeUtil.TZ_ID);

        if (localDateTime.getMinute() == 4) {
            long hourAgo = TimeUtil.hoursAgo(instant, 1);
            mono = mono.then(
                    accessLogArchiveService.archiveAccessLog(
                            envId, hourAgo, hourAgo + TimeUtil.HOUR_IN_MILLS, AccessLogStatisticsArchiveLevel.HOURS
                    )
            );
        }

        if (localDateTime.getHour() == 0 && localDateTime.getMinute() == 5) {
            long dayAgo = TimeUtil.daysAgo(instant, 1);
            mono = mono.then(
                    accessLogArchiveService.archiveAccessLog(
                            envId, dayAgo, dayAgo + TimeUtil.DAY_IN_MILLIS, AccessLogStatisticsArchiveLevel.DAYS
                    )
            );
        }

        return mono;
    }

    public static class ArchiveThreadFactory implements ThreadFactory {

        private final AtomicLong seq = new AtomicLong(0);

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable);
            thread.setName("AccessLogArchiveThread-" + seq.incrementAndGet());
            thread.setDaemon(true);
            thread.setPriority(Thread.MIN_PRIORITY);
            return thread;
        }

    }

}
