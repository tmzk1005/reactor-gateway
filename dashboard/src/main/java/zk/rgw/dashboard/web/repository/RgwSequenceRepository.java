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

import java.util.Objects;

import com.mongodb.client.model.Filters;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.bson.BsonDocument;
import org.bson.conversions.Bson;
import reactor.core.publisher.Mono;

import zk.rgw.common.exception.RgwRuntimeException;
import zk.rgw.dashboard.framework.mongodb.MongodbUtil;
import zk.rgw.dashboard.web.bean.entity.RgwSequence;

public class RgwSequenceRepository {

    private static final Bson UPDATE = BsonDocument.parse("{\"$inc\":{\"value\":1}}");

    protected final MongoCollection<RgwSequence> mongoCollection;

    public RgwSequenceRepository(MongoDatabase database) {
        this.mongoCollection = database.getCollection(MongodbUtil.getCollectionName(RgwSequence.class), RgwSequence.class);
    }

    private Mono<Long> initForSeqName(String name) {
        return Mono.from(mongoCollection.insertOne(new RgwSequence(name))).flatMap(insertOneResult -> {
            if (insertOneResult.wasAcknowledged()) {
                return next(name);
            } else {
                return Mono.error(new RgwRuntimeException("Failed to init sequence named " + name));
            }
        });
    }

    public Mono<Long> next(String seqName) {
        Objects.requireNonNull(seqName);
        Bson filter = Filters.eq("name", seqName);
        return Mono.from(mongoCollection.findOneAndUpdate(filter, UPDATE))
                .map(RgwSequence::getValue)
                .switchIfEmpty(initForSeqName(seqName));
    }

}
