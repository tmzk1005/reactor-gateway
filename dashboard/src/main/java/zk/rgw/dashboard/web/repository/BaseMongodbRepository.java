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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.mongodb.client.model.Filters;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import zk.rgw.dashboard.framework.exception.NotObjectIdException;
import zk.rgw.dashboard.framework.mongodb.MongodbOperations;
import zk.rgw.dashboard.framework.mongodb.MongodbUtil;
import zk.rgw.dashboard.framework.xo.Po;
import zk.rgw.dashboard.web.bean.Page;
import zk.rgw.dashboard.web.bean.PageData;

public class BaseMongodbRepository<E extends Po<?>> {

    protected final MongoClient mongoClient;

    protected final MongoDatabase database;

    protected final Class<E> entityClass;

    protected final MongoCollection<E> mongoCollection;

    public BaseMongodbRepository(MongoClient mongoClient, MongoDatabase database, Class<E> entityClass) {
        this.mongoClient = mongoClient;
        this.database = database;
        this.entityClass = entityClass;
        this.mongoCollection = this.database.getCollection(MongodbUtil.getCollectionName(entityClass), entityClass);
    }

    public Mono<E> insert(E entity) {
        return MongodbOperations.insert(mongoCollection, entity);
    }

    public Mono<E> save(E entity) {
        if (Objects.isNull(entity.getId())) {
            return insert(entity);
        }
        return MongodbOperations.save(mongoCollection, entity);
    }

    public Mono<E> findOne(Bson filter) {
        return MongodbOperations.findOne(mongoCollection, filter);
    }

    public Mono<E> findOne(Bson filter, List<Bson> lookupAndProjection) {
        return MongodbOperations.findOne(mongoCollection, filter, lookupAndProjection);
    }

    public Mono<Boolean> exists(Bson filter) {
        return MongodbOperations.count(mongoCollection, filter).map(num -> num > 0);
    }

    public Mono<Boolean> existsById(String id) {
        Bson filter;
        try {
            filter = MongodbUtil.createFilterById(id);
        } catch (NotObjectIdException exception) {
            return Mono.just(false);
        }
        return exists(filter);
    }

    public Flux<E> find(Bson filter) {
        return MongodbOperations.find(mongoCollection, filter);
    }

    public Flux<E> findAll() {
        return find(Filters.empty());
    }

    public Mono<PageData<E>> find(Bson filter, Bson sorts, Page page) {
        return find(filter, sorts, page, null);
    }

    public Mono<PageData<E>> find(Bson filter, Bson sorts, Page page, List<Bson> lookupAndProjection) {
        Mono<List<E>> dataMono = MongodbOperations.find(mongoCollection, filter, sorts, page, lookupAndProjection).collectList();
        Mono<Long> countMono = MongodbOperations.count(mongoCollection, filter);
        return Mono.zip(dataMono, countMono)
                .map(tuple -> new PageData<>(tuple.getT1(), tuple.getT2(), page.getPageNum(), page.getPageSize()));
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

    public Mono<E> findOneById(String id, List<Bson> lookupAndProjection) {
        Bson filter;
        try {
            filter = MongodbUtil.createFilterById(id);
        } catch (NotObjectIdException exception) {
            return Mono.empty();
        }
        return findOne(filter, lookupAndProjection);
    }

    public Mono<List<E>> getListByIdIn(Collection<String> ids) {
        Bson filter = Filters.in("_id", ids.stream().map(ObjectId::new).toList());
        return find(filter).collectList();
    }

    public Mono<Map<String, E>> getMapByIdIn(Collection<String> ids) {
        return getListByIdIn(ids).map(list -> {
            Map<String, E> map = new HashMap<>(2 * ids.size());
            for (E item : list) {
                map.put(item.getId(), item);
            }
            return map;
        });
    }

    public <T> Mono<T> doInTransaction(Mono<T> mono) {
        return Mono.from(mongoClient.startSession()).flatMap(session -> {
            session.startTransaction();
            return mono.doOnError(ignore -> session.abortTransaction())
                    .flatMap(data -> Mono.from(session.commitTransaction()).thenReturn(data));
        });
    }

}
