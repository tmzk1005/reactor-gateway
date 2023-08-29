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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoDatabase;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonType;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonRepresentation;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import zk.rgw.common.util.ObjectUtil;
import zk.rgw.dashboard.framework.context.ContextUtil;
import zk.rgw.dashboard.framework.mongodb.MongodbOperations;
import zk.rgw.dashboard.framework.xo.BaseAuditableEntity;

@Slf4j
public class BaseAuditableEntityMongodbRepository<E extends BaseAuditableEntity<?>> extends BaseMongodbRepository<E> {

    public BaseAuditableEntityMongodbRepository(MongoClient mongoClient, MongoDatabase database, Class<E> entityClass) {
        super(mongoClient, database, entityClass);
    }

    @Override
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

    @Override
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

    /**
     * 根据name字段进行模糊查询过滤，同时必须属于当前用户，返回匹配文档的id
     * 只有文档含有 _id, name, organization这3个字段时才可以调用
     */
    public Flux<String> findIdsBelongsCurrentUserFilterByName(String name, boolean filterByOrg) {
        return ContextUtil.getUser().flatMapMany(user -> {
            List<Bson> aggPipelines = new ArrayList<>(2);

            Bson filer = ObjectUtil.isEmpty(name) ? Filters.empty() : Filters.regex("name", name, "i");
            if (!user.isSystemAdmin() && filterByOrg) {
                Bson orgFilter = Filters.eq("organization", new ObjectId(user.getOrganization().getId()));
                filer = ObjectUtil.isEmpty(name) ? orgFilter : Filters.and(filer, orgFilter);
            }
            aggPipelines.add(Aggregates.match(filer));

            Bson project = Aggregates.project(Projections.fields(Projections.include("_id")));
            aggPipelines.add(project);

            return Flux.from(mongoCollection.withDocumentClass(IdHolder.class).aggregate(aggPipelines)).map(IdHolder::getId);
        });
    }

    /**
     * 根据id判断文档是否属于当前用户
     * 只有文档含有 _id, organization这2个字段时才可以调用
     */
    public Mono<Boolean> isIdBelongsCurrentUser(ObjectId id, boolean filterByOrg) {
        return ContextUtil.getUser().flatMap(user -> {
            Bson filter = Filters.eq("_id", id);
            if (!user.isSystemAdmin() && filterByOrg) {
                filter = Filters.and(
                        filter,
                        Filters.eq("organization", new ObjectId(user.getOrganization().getId()))
                );
            }
            return Mono.from(mongoCollection.countDocuments(filter)).map(count -> count > 0);
        });
    }

    @Getter
    @Setter
    public static class IdHolder {

        @BsonId
        @BsonRepresentation(BsonType.OBJECT_ID)
        String id;
    }

}
