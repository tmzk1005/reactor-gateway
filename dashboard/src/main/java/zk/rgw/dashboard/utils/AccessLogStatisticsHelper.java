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

import java.util.List;

import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.BsonField;
import com.mongodb.client.model.Field;
import com.mongodb.client.model.MergeOptions;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;

import zk.rgw.dashboard.web.bean.AccessLogStatistics;
import zk.rgw.dashboard.web.bean.AccessLogStatisticsArchiveLevel;

@Slf4j
public class AccessLogStatisticsHelper {

    private static final String BASE_COLLECTION_NAME = "AccessLogStatistics";

    public static final Bson AGG_GROUP_MINUTE;
    public static final Bson AGG_GROUP_HOUR;
    public static final Bson AGG_GROUP_DAY;
    public static final Bson AGG_GROUP_MONTH;

    public static final MergeOptions MERGE_OPTIONS = new MergeOptions().whenMatched(MergeOptions.WhenMatched.REPLACE);

    public static final MergeOptions MERGE_OPTIONS_FOR_TOTAL;

    static {
        // init some constant
        String groupIdMinuteDef = """
                {
                    "apiId": "$apiId",
                    "timestampMillis": {
                        "$dateTrunc": {
                            "date": "$timestampMillis",
                            "unit": "minute",
                            "binSize": 1
                        }
                    }
                }""";
        Document groupIdMinute = Document.parse(groupIdMinuteDef);

        String template = """
                {
                    "apiId": "$_id.apiId",
                    "timestampMillis": {
                        "$dateTrunc": {
                            "date": "$_id.timestampMillis",
                            "unit": "%s",
                            "binSize": 1
                        }
                    }
                }""";
        Document groupIdHour = Document.parse(String.format(template, "hour"));
        Document groupIdDay = Document.parse(String.format(template, "day"));
        Document groupIdMonth = Document.parse(String.format(template, "month"));

        AGG_GROUP_MINUTE = Aggregates.group(
                groupIdMinute,
                AccessLogStatisticsHelper.accumulatorForRespCode(1),
                AccessLogStatisticsHelper.accumulatorForRespCode(2),
                AccessLogStatisticsHelper.accumulatorForRespCode(3),
                AccessLogStatisticsHelper.accumulatorForRespCode(4),
                AccessLogStatisticsHelper.accumulatorForRespCode(5),
                Accumulators.sum("millisCost", "$millisCost"),
                Accumulators.sum("upFlow", "$requestInfo.bodySize"),
                Accumulators.sum("downFlow", "$responseInfo.bodySize")
        );

        BsonField count1xx = Accumulators.sum("count1xx", "$count1xx");
        BsonField count2xx = Accumulators.sum("count2xx", "$count2xx");
        BsonField count3xx = Accumulators.sum("count3xx", "$count3xx");
        BsonField count4xx = Accumulators.sum("count4xx", "$count4xx");
        BsonField count5xx = Accumulators.sum("count5xx", "$count5xx");

        BsonField millisCostSum = Accumulators.sum("millisCost", "$millisCost");
        BsonField upFlowSum = Accumulators.sum("upFlow", "$upFlow");
        BsonField downFlowSum = Accumulators.sum("downFlow", "$downFlow");

        AGG_GROUP_HOUR = Aggregates.group(groupIdHour, count1xx, count2xx, count3xx, count4xx, count5xx, millisCostSum, upFlowSum, downFlowSum);
        AGG_GROUP_DAY = Aggregates.group(groupIdDay, count1xx, count2xx, count3xx, count4xx, count5xx, millisCostSum, upFlowSum, downFlowSum);
        AGG_GROUP_MONTH = Aggregates.group(groupIdMonth, count1xx, count2xx, count3xx, count4xx, count5xx, millisCostSum, upFlowSum, downFlowSum);

        List<Bson> matchedPipeline = List.of(
                Aggregates.set(
                        new Field<>("count1xx", Document.parse("{\"$add\": [\"$count1xx\", \"$$new.count1xx\"]}")),
                        new Field<>("count2xx", Document.parse("{\"$add\": [\"$count2xx\", \"$$new.count2xx\"]}")),
                        new Field<>("count3xx", Document.parse("{\"$add\": [\"$count3xx\", \"$$new.count3xx\"]}")),
                        new Field<>("count4xx", Document.parse("{\"$add\": [\"$count4xx\", \"$$new.count4xx\"]}")),
                        new Field<>("count5xx", Document.parse("{\"$add\": [\"$count5xx\", \"$$new.count5xx\"]}")),
                        new Field<>("millisCost", Document.parse("{\"$add\": [\"$millisCost\", \"$$new.millisCost\"]}")),
                        new Field<>("upFlow", Document.parse("{\"$add\": [\"$upFlow\", \"$$new.upFlow\"]}")),
                        new Field<>("downFlow", Document.parse("{\"$add\": [\"$downFlow\", \"$$new.downFlow\"]}"))
                )
        );
        MERGE_OPTIONS_FOR_TOTAL = new MergeOptions().whenMatched(MergeOptions.WhenMatched.PIPELINE).whenMatchedPipeline(matchedPipeline);
    }

    private AccessLogStatisticsHelper() {
    }

    public static String collectionNameForEnvAndArchiveLevel(String envId, AccessLogStatisticsArchiveLevel level) {
        return BASE_COLLECTION_NAME + "_" + level.name() + "_" + envId;
    }

    public static MongoCollection<AccessLogStatistics> getAccessLogStatisticsCollection(
            MongoDatabase mongoDatabase,
            String envId,
            AccessLogStatisticsArchiveLevel level
    ) {
        String collectionName = collectionNameForEnvAndArchiveLevel(envId, level);
        return mongoDatabase.getCollection(collectionName, AccessLogStatistics.class);
    }

    private static BsonField accumulatorForRespCode(int bigCode) {
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
