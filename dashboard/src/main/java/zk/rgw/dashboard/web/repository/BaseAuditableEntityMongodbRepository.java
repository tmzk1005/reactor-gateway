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
import java.util.Objects;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import zk.rgw.dashboard.framework.context.ContextUtil;
import zk.rgw.dashboard.framework.mongodb.MongodbOperations;
import zk.rgw.dashboard.framework.xo.BaseAuditableEntity;

@Slf4j
public class BaseAuditableEntityMongodbRepository<E extends BaseAuditableEntity<?>> extends BaseMongodbRepository<E> {

    public BaseAuditableEntityMongodbRepository(MongoClient mongoClient, MongoDatabase database, Class<E> entityClass) {
        super(mongoClient, database, entityClass);
    }

    public Mono<E> insert(E entity) {
        Instant now = Instant.now();
        entity.setCreatedDate(now);
        entity.setLastModifiedDate(now);
        return ContextUtil.getUser().flatMap(user -> {
            entity.setCreatedBy(user);
            entity.setLastModifiedBy(user);
            return MongodbOperations.insert(mongoCollection, entity);
        }).switchIfEmpty(MongodbOperations.insert(mongoCollection, entity));
    }

    public Mono<E> save(E entity) {
        if (Objects.isNull(entity.getId())) {
            return insert(entity);
        }
        Instant now = Instant.now();
        entity.setLastModifiedDate(now);
        return ContextUtil.getUser().flatMap(user -> {
            entity.setLastModifiedBy(user);
            return MongodbOperations.save(mongoCollection, entity);
        }).switchIfEmpty(MongodbOperations.save(mongoCollection, entity));
    }

}
