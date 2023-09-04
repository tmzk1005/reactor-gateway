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

import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import reactor.core.publisher.Mono;

import zk.rgw.dashboard.framework.context.ContextUtil;
import zk.rgw.dashboard.framework.exception.AccessDeniedException;
import zk.rgw.dashboard.framework.xo.AccessLogStatisticsArchiveLevel;
import zk.rgw.dashboard.web.bean.ApiPublishStatus;
import zk.rgw.dashboard.web.bean.vo.dashboard.ApiCallsCount;
import zk.rgw.dashboard.web.repository.AccessLogRepository;
import zk.rgw.dashboard.web.repository.AccessLogStatisticsRepository;
import zk.rgw.dashboard.web.repository.ApiRepository;
import zk.rgw.dashboard.web.repository.factory.RepositoryFactory;
import zk.rgw.dashboard.web.service.DashboardService;

public class DashboardServiceImpl implements DashboardService {

    private final ApiRepository apiRepository = RepositoryFactory.get(ApiRepository.class);

    private final AccessLogRepository accessLogRepository = RepositoryFactory.get(AccessLogRepository.class);

    private final AccessLogStatisticsRepository accessLogStatisticsRepository = RepositoryFactory.get(AccessLogStatisticsRepository.class);

    @Override
    public Mono<ApiCallsCount> apiCallsCount(String envId, String orgId) {
        Mono<Long> apiCountMono = getApiFilterByEnvAndOrg(envId, orgId).flatMap(apiRepository::count);

        return apiCountMono.map(apiCount -> {
            ApiCallsCount apiCallsCount = new ApiCallsCount();
            apiCallsCount.setApiCount(apiCount.intValue());
            // TODO : set api calls count, succeed and failed
            return apiCallsCount;
        });
    }

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

    private Mono<Bson> getApiFilterByEnvAndOrg(final String envId, final String orgId) {
        Objects.requireNonNull(envId);
        return ContextUtil.getUser().map(user -> {
            Bson filter = Filters.and(
                    Filters.exists("publishSnapshots." + envId),
                    Filters.eq("publishSnapshots." + envId + ".publishStatus", ApiPublishStatus.PUBLISHED.name())
            );

            String finalOrgId;
            String userOrgId = user.getOrganization().getId();

            if (!user.isSystemAdmin() && Objects.nonNull(orgId) && !Objects.equals(orgId, userOrgId)) {
                throw new AccessDeniedException();
            }

            if (user.isSystemAdmin()) {
                finalOrgId = orgId;
            } else {
                finalOrgId = userOrgId;
            }

            if (Objects.nonNull(finalOrgId)) {
                return Filters.and(filter, Filters.eq("organization", new ObjectId(finalOrgId)));
            } else {
                return filter;
            }
        });
    }

}