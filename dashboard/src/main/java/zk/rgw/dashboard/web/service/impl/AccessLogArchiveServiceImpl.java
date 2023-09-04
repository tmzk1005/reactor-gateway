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
package zk.rgw.dashboard.web.service.impl;

import java.util.Objects;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import zk.rgw.dashboard.web.bean.AccessLogStatisticsArchiveLevel;
import zk.rgw.dashboard.web.lock.MongoLock;
import zk.rgw.dashboard.web.repository.AccessLogRepository;
import zk.rgw.dashboard.web.repository.AccessLogStatisticsRepository;
import zk.rgw.dashboard.web.repository.MongoLockRepository;
import zk.rgw.dashboard.web.repository.factory.RepositoryFactory;
import zk.rgw.dashboard.web.service.AccessLogArchiveService;

@Slf4j
public class AccessLogArchiveServiceImpl implements AccessLogArchiveService {

    private final AccessLogRepository accessLogRepository = RepositoryFactory.get(AccessLogRepository.class);

    private final AccessLogStatisticsRepository accessLogStatisticsRepository = RepositoryFactory.get(AccessLogStatisticsRepository.class);

    private final MongoLockRepository mongoLockRepository = RepositoryFactory.get(MongoLockRepository.class);

    @Override
    public Mono<Void> archiveAccessLog(String envId, long minTimestamp, long maxTimestamp, AccessLogStatisticsArchiveLevel level) {
        Objects.requireNonNull(envId);
        Objects.requireNonNull(level);
        if (level == AccessLogStatisticsArchiveLevel.MINUTES) {
            return accessLogRepository.archiveAccessLogsByMinutes(envId, minTimestamp, maxTimestamp);
        } else {
            return accessLogStatisticsRepository.archiveSelf(envId, minTimestamp, maxTimestamp, level);
        }
    }

    @Override
    public Mono<Void> archiveAccessLogForNow(String envId) {
        String lockName = "access_log_archive_for_env_" + envId;
        MongoLock lock = mongoLockRepository.getLock(lockName);
        return lock.tryLock().filter(Boolean.TRUE::equals)
                .flatMap(locked -> doArchiveAccessLogForNow(envId).then(lock.release()))
                .onErrorResume(throwable -> lock.release());
    }

    private Mono<Void> doArchiveAccessLogForNow(String envId) {
        log.info("Going to archive access logs from environment {}", envId);
        // TODO
        return Mono.empty();
    }

}
