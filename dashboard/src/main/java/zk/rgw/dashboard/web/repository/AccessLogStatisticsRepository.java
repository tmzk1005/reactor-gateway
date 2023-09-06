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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Field;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import org.bson.conversions.Bson;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import zk.rgw.common.util.ObjectUtil;
import zk.rgw.common.util.TimeUtil;
import zk.rgw.dashboard.utils.AccessLogStatisticsHelper;
import zk.rgw.dashboard.web.bean.AccessLogStatistics;
import zk.rgw.dashboard.web.bean.AccessLogStatisticsArchiveLevel;
import zk.rgw.dashboard.web.bean.AccessLogStatisticsWithTime;
import zk.rgw.dashboard.web.bean.TimeRangeType;

@Slf4j
public class AccessLogStatisticsRepository {

    private final MongoDatabase database;

    public AccessLogStatisticsRepository(MongoDatabase database) {
        this.database = database;
    }

    public Mono<Void> archiveSelf(String envId, long beginTime, long endTime, AccessLogStatisticsArchiveLevel level) {
        List<Bson> aggPipelines = new ArrayList<>();

        // step-1 : match
        Bson matchFilter = Filters.and(
                Filters.gte("_id.timestampMillis", Instant.ofEpochMilli(beginTime)),
                Filters.lt("_id.timestampMillis", Instant.ofEpochMilli(endTime))
        );
        aggPipelines.add(Aggregates.match(matchFilter));

        // step-2 : group
        Bson groupDefBson = switch (level) {
            case HOURS -> AccessLogStatisticsHelper.AGG_GROUP_HOUR;
            case DAYS -> AccessLogStatisticsHelper.AGG_GROUP_DAY;
            case MONTHS -> AccessLogStatisticsHelper.AGG_GROUP_MONTH;
            default -> throw new IllegalArgumentException("AccessLogStatisticsArchiveLevel illegal for archive self.");
        };
        aggPipelines.add(groupDefBson);

        // step-3 : merge into AccessLogStatistics
        String targetCollectionName = AccessLogStatisticsHelper.collectionNameForEnvAndArchiveLevel(envId, level);
        Bson merge = Aggregates.merge(targetCollectionName, AccessLogStatisticsHelper.MERGE_OPTIONS);
        aggPipelines.add(merge);

        MongoCollection<AccessLogStatistics> collection = AccessLogStatisticsHelper.getAccessLogStatisticsCollection(
                database, envId, AccessLogStatisticsArchiveLevel.values()[level.ordinal() - 1]
        );

        return Mono.from(collection.aggregate(aggPipelines)).then();
    }

    public Mono<List<AccessLogStatisticsWithTime>> searchAccessLogStatistics(String envId, List<String> apiIds, TimeRangeType timeRangeType) {
        AccessLogStatisticsArchiveLevel level = null;
        Instant now = Instant.now();
        long beginTime = 0;
        long endTime = 0;
        long step = 0;
        switch (timeRangeType) {
            case LAST_ONE_HOUR -> {
                level = AccessLogStatisticsArchiveLevel.MINUTES;
                endTime = TimeUtil.minutesAgoRange(now, 2).getEnd();
                step = 60_000L;
                beginTime = endTime - 60 * step;
            }
            case LAST_SIX_HOUR -> {
                level = AccessLogStatisticsArchiveLevel.HOURS;
                endTime = TimeUtil.lastHourRange(now).getEnd();
                step = 60 * 60_000L;
                beginTime = endTime - 6 * step;
            }
            case LAST_HALF_DAY -> {
                level = AccessLogStatisticsArchiveLevel.HOURS;
                endTime = TimeUtil.lastHourRange(now).getEnd();
                step = 60 * 60_000L;
                beginTime = endTime - 12 * step;
            }
            case LAST_DAY -> {
                level = AccessLogStatisticsArchiveLevel.HOURS;
                endTime = TimeUtil.lastHourRange(now).getEnd();
                step = 60 * 60_000L;
                beginTime = endTime - 24 * step;
            }
            case LAST_MONTH -> {
                level = AccessLogStatisticsArchiveLevel.DAYS;
                endTime = TimeUtil.lastDayRange(now).getEnd();
                step = 24 * 60 * 60_000L;
                beginTime = endTime - 30 * step;
            }
            case LAST_YEAR -> level = AccessLogStatisticsArchiveLevel.MONTHS;
            case ALL_TIME -> level = AccessLogStatisticsArchiveLevel.ALL;
        }

        MongoCollection<AccessLogStatistics> collection = AccessLogStatisticsHelper.getAccessLogStatisticsCollection(database, envId, level);

        List<Bson> aggPipeline = new ArrayList<>();

        Bson matchFilter = null;

        if (level != AccessLogStatisticsArchiveLevel.ALL && endTime != 0 && beginTime != 0) {
            matchFilter = Filters.and(
                    Filters.gte("_id.timestampMillis", Instant.ofEpochMilli(beginTime)),
                    Filters.lt("_id.timestampMillis", Instant.ofEpochMilli(endTime))
            );
        }
        if (!ObjectUtil.isEmpty(apiIds)) {
            Bson apiIdFilter = Filters.in("_id.apiId", apiIds);
            if (Objects.isNull(matchFilter)) {
                matchFilter = apiIdFilter;
            } else {
                matchFilter = Filters.and(matchFilter, apiIdFilter);
            }
        }

        if (Objects.nonNull(matchFilter)) {
            aggPipeline.add(Aggregates.match(matchFilter));
        }

        aggPipeline.add(Aggregates.set(new Field<>("timestampMillis", "$_id.timestampMillis")));
        aggPipeline.add(Aggregates.unset("_id.timestampMillis"));

        aggPipeline.add(
                Aggregates.group(
                        "$timestampMillis",
                        AccessLogStatisticsHelper.COUNT_1_XX,
                        AccessLogStatisticsHelper.COUNT_2_XX,
                        AccessLogStatisticsHelper.COUNT_3_XX,
                        AccessLogStatisticsHelper.COUNT_4_XX,
                        AccessLogStatisticsHelper.COUNT_5_XX,
                        AccessLogStatisticsHelper.MILLIS_COST_SUM,
                        AccessLogStatisticsHelper.UP_FLOW_SUM,
                        AccessLogStatisticsHelper.DOWN_FLOW_SUM
                )
        );

        aggPipeline.add(Aggregates.sort(Sorts.ascending("timestampMillis")));

        Mono<List<AccessLogStatisticsWithTime>> resultMono = Flux.from(
                collection.withDocumentClass(AccessLogStatisticsWithTime.class).aggregate(aggPipeline)
        ).collectList();

        if (step != 0) {
            long finalBeginTime = beginTime;
            long finalEndTime = endTime;
            long finalStep = step;
            resultMono = resultMono.map(list -> fillBlankPoint(list, finalBeginTime, finalEndTime, finalStep));
        }
        return resultMono;
    }

    private List<AccessLogStatisticsWithTime> fillBlankPoint(List<AccessLogStatisticsWithTime> list, long beginTime, long endTime, long step) {
        List<AccessLogStatisticsWithTime> result = new LinkedList<>();

        Iterator<AccessLogStatisticsWithTime> iterator = list.iterator();
        AccessLogStatisticsWithTime nextItem = iterator.hasNext() ? iterator.next() : null;

        long dot = beginTime;

        while (dot < endTime) {
            Instant instant = Instant.ofEpochMilli(dot);
            if (Objects.nonNull(nextItem) && instant.equals(nextItem.getTimestampMillis())) {
                result.add(nextItem);
                nextItem = iterator.hasNext() ? iterator.next() : null;
            } else {
                result.add(new AccessLogStatisticsWithTime(instant));
            }
            dot += step;
        }

        return result;
    }

}
