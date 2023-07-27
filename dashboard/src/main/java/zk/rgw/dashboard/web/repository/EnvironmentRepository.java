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
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoDatabase;
import reactor.core.publisher.Mono;

import zk.rgw.dashboard.web.bean.entity.Environment;

public class EnvironmentRepository extends AbstractMongodbRepository<Environment> {

    public EnvironmentRepository(MongoClient mongoClient, MongoDatabase database) {
        super(mongoClient, database, Environment.class);
    }

    public Mono<Environment> findOneByName(String name) {
        return findOne(Filters.eq("name", name));
    }

    public Mono<Boolean> existOneByName(String name) {
        return exists(Filters.eq("name", name));
    }

}
