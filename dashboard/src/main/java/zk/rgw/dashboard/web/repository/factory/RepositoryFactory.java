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
package zk.rgw.dashboard.web.repository.factory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoDatabase;

import zk.rgw.dashboard.web.repository.ApiPluginRepository;
import zk.rgw.dashboard.web.repository.ApiRepository;
import zk.rgw.dashboard.web.repository.ApiSubscribeRepository;
import zk.rgw.dashboard.web.repository.ApiSubscriptionRepository;
import zk.rgw.dashboard.web.repository.AppRepository;
import zk.rgw.dashboard.web.repository.EnvBindingRepository;
import zk.rgw.dashboard.web.repository.EnvironmentRepository;
import zk.rgw.dashboard.web.repository.GatewayNodeRepository;
import zk.rgw.dashboard.web.repository.OrganizationRepository;
import zk.rgw.dashboard.web.repository.RgwSequenceRepository;
import zk.rgw.dashboard.web.repository.UserRepository;

public class RepositoryFactory {

    private static final Map<Class<?>, Object> REPOSITORY_MAP = new HashMap<>();

    private RepositoryFactory() {
    }

    public static void init(MongoClient mongoClient, MongoDatabase mongoDatabase) {
        Objects.requireNonNull(mongoClient);
        Objects.requireNonNull(mongoDatabase);
        REPOSITORY_MAP.put(UserRepository.class, new UserRepository(mongoClient, mongoDatabase));
        REPOSITORY_MAP.put(OrganizationRepository.class, new OrganizationRepository(mongoClient, mongoDatabase));
        REPOSITORY_MAP.put(EnvironmentRepository.class, new EnvironmentRepository(mongoClient, mongoDatabase));
        REPOSITORY_MAP.put(EnvBindingRepository.class, new EnvBindingRepository(mongoClient, mongoDatabase));
        REPOSITORY_MAP.put(ApiRepository.class, new ApiRepository(mongoClient, mongoDatabase));
        REPOSITORY_MAP.put(AppRepository.class, new AppRepository(mongoClient, mongoDatabase));
        REPOSITORY_MAP.put(ApiSubscribeRepository.class, new ApiSubscribeRepository(mongoClient, mongoDatabase));
        REPOSITORY_MAP.put(ApiSubscriptionRepository.class, new ApiSubscriptionRepository(mongoClient, mongoDatabase));
        REPOSITORY_MAP.put(GatewayNodeRepository.class, new GatewayNodeRepository(mongoClient, mongoDatabase));
        REPOSITORY_MAP.put(ApiPluginRepository.class, new ApiPluginRepository(mongoClient, mongoDatabase));
        REPOSITORY_MAP.put(RgwSequenceRepository.class, new RgwSequenceRepository(mongoDatabase));
    }

    @SuppressWarnings("unchecked")
    public static <R> R get(Class<R> repositoryClass) {
        return (R) REPOSITORY_MAP.get(repositoryClass);
    }

}
