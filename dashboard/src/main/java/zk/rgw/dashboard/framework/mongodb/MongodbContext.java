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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.Convention;
import org.bson.codecs.pojo.Conventions;
import org.bson.codecs.pojo.PojoCodecProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import zk.rgw.dashboard.framework.security.Role;
import zk.rgw.dashboard.framework.security.hash.Pbkdf2PasswordEncoder;
import zk.rgw.dashboard.web.bean.entity.EnvBinding;
import zk.rgw.dashboard.web.bean.entity.Environment;
import zk.rgw.dashboard.web.bean.entity.Organization;
import zk.rgw.dashboard.web.bean.entity.User;
import zk.rgw.dashboard.web.repository.EnvironmentRepository;
import zk.rgw.dashboard.web.repository.OrganizationRepository;
import zk.rgw.dashboard.web.repository.UserRepository;
import zk.rgw.dashboard.web.repository.factory.RepositoryFactory;

@Slf4j
@Getter
public class MongodbContext {

    private final String connectionString;

    private final String databaseName;

    private MongoClientSettings mongoClientSettings;

    private MongoClient mongoClient;

    private MongoDatabase database;

    private CodecRegistry codecRegistry;

    public MongodbContext(String connectionString, String databaseName) {
        this.connectionString = connectionString;
        this.databaseName = databaseName;
        init0();
    }

    private void initCodecRegistry() {
        List<Convention> conventions = new ArrayList<>(Conventions.DEFAULT_CONVENTIONS);
        CustomConvention customConvention = new CustomConvention();
        conventions.add(customConvention);
        codecRegistry = CodecRegistries.fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).conventions(conventions).build())
        );
        customConvention.setCodecRegistry(codecRegistry);
    }

    private void init0() {
        initCodecRegistry();
        ConnectionString cs = new ConnectionString(connectionString);
        this.mongoClientSettings = MongoClientSettings.builder().applyConnectionString(cs).codecRegistry(codecRegistry).build();
        this.mongoClient = MongoClients.create(mongoClientSettings);
        this.database = mongoClient.getDatabase(databaseName);
    }

    public void init() {
        initCollectionForEntity(User.class)
                .then(initCollectionForEntity(Organization.class))
                .then(initCollectionForEntity(Environment.class))
                .then(initCollectionForEntity(EnvBinding.class))
                .subscribe();

        RepositoryFactory.init(this.mongoClient, this.database);

        initUser("admin", "Admin", "admin@rgw", Role.SYSTEM_ADMIN)
                .then(initUser("rgw", "Rgw", "rgw@rgw", Role.NORMAL_USER))
                .subscribe();

        createEnvironment("开发环境")
                .then(createEnvironment("测试环境"))
                .then(createEnvironment("预生产环境"))
                .then(createEnvironment("生产环境"))
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
        OrganizationRepository organizationRepository = RepositoryFactory.get(OrganizationRepository.class);
        UserRepository userRepository = RepositoryFactory.get(UserRepository.class);

        final String orgName = "系统管理组";

        return organizationRepository.findOneByName(orgName).switchIfEmpty(Mono.defer(() -> {
            Organization organization = new Organization();
            organization.setName(orgName);
            log.info("Create a organization named {}", orgName);
            return organizationRepository.insert(organization);
        })).flatMap(
                organization -> userRepository.findOneByUsername(username)
                        .switchIfEmpty(Mono.defer(() -> {
                            User user = new User();
                            user.setUsername(username);
                            user.setNickname(nickname);
                            user.setPassword(Pbkdf2PasswordEncoder.getDefaultInstance().encode(password));
                            user.setRole(role);
                            user.setOrganizationId(organization.getId());
                            return userRepository.insert(user).doOnNext(newUser -> {
                                log.info("Create a user named {}, generated id is {}", newUser.getUsername(), newUser.getId());
                            });
                        }))
        ).then();
    }

    private static Mono<Void> createEnvironment(String envName) {
        EnvironmentRepository environmentRepository = RepositoryFactory.get(EnvironmentRepository.class);
        return environmentRepository.findOneByName(envName).switchIfEmpty(Mono.defer(() -> {
            Environment environment = new Environment();
            environment.setName(envName);
            log.info("Create an environment named {}", envName);
            return environmentRepository.insert(environment);
        })).then();
    }

}
