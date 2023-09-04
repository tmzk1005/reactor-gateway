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

import com.mongodb.client.result.InsertOneResult;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import reactor.core.publisher.Mono;

import zk.rgw.dashboard.framework.mongodb.MongodbUtil;
import zk.rgw.dashboard.web.bean.entity.ArchiveProgress;

public class ArchiveProgressRepository {

    private final MongoCollection<ArchiveProgress> collection;

    public ArchiveProgressRepository(MongoDatabase database) {
        String collectionName = MongodbUtil.getCollectionName(ArchiveProgress.class);
        this.collection = database.getCollection(collectionName, ArchiveProgress.class);
    }

    public Mono<Boolean> save(String envId, Instant instant) {
        if (instant.toEpochMilli() % 60_000L != 0) {
            return Mono.just(false);
        }
        ArchiveProgress archiveProgress = new ArchiveProgress(envId, instant);
        return Mono.from(collection.insertOne(archiveProgress))
                .map(InsertOneResult::wasAcknowledged)
                .onErrorResume(throwable -> Mono.just(Boolean.FALSE));
    }

}
