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

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoCollection;

import zk.rgw.dashboard.framework.xo.Auditable;

public class AbstractMongodbRepository<E extends Auditable> {

    protected final MongoClient mongoClient;

    protected final String databaseName;

    protected final Class<E> entityClass;

    protected final MongoCollection<E> mongoCollection;

    protected AbstractMongodbRepository(MongoClient mongoClient, String databaseName, Class<E> entityClass) {
        this.mongoClient = mongoClient;
        this.databaseName = databaseName;
        this.entityClass = entityClass;
        this.mongoCollection = null;
    }

}
