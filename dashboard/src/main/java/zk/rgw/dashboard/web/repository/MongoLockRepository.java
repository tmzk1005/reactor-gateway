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

import com.mongodb.client.model.Filters;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import reactor.core.publisher.Mono;

import zk.rgw.dashboard.framework.mongodb.MongodbUtil;
import zk.rgw.dashboard.web.bean.entity.MongoLockDocument;
import zk.rgw.dashboard.web.lock.MongoLock;

public class MongoLockRepository {

    private final MongoCollection<MongoLockDocument> collection;

    public MongoLockRepository(MongoDatabase mongoDatabase) {
        String collectionName = MongodbUtil.getCollectionName(MongoLockDocument.class);
        this.collection = mongoDatabase.getCollection(collectionName, MongoLockDocument.class);
    }

    public MongoLock getLock(String lockName) {
        return new MongoLock() {
            @Override
            public Mono<Boolean> tryLock() {
                return Mono.from(collection.insertOne(new MongoLockDocument(lockName)))
                        .map(InsertOneResult::wasAcknowledged)
                        .onErrorResume(throwable -> Mono.just(Boolean.FALSE));
            }

            @Override
            public Mono<Void> release() {
                return Mono.from(collection.deleteOne(Filters.eq("lockName", lockName))).then();
            }
        };
    }

}
