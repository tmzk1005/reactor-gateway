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
package zk.rgw.dashboard.framework.mongodb;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import lombok.Getter;

@Getter
public class MongodbContext {

    private final String connectionString;

    private final MongoClientSettings mongoClientSettings;

    private final MongoClient mongoClient;

    public MongodbContext(String connectionString) {
        this.connectionString = connectionString;
        ConnectionString cs = new ConnectionString(connectionString);
        this.mongoClientSettings = MongoClientSettings.builder().applyConnectionString(cs).build();
        this.mongoClient = MongoClients.create(mongoClientSettings);
    }

}
