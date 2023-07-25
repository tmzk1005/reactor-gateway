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

import zk.rgw.dashboard.web.bean.entity.User;

public class UserRepository extends AbstractMongodbRepository<User> {

    public UserRepository(MongoClient mongoClient, MongoDatabase database, Class<User> entityClass) {
        super(mongoClient, database, entityClass);
    }

    public Mono<User> findOneByUsername(String username) {
        return findOne(Filters.eq("username", username));
    }

}
