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

import java.util.Objects;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import zk.rgw.dashboard.framework.security.Role;
import zk.rgw.dashboard.web.bean.entity.Organization;
import zk.rgw.dashboard.web.bean.entity.User;
import zk.rgw.dashboard.web.repository.UserRepository;
import zk.rgw.dashboard.web.repository.factory.RepositoryFactory;

@Slf4j
@Getter
public class MongodbContext {

    private final String connectionString;

    private final MongoClientSettings mongoClientSettings;

    private final MongoClient mongoClient;

    private final String databaseName;

    private final MongoDatabase database;

    public MongodbContext(String connectionString, String databaseName) {
        this.connectionString = connectionString;
        ConnectionString cs = new ConnectionString(connectionString);
        this.mongoClientSettings = MongoClientSettings.builder().applyConnectionString(cs).build();
        this.mongoClient = MongoClients.create(mongoClientSettings);
        this.databaseName = databaseName;
        this.database = mongoClient.getDatabase(databaseName);
    }

    public void init() {
        initCollectionForEntity(User.class)
                .then(initCollectionForEntity(Organization.class))
                .subscribe();
        RepositoryFactory.init(this.mongoClient, this.database);
        initUser("admin", "Admin", "admin@rgw", Role.SYSTEM_ADMIN)
                .then(initUser("rgw", "Rgw", "rgw@rgw", Role.NORMAL_USER))
                .subscribe();
    }

    private Mono<Void> initCollectionForEntity(Class<?> entityClass) {
        String collectionName = MongodbUtil.getCollectionName(entityClass);
        MongoCollection<Document> collection = database.getCollection(collectionName);
        Index annotation = entityClass.getAnnotation(Index.class);
        if (Objects.nonNull(annotation)) {
            return Flux.from(collection.listIndexes()).filter(document -> document.get("name").equals(annotation.name())).count().flatMap(count -> {
                if (count == 0) {
                    log.info("Create collection {} in mongodb database {}, and create index named {}", collectionName, databaseName, annotation.name());
                    return createIndex(collection, annotation.name(), annotation.unique(), annotation.def());
                }
                return Mono.empty();
            });
        }
        return Mono.empty();
    }

    private static Mono<Void> createIndex(MongoCollection<Document> collection, String name, boolean unique, String definition) {
        IndexOptions indexOptions = new IndexOptions().name(name).unique(unique);
        return Mono.from(collection.createIndex(Document.parse(definition), indexOptions)).then();
    }

    private static Mono<Void> initUser(String username, String nickname, String password, Role role) {
        UserRepository userRepository = RepositoryFactory.get(UserRepository.class);
        return userRepository.findOne(Filters.eq("username", username))
                .switchIfEmpty(Mono.defer(() -> {
                    User user = new User();
                    user.setName(username);
                    user.setNickname(nickname);
                    user.setPassword(password);
                    user.setRole(role);
                    return userRepository.insert(user);
                }))
                .then();
    }

}
