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
import java.util.List;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import org.bson.conversions.Bson;
import reactor.core.publisher.Mono;

import zk.rgw.dashboard.utils.AccessLogStatisticsHelper;
import zk.rgw.dashboard.web.bean.AccessLogStatistics;
import zk.rgw.dashboard.web.bean.AccessLogStatisticsArchiveLevel;

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

    public Mono<AccessLogStatistics> getAccessLogStatistics() {
        // TODO
        return Mono.empty();
    }

}
