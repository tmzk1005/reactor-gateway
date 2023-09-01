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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mongodb.client.model.Filters;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import zk.rgw.dashboard.web.bean.entity.EnvBinding;

public class EnvBindingRepository extends BaseAuditableEntityMongodbRepository<EnvBinding> {

    public EnvBindingRepository(MongoClient mongoClient, MongoDatabase database) {
        super(mongoClient, database, EnvBinding.class);
    }

    public Flux<EnvBinding> findByEnvAndOrgIds(String envId, List<String> orgIds) {
        Bson filter = Filters.and(
                Filters.eq("environment", new ObjectId(envId)),
                Filters.in("organization", orgIds.stream().map(ObjectId::new).toList())
        );
        return find(filter);
    }

    public Mono<Map<String, Map<String, String>>> findByEnvAndOrgIdsAsMap(String envId, List<String> orgIds) {
        return findByEnvAndOrgIds(envId, orgIds).collectList().map(list -> {
            Map<String, Map<String, String>> map = new HashMap<>(list.size() * 2);
            for (EnvBinding envBinding : list) {
                map.put(envBinding.getOrganization().getId(), envBinding.getVariables());
            }
            return map;
        });
    }

}
