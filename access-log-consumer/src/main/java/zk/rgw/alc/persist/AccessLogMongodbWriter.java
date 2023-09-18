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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import zk.rgw.alc.common.AccessLogDocument;
import zk.rgw.alc.common.AccessLogDocumentUtil;
import zk.rgw.common.access.AccessLog;
import zk.rgw.common.bootstrap.LifeCycle;
import zk.rgw.common.constant.Constants;
import zk.rgw.common.util.JsonUtil;

@Slf4j
public class AccessLogMongodbWriter implements LifeCycle {

    private final String kafkaBootstrapServers;

    private Consumer<String, String> accessLogConsumer;

    private Thread consumerThread;

    private final ExecutorService executorService;

    private Scheduler subscribeScheduler;

    private volatile boolean running = false;

    private final String topicName;

    private final String envId;

    private final MongoDatabase mongoDatabase;

    private MongoCollection<AccessLogDocument> accessLogCollection;

    public AccessLogMongodbWriter(
            String kafkaBootstrapServers,
            MongoDatabase mongoDatabase,
            String envId,
            ExecutorService executorService
    ) {
        this.kafkaBootstrapServers = kafkaBootstrapServers;
        this.mongoDatabase = mongoDatabase;
        this.envId = envId;
        this.topicName = Constants.ACCESS_LOG_KAFKA_TOPIC_NAME_PREFIX + envId;
        this.executorService = executorService;
    }

    @Override
    public void start() {
        accessLogCollection = AccessLogDocumentUtil.getAccessLogCollectionForEnv(mongoDatabase, envId).block();
        Objects.requireNonNull(accessLogCollection);

        subscribeScheduler = Schedulers.fromExecutor(executorService);
        subscribeScheduler.init();

        initAccessLogConsumer();
        running = true;
        accessLogConsumer.subscribe(Collections.singleton(topicName));
        String threadName = this.getClass().getSimpleName();
        consumerThread = new Thread(() -> {
            log.info("{} start.", threadName);
            while (running) {
                pull();
            }
            log.info("{} stopped.", threadName);
        }, threadName);
        consumerThread.setDaemon(true);
        consumerThread.start();
    }

    @Override
    public void stop() {
        running = false;
        if (Objects.nonNull(consumerThread)) {
            try {
                consumerThread.join();
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
            }
        }
        if (Objects.nonNull(subscribeScheduler)) {
            subscribeScheduler.dispose();
        }
    }

    private void initAccessLogConsumer() {
        Properties properties = new Properties();
        String clientId = "rgw_kafka_consumer_client_" + this.getClass().getSimpleName() + "_" + envId;
        String groupId = "rgw_kafka_consumer_group_" + this.getClass().getSimpleName() + "_" + envId;
        properties.put(ConsumerConfig.CLIENT_ID_CONFIG, clientId);
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        accessLogConsumer = new KafkaConsumer<>(properties);
    }

    private void pull() {
        List<String> accessLogs;
        try {
            ConsumerRecords<String, String> consumerRecords = accessLogConsumer.poll(Duration.ofMillis(5000));
            if (consumerRecords.isEmpty()) {
                return;
            }
            accessLogs = new ArrayList<>(consumerRecords.count());
            consumerRecords.forEach(consumerRecord -> accessLogs.add(consumerRecord.value()));
        } catch (Exception exception) {
            log.error("Pull AccessLog from kafka exception: ", exception);
            return;
        }

        try {
            saveAccessLogToMongodb(accessLogs);
        } catch (Exception exception) {
            log.error("Save AccessLog to mongodb exception: ", exception);
        }
    }

    private void saveAccessLogToMongodb(List<String> accessLogs) {
        List<AccessLogDocument> accessLogDocuments = new ArrayList<>();

        for (String jsonStr : accessLogs) {
            try {
                AccessLog accessLog = JsonUtil.readValue(jsonStr, AccessLog.class);
                accessLogDocuments.add(AccessLogDocumentUtil.convertFromAccessLog(accessLog));
            } catch (Exception exception) {
                log.error("Failed to deserialize json string to AccessLog instance.", exception);
            }
        }

        Mono.from(accessLogCollection.insertMany(accessLogDocuments))
                .doOnError(throwable -> log.error("Failed to save some access logs to mongodb.", throwable))
                .subscribeOn(subscribeScheduler)
                .subscribe();
    }

}
