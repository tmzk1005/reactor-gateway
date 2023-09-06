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

import java.util.List;
import java.util.Objects;

import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import zk.rgw.common.util.ObjectUtil;
import zk.rgw.dashboard.framework.exception.AccessDeniedException;
import zk.rgw.dashboard.utils.OrgIdDecideUtil;
import zk.rgw.dashboard.web.bean.AccessLogStatistics;
import zk.rgw.dashboard.web.bean.AccessLogStatisticsWithTime;
import zk.rgw.dashboard.web.bean.ApiPublishStatus;
import zk.rgw.dashboard.web.bean.TimeRangeType;
import zk.rgw.dashboard.web.repository.AccessLogStatisticsRepository;
import zk.rgw.dashboard.web.repository.ApiRepository;
import zk.rgw.dashboard.web.repository.factory.RepositoryFactory;
import zk.rgw.dashboard.web.service.DashboardService;

public class DashboardServiceImpl implements DashboardService {

    private final ApiRepository apiRepository = RepositoryFactory.get(ApiRepository.class);

    private final AccessLogStatisticsRepository accessLogStatisticsRepository = RepositoryFactory.get(AccessLogStatisticsRepository.class);

    @Override
    public Mono<Long> apisCount(String envId, String orgId) {
        return getApiFilterByEnvAndOrg(envId, orgId).flatMap(apiRepository::count);
    }

    @Override
    public Flux<AccessLogStatisticsWithTime> apiCallsCountTrend(String envId, String orgId, String apiId, TimeRangeType timeRangeType) {
        return OrgIdDecideUtil.decideOrgId(orgId).flatMapMany(finalOrgId -> {
            Mono<Boolean> boolMono;
            if (!ObjectUtil.isEmpty(finalOrgId) && !ObjectUtil.isEmpty(apiId)) {
                boolMono = apiRepository.belongsToOrg(apiId, finalOrgId);
            } else {
                boolMono = Mono.just(Boolean.TRUE);
            }

            return boolMono.flatMapMany(boolValue -> {
                if (Boolean.TRUE.equals(boolValue)) {
                    return doComputeApiCallsCount(envId, finalOrgId, apiId, timeRangeType);
                } else {
                    throw new AccessDeniedException();
                }
            });
        });
    }

    @Override
    public Mono<AccessLogStatistics> apiCallsCount(String envId, String orgId, String apiId) {
        return apiCallsCountTrend(envId, orgId, apiId, TimeRangeType.ALL_TIME)
                .next()
                .switchIfEmpty(Mono.just(new AccessLogStatisticsWithTime())).map(item -> {
                    item.setTimestampMillis(null);
                    return item;
                });
    }

    private Mono<Bson> getApiFilterByEnvAndOrg(final String envId, final String orgId) {
        Objects.requireNonNull(envId);
        return OrgIdDecideUtil.decideOrgId(orgId).map(finalOrgId -> {
            Bson filter = Filters.and(
                    Filters.exists("publishSnapshots." + envId),
                    Filters.eq("publishSnapshots." + envId + ".publishStatus", ApiPublishStatus.PUBLISHED.name())
            );
            if (Objects.nonNull(finalOrgId)) {
                return Filters.and(filter, Filters.eq("organization", new ObjectId(finalOrgId)));
            } else {
                return filter;
            }
        });
    }

    private Flux<AccessLogStatisticsWithTime> doComputeApiCallsCount(String envId, String orgId, String apiId, TimeRangeType timeRangeType) {
        Mono<List<String>> apiIdsMono;
        if (!ObjectUtil.isEmpty(apiId)) {
            apiIdsMono = Mono.just(List.of(apiId));
        } else if (!ObjectUtil.isEmpty(orgId)) {
            apiIdsMono = apiRepository.getApiIdsByOrgId(orgId).collectList();
        } else {
            apiIdsMono = Mono.just(List.of());
        }
        return apiIdsMono.flatMapMany(apiIds -> accessLogStatisticsRepository.searchAccessLogStatistics(envId, apiIds, timeRangeType));
    }

}
