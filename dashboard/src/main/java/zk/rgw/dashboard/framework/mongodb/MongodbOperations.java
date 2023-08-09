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
package zk.rgw.dashboard.framework.mongodb;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.reactivestreams.client.MongoCollection;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import zk.rgw.dashboard.framework.xo.BaseAuditableEntity;
import zk.rgw.dashboard.web.bean.Page;

public class MongodbOperations {

    private MongodbOperations() {
    }

    public static <T extends BaseAuditableEntity<?>> Mono<T> findOne(MongoCollection<T> collection, Bson filter) {
        return Mono.from(collection.find(filter));
    }

    public static <T extends BaseAuditableEntity<?>> Flux<T> find(MongoCollection<T> collection, Bson filter) {
        return Flux.from(collection.find(filter));
    }

    public static <T extends BaseAuditableEntity<?>> Flux<T> find(MongoCollection<T> collection, Bson filter, Bson sorts, Page page) {
        return find(collection, filter, sorts, page, null);
    }

    public static <T extends BaseAuditableEntity<?>> Flux<T> find(
            MongoCollection<T> collection, Bson filter, Bson sorts, Page page, List<Bson> lookupAndProjection
    ) {
        List<Bson> aggPipelines = new ArrayList<>(4);
        if (Objects.nonNull(filter)) {
            aggPipelines.add(Aggregates.match(filter));
        }
        if (Objects.nonNull(sorts)) {
            aggPipelines.add(Aggregates.sort(sorts));
        }
        if (Objects.nonNull(page)) {
            aggPipelines.add(Aggregates.skip(page.getOffset()));
            aggPipelines.add(Aggregates.limit(page.getPageSize()));
        }
        if (Objects.nonNull(lookupAndProjection) && !lookupAndProjection.isEmpty()) {
            aggPipelines.addAll(lookupAndProjection);
        }

        if (aggPipelines.isEmpty()) {
            // just find, no aggregate
            return find(collection, Filters.empty());
        } else {
            return Flux.from(collection.aggregate(aggPipelines));
        }
    }

    public static <T extends BaseAuditableEntity<?>> Mono<T> insert(MongoCollection<T> collection, T entity) {
        if (Objects.nonNull(entity.getId())) {
            throw new IllegalArgumentException("Entity to insert to mongodb should not has an id already");
        }
        return Mono.from(collection.insertOne(entity))
                .flatMap(insertOneResult -> findOne(collection, Filters.eq("_id", insertOneResult.getInsertedId())));
    }

    public static <T extends BaseAuditableEntity<?>> Mono<T> save(MongoCollection<T> collection, T entity) {
        if (Objects.isNull(entity.getId())) {
            throw new IllegalArgumentException("Entity to save(update) to mongodb should has an id.");
        }
        Bson filter = Filters.eq("_id", new ObjectId(entity.getId()));
        return Mono.from(collection.replaceOne(filter, entity))
                .flatMap(updateResult -> findOne(collection, filter));
    }

    public static <T extends BaseAuditableEntity<?>> Mono<Long> count(MongoCollection<T> collection, Bson filter) {
        return Mono.from(collection.countDocuments(filter));
    }

    public static <T extends BaseAuditableEntity<?>> Mono<Void> delete(MongoCollection<T> collection, Bson filter) {
        return Mono.from(collection.deleteMany(filter)).then();
    }

}
