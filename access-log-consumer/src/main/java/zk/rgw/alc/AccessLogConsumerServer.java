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

package zk.rgw.alc;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import lombok.extern.slf4j.Slf4j;

import zk.rgw.alc.persist.AccessLogMongodbWriters;
import zk.rgw.common.bootstrap.Server;

@Slf4j
public class AccessLogConsumerServer implements Server {

    private final AlcConfiguration configuration;

    private MongoClient mongoClient;

    private AccessLogMongodbWriters accessLogMongodbWriters;

    private final CountDownLatch shutdownWaiter = new CountDownLatch(1);

    public AccessLogConsumerServer(AlcConfiguration alcConfiguration) {
        this.configuration = alcConfiguration;
    }

    @Override
    public void start() {
        initMongoClient();
        accessLogMongodbWriters = new AccessLogMongodbWriters(
                configuration.getKafkaBootstrapServers(),
                mongoClient.getDatabase(configuration.getMongodbDatabaseName()),
                configuration.getEnvironments()
        );
        accessLogMongodbWriters.start();

        log.info("{} started.", this.getClass().getSimpleName());

        try {
            shutdownWaiter.await();
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void stop() {
        accessLogMongodbWriters.stop();

        if (Objects.nonNull(mongoClient)) {
            mongoClient.close();
        }

        shutdownWaiter.countDown();
        log.info("{} stopped.", this.getClass().getSimpleName());
    }

    private void initMongoClient() {
        ConnectionString cs = new ConnectionString(configuration.getMongodbConnectString());
        MongoClientSettings mongoClientSettings = MongoClientSettings.builder().applyConnectionString(cs).build();
        this.mongoClient = MongoClients.create(mongoClientSettings);
    }

}
