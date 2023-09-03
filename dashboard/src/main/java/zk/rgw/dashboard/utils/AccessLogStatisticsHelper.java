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
package zk.rgw.dashboard.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.BsonField;
import com.mongodb.client.model.MergeOptions;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;

import zk.rgw.dashboard.framework.xo.AccessLogStatistics;
import zk.rgw.dashboard.framework.xo.AccessLogStatisticsArchiveLevel;

@Slf4j
public class AccessLogStatisticsHelper {

    private static final String BASE_COLLECTION_NAME = "AccessLogStatistics";

    private static final Document GROUP_ID_DEF = Document.parse(
            """
                    {
                        "apiId": "$apiId",
                        "timestampMillis": {
                            "$dateTrunc": {
                                "date": "$timestampMillis",
                                "unit": "minute",
                                "binSize": 1
                            }
                        }
                    }"""
    );

    public static final Bson AGG_GROUP_DEF = Aggregates.group(
            GROUP_ID_DEF,
            AccessLogStatisticsHelper.accumulatorForRespCode(1),
            AccessLogStatisticsHelper.accumulatorForRespCode(2),
            AccessLogStatisticsHelper.accumulatorForRespCode(3),
            AccessLogStatisticsHelper.accumulatorForRespCode(4),
            AccessLogStatisticsHelper.accumulatorForRespCode(5),
            Accumulators.sum("millisCost", "$millisCost"),
            Accumulators.sum("upFlow", "$requestInfo.bodySize"),
            Accumulators.sum("downFlow", "$responseInfo.bodySize")
    );

    public static final MergeOptions MERGE_OPTIONS = new MergeOptions()
            .whenMatched(MergeOptions.WhenMatched.REPLACE);

    private AccessLogStatisticsHelper() {
    }

    private static final Map<String, MongoCollection<AccessLogStatistics>> COLLECTION_MAP = new HashMap<>();

    public static String collectionNameForEnvAndArchiveLevel(String envId, AccessLogStatisticsArchiveLevel level) {
        return BASE_COLLECTION_NAME + "_" + level.name() + "_" + envId;
    }

    public static MongoCollection<AccessLogStatistics> getAccessLogStatisticsCollection(
            MongoDatabase mongoDatabase,
            String envId,
            AccessLogStatisticsArchiveLevel level
    ) {
        if (COLLECTION_MAP.containsKey(envId)) {
            return COLLECTION_MAP.get(envId);
        }
        String collectionName = collectionNameForEnvAndArchiveLevel(envId, level);
        MongoCollection<AccessLogStatistics> collection = mongoDatabase.getCollection(collectionName, AccessLogStatistics.class);
        COLLECTION_MAP.put(envId, collection);
        return collection;
    }

    public static BsonField accumulatorForRespCode(int bigCode) {
        String fieldName = "count" + bigCode + "xx";
        String initFunc = "function () { return { count: 0 } }";

        String accumulateFunc = String.format(
                "function (state, code) { if (Math.round(code / 100) == %d) { return { count: state.count + 1 } } else { return state } }",
                bigCode
        );

        List<String> accumulateArgs = List.of("$responseInfo.code");
        String mergeFunc = "function (state1, state2) { return { count: state1.count + state2.count } }";
        String finalizeFunction = "function (state) { return state.count }";
        String lang = "js";
        return Accumulators.accumulator(fieldName, initFunc, null, accumulateFunc, accumulateArgs, mergeFunc, finalizeFunction, lang);
    }

}
