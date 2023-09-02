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

import java.time.Instant;

import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import zk.rgw.common.heartbeat.JvmMetrics;
import zk.rgw.dashboard.framework.mongodb.MongodbUtil;
import zk.rgw.dashboard.web.bean.entity.GatewayNode;
import zk.rgw.dashboard.web.bean.entity.GatewayNodeMetrics;

@Slf4j
public class GatewayNodeMetricsRepository {

    private final MongoCollection<GatewayNodeMetrics> collection;

    public GatewayNodeMetricsRepository(MongoDatabase mongoDatabase) {
        this.collection = mongoDatabase.getCollection(MongodbUtil.getCollectionName(GatewayNodeMetrics.class), GatewayNodeMetrics.class);
    }

    public Mono<Void> insert(GatewayNode node, JvmMetrics jvmMetrics) {
        GatewayNodeMetrics gatewayNodeMetrics = new GatewayNodeMetrics();
        gatewayNodeMetrics.setGatewayNode(node);
        gatewayNodeMetrics.setTimestampMillis(Instant.now());
        gatewayNodeMetrics.setJvmMetrics(jvmMetrics);
        return Mono.from(collection.insertOne(gatewayNodeMetrics)).doOnNext(insertOneResult -> {
            if (!insertOneResult.wasAcknowledged()) {
                log.error("Insert gateway node metrics got unacknowledged result, node id is {}", node.getId());
            }
        }).then();
    }

}
