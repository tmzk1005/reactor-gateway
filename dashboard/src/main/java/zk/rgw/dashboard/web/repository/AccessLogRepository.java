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

import java.util.ArrayList;
import java.util.List;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.bson.conversions.Bson;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import zk.rgw.alc.common.AccessLogDocument;
import zk.rgw.alc.common.AccessLogDocumentUtil;
import zk.rgw.dashboard.web.bean.Page;
import zk.rgw.dashboard.web.bean.PageData;

public class AccessLogRepository {

    private final MongoDatabase database;

    public AccessLogRepository(MongoDatabase mongoDatabase) {
        this.database = mongoDatabase;
    }

    public Mono<PageData<AccessLogDocument>> searchAccessLogs(String envId, Page page) {
        return AccessLogDocumentUtil.getAccessLogCollectionForEnv(database, envId)
                .flatMap(collection -> searchAccessLogs(collection, page));
    }

    private Mono<PageData<AccessLogDocument>> searchAccessLogs(MongoCollection<AccessLogDocument> collection, Page page) {
        Bson filter = Filters.empty();
        Bson sorts = Sorts.descending("timestampMillis");

        List<Bson> aggPipelines = new ArrayList<>(4);
        aggPipelines.add(Aggregates.match(filter));
        aggPipelines.add(Aggregates.sort(sorts));
        aggPipelines.add(Aggregates.skip(page.getOffset()));
        aggPipelines.add(Aggregates.limit(page.getPageSize()));

        Mono<List<AccessLogDocument>> dataMono = Flux.from(collection.aggregate(aggPipelines)).collectList();
        Mono<Long> countMono = Mono.from(collection.countDocuments(filter));

        return Mono.zip(dataMono, countMono)
                .map(tuple -> new PageData<>(tuple.getT1(), tuple.getT2(), page.getPageNum(), page.getPageSize()));
    }

}
