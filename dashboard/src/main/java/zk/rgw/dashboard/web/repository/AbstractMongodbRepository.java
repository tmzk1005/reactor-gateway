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
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import org.bson.conversions.Bson;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import zk.rgw.dashboard.framework.context.ContextUtil;
import zk.rgw.dashboard.framework.exception.AccessDeniedException;
import zk.rgw.dashboard.framework.exception.NotObjectIdException;
import zk.rgw.dashboard.framework.mongodb.MongodbOperations;
import zk.rgw.dashboard.framework.mongodb.MongodbUtil;
import zk.rgw.dashboard.framework.xo.BaseAuditableEntity;

@Slf4j
public class AbstractMongodbRepository<E extends BaseAuditableEntity<?>> {

    protected final MongoClient mongoClient;

    protected final MongoDatabase database;

    protected final Class<E> entityClass;

    protected final MongoCollection<E> mongoCollection;

    protected AbstractMongodbRepository(MongoClient mongoClient, MongoDatabase database, Class<E> entityClass) {
        this.mongoClient = mongoClient;
        this.database = database;
        this.entityClass = entityClass;
        this.mongoCollection = this.database.getCollection(MongodbUtil.getCollectionName(entityClass), entityClass);
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

    public Mono<E> saveMine(E entity) {
        Instant now = Instant.now();
        entity.setLastModifiedDate(now);
        return ContextUtil.getUser().flatMap(user -> {
            String createdById = entity.getCreatedBy().getId();
            String curUserId = user.getId();
            if (!createdById.equals(curUserId)) {
                throw new AccessDeniedException("没有权限操作id为" + entity.getId() + "的对象");
            }
            entity.setLastModifiedBy(user);
            return MongodbOperations.save(mongoCollection, entity);
        }).switchIfEmpty(MongodbOperations.save(mongoCollection, entity));
    }

    public Mono<E> findOne(Bson filter) {
        return MongodbOperations.findOne(mongoCollection, filter);
    }

    public Mono<Boolean> exists(Bson filter) {
        return MongodbOperations.count(mongoCollection, filter).map(num -> num > 0);
    }

    public Flux<E> find(Bson filter) {
        return MongodbOperations.find(mongoCollection, filter);
    }

    public Mono<E> findOneById(String id) {
        Bson filter;
        try {
            filter = MongodbUtil.createFilterById(id);
        } catch (NotObjectIdException exception) {
            return Mono.empty();
        }
        return findOne(filter);
    }

    public <T> Mono<T> doInTransaction(Mono<T> mono) {
        return Mono.from(mongoClient.startSession()).flatMap(session -> {
            session.startTransaction();
            return mono.doOnError(ignore -> session.abortTransaction())
                    .flatMap(data -> Mono.from(session.commitTransaction()).thenReturn(data));
        });
    }

}
