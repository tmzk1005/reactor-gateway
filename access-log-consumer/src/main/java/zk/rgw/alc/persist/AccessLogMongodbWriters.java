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

package zk.rgw.alc.persist;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.mongodb.reactivestreams.client.MongoDatabase;

import zk.rgw.common.bootstrap.LifeCycle;
import zk.rgw.common.constant.Constants;

public class AccessLogMongodbWriters implements LifeCycle {

    private final String kafkaBootstrapServers;

    private ExecutorService executorService;

    private final List<String> endIds;

    private final MongoDatabase mongoDatabase;

    private final List<AccessLogMongodbWriter> writers;

    public AccessLogMongodbWriters(String kafkaBootstrapServers, MongoDatabase mongoDatabase, List<String> envIds) {
        this.kafkaBootstrapServers = kafkaBootstrapServers;
        this.mongoDatabase = mongoDatabase;
        this.endIds = envIds;
        this.writers = new ArrayList<>(envIds.size());
    }

    @Override
    public void start() {
        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        for (String envId : endIds) {
            String realEvnId = Constants.BUILT_IN_ENV_IDS.getOrDefault(envId, envId);
            AccessLogMongodbWriter accessLogMongodbWriter = new AccessLogMongodbWriter(kafkaBootstrapServers, mongoDatabase, realEvnId, executorService);
            accessLogMongodbWriter.start();
            writers.add(accessLogMongodbWriter);
        }
    }

    @Override
    public void stop() {
        for (AccessLogMongodbWriter accessLogMongodbWriter : writers) {
            accessLogMongodbWriter.stop();
        }
        if (Objects.nonNull(executorService)) {
            executorService.shutdown();
        }
    }

}
