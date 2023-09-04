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
import java.util.concurrent.TimeUnit;

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
        scheduledExecutorService = Executors.newScheduledThreadPool(4);
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
        environmentRepository.findAll().doOnNext(environment -> {
            scheduledExecutorService.submit(() -> archive(environment.getId(), now).subscribe());
        }).subscribe();
    }

    private Mono<Void> archive(String envId, Instant instant) {
        return archiveProgressRepository.save(envId, instant).flatMap(succeed -> {
            if (Boolean.TRUE.equals(succeed)) {
                return doArchive(envId, instant);
            } else {
                return Mono.empty();
            }
        });
    }

    private Mono<Void> doArchive(String envId, Instant instant) {
        log.debug("Going to archive access logs from environment {}", envId);
        // 对2分钟之前的数据进行归档
        TimeUtil.Range twoMinutesAgoRange = TimeUtil.minutesAgoRange(instant, 2);
        Mono<Void> mono = accessLogArchiveService.archiveAccessLog(
                envId, twoMinutesAgoRange.getBegin(),
                twoMinutesAgoRange.getEnd(), AccessLogStatisticsArchiveLevel.MINUTES
        );

        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, TimeUtil.TZ_ID);

        if (localDateTime.getMinute() == 4) {
            TimeUtil.Range lastHourRange = TimeUtil.lastHourRange(instant);
            mono = mono.then(
                    accessLogArchiveService.archiveAccessLog(
                            envId, lastHourRange.getBegin(),
                            lastHourRange.getEnd(), AccessLogStatisticsArchiveLevel.HOURS
                    )
            );
        }

        if (localDateTime.getHour() == 0 && localDateTime.getMinute() == 5) {
            TimeUtil.Range lastDayRange = TimeUtil.lastDayRange(instant);
            mono = mono.then(
                    accessLogArchiveService.archiveAccessLog(
                            envId, lastDayRange.getBegin(),
                            lastDayRange.getEnd(), AccessLogStatisticsArchiveLevel.DAYS
                    )
            );
        }

        if (localDateTime.getDayOfMonth() == 1 && localDateTime.getHour() == 0 && localDateTime.getMinute() == 10) {
            TimeUtil.Range lastMonthRange = TimeUtil.lastMonthRange(instant);
            mono = mono.then(
                    accessLogArchiveService.archiveAccessLog(
                            envId, lastMonthRange.getBegin(),
                            lastMonthRange.getEnd(), AccessLogStatisticsArchiveLevel.MONTHS
                    )
            );
        }

        return mono;
    }

}
