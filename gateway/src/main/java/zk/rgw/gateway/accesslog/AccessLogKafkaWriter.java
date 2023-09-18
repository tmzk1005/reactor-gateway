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
package zk.rgw.gateway.accesslog;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import zk.rgw.common.access.AccessLog;
import zk.rgw.common.bootstrap.LifeCycle;
import zk.rgw.common.constant.Constants;
import zk.rgw.common.util.JsonUtil;

@Slf4j
public class AccessLogKafkaWriter implements Consumer<AccessLog>, LifeCycle {

    private static final int QUEUE_SIZE = 16384 * 2;

    private final BlockingQueue<AccessLog> queue = new ArrayBlockingQueue<>(QUEUE_SIZE);

    private final String kafkaBootstrapServers;

    private final String topicName;

    private Producer<String, String> producer;

    private volatile boolean running = false;

    private Thread producerThread;

    public AccessLogKafkaWriter(String kafkaBootstrapServers, String envId) {
        this.kafkaBootstrapServers = kafkaBootstrapServers;
        this.topicName = Constants.ACCESS_LOG_KAFKA_TOPIC_NAME_PREFIX + envId;
    }

    @Override
    public void accept(AccessLog accessLog) {
        try {
            this.queue.add(accessLog);
        } catch (Exception exception) {
            log.error("Failed to add AccessLog into queue.", exception);
        }
    }

    @Override
    public void start() {
        initProducer();
        this.running = true;

        producerThread = new Thread(() -> {
            while (running) {
                send();
            }
        });

        producerThread.setName(this.getClass().getSimpleName());
        producerThread.setDaemon(true);
        producerThread.start();
        log.info("{} started", this.getClass().getSimpleName());
    }

    @Override
    public void stop() {
        running = false;
        if (Objects.nonNull(producerThread)) {
            producerThread.interrupt();
            Thread.yield();

            try {
                producerThread.join();
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
            }

        }

        List<AccessLog> leftInQueue = new ArrayList<>();
        int leftCount = queue.drainTo(leftInQueue);
        if (leftCount > 0) {
            log.info("Send {} access log before stop.", leftCount);
            for (AccessLog accessLog : leftInQueue) {
                doSend(accessLog);
            }
            log.info("Send access log done.");
        }

        producer.close();
        log.info("{} stop", this.getClass().getSimpleName());
    }

    private void send() {
        AccessLog accessLog;
        try {
            accessLog = queue.take();
        } catch (InterruptedException interruptedException) {
            if (running) {
                // 非正常，非主动停止，打印下日志
                log.error("InterruptedException happened while take AccessLog from queue.", interruptedException);
            }
            Thread.currentThread().interrupt();
            return;
        }
        doSend(accessLog);
    }

    private void doSend(AccessLog accessLog) {
        String json;
        try {
            json = JsonUtil.toJson(accessLog);
        } catch (JsonProcessingException jsonProcessingException) {
            log.error("Failed to serialize an AccessLog to json string", jsonProcessingException);
            return;
        }
        producer.send(new ProducerRecord<>(topicName, null, accessLog.getReqTimestamp(), accessLog.getApiId(), json));
    }

    private void initProducer() {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
        properties.put(ProducerConfig.ACKS_CONFIG, "0");
        String clientId = "rgw_kafka_producer_" + this.getClass().getSimpleName();
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
        properties.put(ProducerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, 5000);
        properties.put(ProducerConfig.RECONNECT_BACKOFF_MS_CONFIG, 5000);

        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producer = new KafkaProducer<>(properties);
    }

}
