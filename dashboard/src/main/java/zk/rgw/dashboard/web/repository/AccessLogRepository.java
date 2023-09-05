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
package zk.rgw.dashboard.web.repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Field;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.bson.conversions.Bson;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import zk.rgw.alc.common.AccessLogDocument;
import zk.rgw.alc.common.AccessLogDocumentUtil;
import zk.rgw.common.util.ObjectUtil;
import zk.rgw.dashboard.utils.AccessLogStatisticsHelper;
import zk.rgw.dashboard.web.bean.AccessLogStatistics;
import zk.rgw.dashboard.web.bean.AccessLogStatisticsArchiveLevel;
import zk.rgw.dashboard.web.bean.Page;
import zk.rgw.dashboard.web.bean.PageData;
import zk.rgw.dashboard.web.bean.entity.AccessLogStatisticsDocument;

public class AccessLogRepository {

    private final MongoDatabase database;

    public AccessLogRepository(MongoDatabase mongoDatabase) {
        this.database = mongoDatabase;
    }

    @SuppressWarnings("java:S107")
    public Mono<PageData<AccessLogDocument>> searchAccessLogs(
            String envId, Page page,
            String requestId, List<String> apiIds, String clientIp, List<String> appIds,
            Integer[] responseStatuses, Integer minTimeCostMillis, Long minTimeMillis, Long maxTimeMillis
    ) {
        return AccessLogDocumentUtil.getAccessLogCollectionForEnv(database, envId)
                .flatMap(
                        collection -> searchAccessLogs(
                                collection, page,
                                requestId, apiIds, clientIp, appIds, responseStatuses,
                                minTimeCostMillis, minTimeMillis, maxTimeMillis
                        )
                );
    }

    @SuppressWarnings("java:S107")
    private Mono<PageData<AccessLogDocument>> searchAccessLogs(
            MongoCollection<AccessLogDocument> collection, Page page,
            String requestId, List<String> apiIds, String clientIp, List<String> appIds,
            Integer[] responseStatuses, Integer minTimeCostMillis, Long minTimeMillis, Long maxTimeMillis
    ) {
        List<Bson> aggPipelines = new ArrayList<>(4);

        List<Bson> filters = createFilters(requestId, apiIds, clientIp, appIds, responseStatuses, minTimeCostMillis, minTimeMillis, maxTimeMillis);

        Bson matchFilter = null;

        if (filters.size() == 1) {
            matchFilter = filters.get(0);
        } else if (filters.size() > 1) {
            matchFilter = Filters.and(filters);
        }

        if (Objects.nonNull(matchFilter)) {
            aggPipelines.add(Aggregates.match(matchFilter));
        }

        Bson sorts = Sorts.descending("timestampMillis");

        aggPipelines.add(Aggregates.sort(sorts));
        aggPipelines.add(Aggregates.skip(page.getOffset()));
        aggPipelines.add(Aggregates.limit(page.getPageSize()));

        Mono<List<AccessLogDocument>> dataMono = Flux.from(collection.aggregate(aggPipelines)).collectList();
        Mono<Long> countMono = Mono.from(Objects.nonNull(matchFilter) ? collection.countDocuments(matchFilter) : collection.countDocuments());

        return Mono.zip(dataMono, countMono)
                .map(tuple -> new PageData<>(tuple.getT1(), tuple.getT2(), page.getPageNum(), page.getPageSize()));
    }

    public Mono<Void> archiveAccessLogsByMinutes(String envId, long beginTime, long endTime) {
        List<Bson> aggPipelines = new ArrayList<>();

        // step-1 : match
        Bson matchFilter = Filters.and(
                Filters.gte("timestampMillis", Instant.ofEpochMilli(beginTime)),
                Filters.lt("timestampMillis", Instant.ofEpochMilli(endTime))
        );
        aggPipelines.add(Aggregates.match(matchFilter));

        // step-2 : group
        aggPipelines.add(AccessLogStatisticsHelper.AGG_GROUP_MINUTE);

        // step-3 : merge into AccessLogStatistics
        String targetCollectionName = AccessLogStatisticsHelper.collectionNameForEnvAndArchiveLevel(envId, AccessLogStatisticsArchiveLevel.MINUTES);
        Bson merge = Aggregates.merge(targetCollectionName, AccessLogStatisticsHelper.MERGE_OPTIONS);
        aggPipelines.add(merge);

        return AccessLogDocumentUtil.getAccessLogCollectionForEnv(database, envId)
                .flatMap(collection -> Mono.from(collection.withDocumentClass(AccessLogStatisticsDocument.class).aggregate(aggPipelines)).then())
                .then(accumulateToTotal(envId, beginTime));
    }

    private Mono<Void> accumulateToTotal(String envId, long beginTime) {
        List<Bson> aggPipelines = new ArrayList<>();

        Bson filter = Filters.gte("_id.timestampMillis", Instant.ofEpochMilli(beginTime));
        aggPipelines.add(Aggregates.match(filter));

        // 把timestampMillis设置为一个固定的值
        aggPipelines.add(Aggregates.set(new Field<>("_id.timestampMillis", Instant.ofEpochMilli(0))));

        String totalCollectionName = AccessLogStatisticsHelper.collectionNameForEnvAndArchiveLevel(envId, AccessLogStatisticsArchiveLevel.ALL);
        aggPipelines.add(Aggregates.merge(totalCollectionName, AccessLogStatisticsHelper.MERGE_OPTIONS_FOR_TOTAL));

        MongoCollection<AccessLogStatistics> minutesCollection = AccessLogStatisticsHelper.getAccessLogStatisticsCollection(
                database, envId, AccessLogStatisticsArchiveLevel.MINUTES
        );
        return Mono.from(minutesCollection.aggregate(aggPipelines)).then();
    }

    @SuppressWarnings({ "java:S3776", "java:S107" })
    private static List<Bson> createFilters(
            String requestId, List<String> apiIds, String clientIp, List<String> appIds,
            Integer[] responseStatuses, Integer minTimeCostMillis, Long minTimeMillis, Long maxTimeMillis
    ) {
        List<Bson> filters = new ArrayList<>(4);

        if (!ObjectUtil.isEmpty(apiIds)) {
            // 注意， AccessLogDocument中apiId字段在mongodb中是String, 不是ObjectId
            if (apiIds.size() == 1) {
                filters.add(Filters.eq("apiId", apiIds.get(0)));
            } else {
                filters.add(Filters.in("apiId", apiIds.stream().toList()));
            }
        }

        if (!ObjectUtil.isEmpty(appIds)) {
            // 注意， AccessLogDocument中clientInfo.appId字段在mongodb中是String, 不是ObjectId
            if (appIds.size() == 1) {
                filters.add(Filters.eq("clientInfo.appId", appIds.get(0)));
            } else {
                filters.add(Filters.in("clientInfo.appId", appIds.stream().toList()));
            }
        }

        if (Objects.nonNull(clientIp)) {
            filters.add(Filters.eq("clientInfo.ip", clientIp));
        }

        if (Objects.nonNull(requestId)) {
            filters.add(Filters.eq("requestId", requestId));
        }

        if (Objects.nonNull(minTimeCostMillis) && minTimeCostMillis > 0) {
            filters.add(Filters.gte("millisCost", minTimeCostMillis));
        }

        if (Objects.nonNull(minTimeMillis) && minTimeMillis >= 0) {
            filters.add(Filters.gte("timestampMillis", Instant.ofEpochMilli(minTimeMillis)));
        }

        if (Objects.nonNull(maxTimeMillis) && maxTimeMillis >= 0) {
            filters.add(Filters.lt("timestampMillis", Instant.ofEpochMilli(maxTimeMillis)));
        }

        if (!ObjectUtil.isEmpty(responseStatuses)) {
            filters.add(createFilterForResponseStatus(Arrays.stream(responseStatuses).toList()));
        }
        return filters;
    }

    private static Bson createFilterForResponseStatus(List<Integer> responseStatuses) {
        Set<Integer> bigCodes = new HashSet<>(8);
        bigCodes.addAll(responseStatuses.stream().filter(code -> code < 10).toList());
        List<Integer> leftCodes = new ArrayList<>();
        responseStatuses.forEach(code -> {
            if (code >= 100 && !bigCodes.contains(code / 100)) {
                leftCodes.add(code);
            }
        });
        List<Bson> filters = new ArrayList<>(2);

        if (!bigCodes.isEmpty()) {
            List<Bson> bigCodeFilters = new ArrayList<>(bigCodes.size());
            for (Integer bigCode : bigCodes) {
                bigCodeFilters.add(
                        Filters.and(
                                Filters.gte("responseInfo.code", bigCode * 100),
                                Filters.lt("responseInfo.code", (bigCode + 1) * 100)
                        )
                );
            }
            if (bigCodeFilters.size() == 1) {
                filters.add(bigCodeFilters.get(0));
            } else {
                filters.add(Filters.or(bigCodeFilters));
            }
        }

        if (!leftCodes.isEmpty()) {
            if (leftCodes.size() == 1) {
                filters.add(Filters.eq("responseInfo.code", leftCodes.get(0)));
            } else {
                filters.add(Filters.in("responseInfo.code", leftCodes));
            }
        }
        if (filters.size() == 1) {
            return filters.get(0);
        } else {
            return Filters.or(filters.get(0), filters.get(1));
        }
    }

}
