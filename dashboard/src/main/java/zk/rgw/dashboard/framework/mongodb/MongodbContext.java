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

import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.TimeSeriesGranularity;
import com.mongodb.client.model.TimeSeriesOptions;
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
import org.bson.conversions.Bson;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import zk.rgw.common.exception.RgwRuntimeException;
import zk.rgw.dashboard.framework.security.Role;
import zk.rgw.dashboard.framework.security.hash.Pbkdf2PasswordEncoder;
import zk.rgw.dashboard.web.bean.dto.ApiPluginDto;
import zk.rgw.dashboard.web.bean.entity.Api;
import zk.rgw.dashboard.web.bean.entity.ApiPlugin;
import zk.rgw.dashboard.web.bean.entity.ApiSubscribe;
import zk.rgw.dashboard.web.bean.entity.ApiSubscription;
import zk.rgw.dashboard.web.bean.entity.App;
import zk.rgw.dashboard.web.bean.entity.ArchiveProgress;
import zk.rgw.dashboard.web.bean.entity.EnvBinding;
import zk.rgw.dashboard.web.bean.entity.Environment;
import zk.rgw.dashboard.web.bean.entity.GatewayNode;
import zk.rgw.dashboard.web.bean.entity.GatewayNodeMetrics;
import zk.rgw.dashboard.web.bean.entity.MongoLockDocument;
import zk.rgw.dashboard.web.bean.entity.Organization;
import zk.rgw.dashboard.web.bean.entity.RgwSequence;
import zk.rgw.dashboard.web.bean.entity.User;
import zk.rgw.dashboard.web.repository.BaseMongodbRepository;
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
                .then(initCollectionForEntity(Api.class))
                .then(initCollectionForEntity(App.class))
                .then(initCollectionForEntity(ApiSubscribe.class))
                .then(initCollectionForEntity(ApiSubscription.class))
                .then(initCollectionForEntity(GatewayNode.class))
                .then(initCollectionForEntity(ApiPlugin.class))
                .then(initCollectionForEntity(RgwSequence.class))
                .then(initCollectionForEntity(GatewayNodeMetrics.class, true, this::createGatewayNodeMetricsTimeSeriesIfNotExist))
                .then(initCollectionForEntity(MongoLockDocument.class))
                .then(initCollectionForEntity(ArchiveProgress.class))
                .subscribe();

        RepositoryFactory.init(this.mongoClient, this.database);

        Mono<User> adminUserMono = initBuiltinUsersAndReturnAdmin();

        adminUserMono.flatMap(adminUser -> createBuiltinEnvironments(adminUser).then(createBuiltinApiPlugins(adminUser))).subscribe();
    }

    private Mono<User> initBuiltinUsersAndReturnAdmin() {
        BaseMongodbRepository<User> userRepository = new BaseMongodbRepository<>(mongoClient, database, User.class);
        BaseMongodbRepository<Organization> organizationRepository = new BaseMongodbRepository<>(mongoClient, database, Organization.class);
        Mono<User> adminUserMono = initTheFirstAdminUserAndItsOrg(userRepository, organizationRepository);
        return adminUserMono.flatMap(user -> initNormalUsers(user, userRepository).thenReturn(user));
    }

    private Mono<Void> initCollectionForEntity(Class<?> entityClass) {
        return initCollectionForEntity(entityClass, false, null);
    }

    private Mono<Void> initCollectionForEntity(Class<?> entityClass, boolean isTimeSeries, Supplier<Mono<MongoCollection<Document>>> collectionSupplier) {
        String collectionName = MongodbUtil.getCollectionName(entityClass);
        Mono<MongoCollection<Document>> collectionMono = isTimeSeries ? collectionSupplier.get() : Mono.just(database.getCollection(collectionName));
        return collectionMono.flatMap(collection -> createIndexForCollection(collection, entityClass.getAnnotation(Index.class)));
    }

    private Mono<Void> createIndexForCollection(MongoCollection<Document> collection, Index annotation) {
        if (Objects.nonNull(annotation)) {
            return Flux.from(collection.listIndexes()).filter(document -> document.get("name").equals(annotation.name())).count().flatMap(count -> {
                if (count == 0) {
                    log.info(
                            "Create collection {} in mongodb database {}, and create index named {}",
                            collection.getNamespace().getCollectionName(), databaseName, annotation.name()
                    );
                    return createIndex(collection, annotation.name(), annotation.unique(), annotation.def(), annotation.expireSeconds());
                }
                return Mono.empty();
            });
        } else {
            return Mono.empty();
        }
    }

    private Mono<MongoCollection<Document>> createGatewayNodeMetricsTimeSeriesIfNotExist() {
        String collectionName = MongodbUtil.getCollectionName(GatewayNodeMetrics.class);
        return Flux.from(database.listCollectionNames()).hasElement(collectionName).flatMap(alreadyExist -> {
            if (Boolean.TRUE.equals(alreadyExist)) {
                return Mono.just(database.getCollection(collectionName));
            } else {
                TimeSeriesOptions timeSeriesOptions = new TimeSeriesOptions(GatewayNodeMetrics.TIME_FIELD)
                        .metaField(GatewayNodeMetrics.META_FILED)
                        .granularity(TimeSeriesGranularity.SECONDS);
                CreateCollectionOptions createCollectionOptions = new CreateCollectionOptions()
                        .timeSeriesOptions(timeSeriesOptions)
                        .expireAfter(2, TimeUnit.DAYS);
                return Mono.from(database.createCollection(collectionName, createCollectionOptions)).thenReturn(database.getCollection(collectionName));
            }
        });
    }

    private static Mono<Void> createIndex(MongoCollection<Document> collection, String name, boolean unique, String definition, long expireSeconds) {
        IndexOptions indexOptions = new IndexOptions().name(name).unique(unique);
        if (expireSeconds > 0) {
            indexOptions.expireAfter(expireSeconds, TimeUnit.SECONDS);
        }
        return Mono.from(collection.createIndex(Document.parse(definition), indexOptions)).then();
    }

    private Mono<User> initTheFirstAdminUserAndItsOrg(
            BaseMongodbRepository<User> userRepository,
            BaseMongodbRepository<Organization> organizationRepository
    ) {
        return userRepository.findOne(Filters.eq("username", "admin"))
                .switchIfEmpty(createAdminUser(userRepository, organizationRepository));
    }

    private Mono<User> createAdminUser(BaseMongodbRepository<User> userRepository, BaseMongodbRepository<Organization> organizationRepository) {
        Organization tmpOrg = new Organization();
        tmpOrg.setName("系统管理组");
        User tmpUser = new User();
        tmpUser.setUsername("admin");

        return Mono.zip(
                organizationRepository.insert(tmpOrg),
                userRepository.insert(tmpUser)
        ).flatMap(tuple2 -> {
            Organization organization = tuple2.getT1();
            User user = tuple2.getT2();

            Instant now = Instant.now();

            organization.setCreatedBy(user);
            organization.setLastModifiedBy(user);
            organization.setCreatedDate(now);
            organization.setLastModifiedDate(now);

            user.setCreatedBy(user);
            user.setLastModifiedBy(user);
            user.setCreatedDate(now);
            user.setLastModifiedDate(now);

            user.setOrganization(organization);

            user.setNickname("系统管理员");
            user.setRole(Role.SYSTEM_ADMIN);
            user.setPassword(Pbkdf2PasswordEncoder.getDefaultInstance().encode("admin@rgw"));
            user.setEnabled(true);
            user.setDeleted(false);
            return organizationRepository.save(organization)
                    .doOnNext(org -> log.info("Create a organization named {}", org.getName()))
                    .then(userRepository.save(user))
                    .doOnNext(adminUser -> log.info("Create system administrator named {}", adminUser.getUsername()))
                    .switchIfEmpty(Mono.error(new RuntimeException("初始化系统管理员失败")))
                    .doOnError(throwable -> {
                        log.error("系统初始化失败", throwable);
                        System.exit(-1);
                    });
        });
    }

    private Mono<Void> initNormalUsers(User adminUser, BaseMongodbRepository<User> userRepository) {
        return initNormalUser(adminUser, userRepository, "rgw1", "Rgw1", "rgw1@rgw", Role.ORGANIZATION_ADMIN)
                .then(initNormalUser(adminUser, userRepository, "rgw2", "Rgw2", "rgw2@rgw", Role.NORMAL_USER));
    }

    private Mono<Void> initNormalUser(
            User adminUser, BaseMongodbRepository<User> userRepository,
            String username, String nickname, String password, Role role
    ) {
        return userRepository.findOne(Filters.eq("username", username))
                .switchIfEmpty(Mono.defer(() -> {
                    User user = new User();
                    user.setUsername(username);
                    user.setNickname(nickname);
                    user.setPassword(Pbkdf2PasswordEncoder.getDefaultInstance().encode(password));
                    user.setRole(role);
                    user.setOrganization(adminUser.getOrganization());

                    user.setEnabled(true);
                    user.setDeleted(false);

                    user.setCreatedBy(adminUser);
                    user.setLastModifiedBy(adminUser);
                    Instant now = Instant.now();
                    user.setCreatedDate(now);
                    user.setLastModifiedDate(now);

                    return userRepository.insert(user)
                            .doOnNext(newUser -> log.info("Create a user named {}, generated id is {}", newUser.getUsername(), newUser.getId()));
                })).then();
    }

    private Mono<Void> createBuiltinEnvironments(User adminUser) {
        BaseMongodbRepository<Environment> repository = new BaseMongodbRepository<>(mongoClient, database, Environment.class);
        return createEnvironment(repository, adminUser, "开发环境")
                .then(createEnvironment(repository, adminUser, "测试环境"))
                .then(createEnvironment(repository, adminUser, "生产环境"));
    }

    private Mono<Void> createEnvironment(BaseMongodbRepository<Environment> repository, User adminUser, String envName) {
        return repository.findOne(Filters.eq("name", envName)).switchIfEmpty(Mono.defer(() -> {
            Environment environment = new Environment();
            environment.setName(envName);

            environment.setCreatedBy(adminUser);
            environment.setLastModifiedBy(adminUser);
            Instant now = Instant.now();
            environment.setCreatedDate(now);
            environment.setLastModifiedDate(now);

            log.info("Create an environment named {}", envName);
            return repository.insert(environment);
        })).then();
    }

    private Mono<Void> createBuiltinApiPlugins(User adminUser) {
        String jsonFileName = "rgw-builtin-plugins.json";
        ObjectMapper objectMapper = new ObjectMapper();
        ApiPluginDto[] apiPluginDtoArr;
        try (InputStream inputStream = ClassLoader.getSystemResourceAsStream(jsonFileName)) {
            apiPluginDtoArr = objectMapper.readValue(inputStream, ApiPluginDto[].class);
        } catch (Exception exception) {
            return Mono.error(new RgwRuntimeException("Failed to read builtin plugins from json file " + jsonFileName));
        }
        if (Objects.isNull(apiPluginDtoArr)) {
            return Mono.empty();
        }
        BaseMongodbRepository<ApiPlugin> repository = new BaseMongodbRepository<>(mongoClient, database, ApiPlugin.class);
        return Flux.fromArray(apiPluginDtoArr).flatMap(apiPluginDto -> {
            ApiPlugin apiPlugin = new ApiPlugin().initFromDto(apiPluginDto);

            apiPlugin.setBuiltin(true);
            apiPlugin.setOrganizationId(null);
            apiPlugin.setInstallerUri(null);

            apiPlugin.setCreatedBy(adminUser);
            apiPlugin.setLastModifiedBy(adminUser);
            Instant now = Instant.now();
            apiPlugin.setCreatedDate(now);
            apiPlugin.setLastModifiedDate(now);

            return saveApiPlugin(repository, apiPlugin);
        }).then();
    }

    private static Mono<Void> saveApiPlugin(BaseMongodbRepository<ApiPlugin> repository, ApiPlugin apiPlugin) {
        Bson checkFilter = Filters.and(
                Filters.eq("name", apiPlugin.getName()),
                Filters.eq("version", apiPlugin.getVersion())
        );
        return repository.exists(checkFilter).flatMap(exist -> {
            if (Boolean.TRUE.equals(exist)) {
                return Mono.empty();
            } else {
                return repository.insert(apiPlugin).doOnNext(pl -> {
                    log.info("save a builtin plugin, name = {}, version={}", pl.getName(), pl.getVersion());
                }).then();
            }
        });
    }

}
