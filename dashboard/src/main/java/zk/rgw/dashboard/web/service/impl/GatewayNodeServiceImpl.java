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
package zk.rgw.dashboard.web.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import org.bson.BsonDocument;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import zk.rgw.common.definition.IdRouteDefinition;
import zk.rgw.common.heartbeat.GwHeartbeatPayload;
import zk.rgw.common.heartbeat.GwHeartbeatResult;
import zk.rgw.common.heartbeat.GwRegisterResult;
import zk.rgw.common.heartbeat.SyncState;
import zk.rgw.dashboard.framework.exception.BizException;
import zk.rgw.dashboard.utils.ErrorMsgUtil;
import zk.rgw.dashboard.web.bean.ApiPublishStatus;
import zk.rgw.dashboard.web.bean.RegisterPayload;
import zk.rgw.dashboard.web.bean.RouteDefinitionPublishSnapshot;
import zk.rgw.dashboard.web.bean.entity.Environment;
import zk.rgw.dashboard.web.bean.entity.GatewayNode;
import zk.rgw.dashboard.web.bean.entity.RgwSequence;
import zk.rgw.dashboard.web.repository.ApiRepository;
import zk.rgw.dashboard.web.repository.EnvBindingRepository;
import zk.rgw.dashboard.web.repository.EnvironmentRepository;
import zk.rgw.dashboard.web.repository.GatewayNodeRepository;
import zk.rgw.dashboard.web.repository.RgwSequenceRepository;
import zk.rgw.dashboard.web.repository.factory.RepositoryFactory;
import zk.rgw.dashboard.web.service.GatewayNodeService;

public class GatewayNodeServiceImpl implements GatewayNodeService {

    private static final List<Bson> LOOKUP = List.of(
            Aggregates.lookup("Environment", "environment", "_id", "environmentLookup"),
            Aggregates.project(
                    Projections.fields(
                            Projections.include("_id", "address", "heartbeat"),
                            Projections.computed("environment", BsonDocument.parse("{\"$first\": \"$environmentLookup\"}"))
                    )
            )
    );

    private final GatewayNodeRepository gatewayNodeRepository = RepositoryFactory.get(GatewayNodeRepository.class);

    private final EnvironmentRepository environmentRepository = RepositoryFactory.get(EnvironmentRepository.class);

    private final EnvBindingRepository envBindingRepository = RepositoryFactory.get(EnvBindingRepository.class);

    private final ApiRepository apiRepository = RepositoryFactory.get(ApiRepository.class);

    private final RgwSequenceRepository rgwSequenceRepository = RepositoryFactory.get(RgwSequenceRepository.class);

    @Override
    public Mono<GwRegisterResult> handleRegister(RegisterPayload registerPayload) {
        String envId = registerPayload.getEnvId();
        Mono<Environment> checkEnvMono = environmentRepository.findOneById(envId)
                .switchIfEmpty(Mono.error(BizException.of(ErrorMsgUtil.envNotExist(envId))));

        return checkEnvMono.flatMap(
                environment -> gatewayNodeRepository.findOneByAddress(registerPayload.getAddress())
                        .switchIfEmpty(Mono.just(new GatewayNode(registerPayload.getAddress(), environment)))
                        .doOnNext(node -> node.setHeartbeat(System.currentTimeMillis()))
                        .flatMap(gatewayNodeRepository::save)
                        .map(gatewayNode -> new GwRegisterResult(gatewayNode.getId()))
        );
    }

    @Override
    public Mono<GwHeartbeatResult> handleHeartbeat(GwHeartbeatPayload gwHeartbeatPayload) {
        return gatewayNodeRepository.findOneById(gwHeartbeatPayload.getNodeId())
                .switchIfEmpty(Mono.error(BizException.of("网关节点未注册")))
                .doOnNext(node -> node.setHeartbeat(System.currentTimeMillis()))
                .flatMap(gatewayNodeRepository::save)
                .flatMap(gatewayNode -> checkSyncState(gatewayNode, gwHeartbeatPayload.getSyncState()));
    }

    private Mono<GwHeartbeatResult> checkSyncState(GatewayNode gatewayNode, SyncState reportedSyncState) {
        return rgwSequenceRepository.getAll(gatewayNode.getEnvironment().getId())
                .map(GatewayNodeServiceImpl::parseSeqValuesToExpectSyncState)
                .flatMap(expect -> generateGwHeartbeatResult(gatewayNode.getEnvironment().getId(), reportedSyncState, expect));
    }

    private Mono<GwHeartbeatResult> generateGwHeartbeatResult(String envId, SyncState reportedSyncState, SyncState expectSyncState) {
        GwHeartbeatResult gwHeartbeatResult = new GwHeartbeatResult();
        gwHeartbeatResult.setSyncState(expectSyncState);

        if (reportedSyncState.getApiOpSeq() < expectSyncState.getApiOpSeq()) {
            gwHeartbeatResult.setApiBehind(true);
        }
        List<String> envBehind = new ArrayList<>();
        Map<String, Long> orgEnvOpSeqMap = reportedSyncState.getOrgEnvOpSeqMap();
        for (Map.Entry<String, Long> entry : expectSyncState.getOrgEnvOpSeqMap().entrySet()) {
            String orgId = entry.getKey();
            if (orgEnvOpSeqMap.getOrDefault(orgId, 0L) < entry.getValue()) {
                envBehind.add(orgId);
            }
        }

        if (envBehind.isEmpty()) {
            return Mono.just(gwHeartbeatResult);
        }

        gwHeartbeatResult.setEnvBehind(true);
        return envBindingRepository.findByEnvAndOrgIdsAsMap(envId, envBehind).map(mapData -> {
            gwHeartbeatResult.setEnvironments(mapData);
            return gwHeartbeatResult;
        });
    }

    @Override
    public Flux<GatewayNode> getNodes(String envId) {
        Bson filter;
        if (Objects.isNull(envId) || envId.isEmpty()) {
            filter = Filters.empty();
        } else {
            try {
                ObjectId envObjId = new ObjectId(envId);
                filter = Filters.eq("environment", envObjId);
            } catch (Exception exception) {
                return Flux.error(BizException.of(ErrorMsgUtil.envNotExist(envId)));
            }
        }

        return gatewayNodeRepository.find(filter, null, LOOKUP);
    }

    @Override
    public Flux<IdRouteDefinition> syncRouteDefinitions(String envId, long seq) {
        return environmentRepository.existsById(envId).flatMapMany(exists -> {
            if (Boolean.TRUE.equals(exists)) {
                return doSyncRouteDefinitions(envId, seq);
            } else {
                throw new BizException(ErrorMsgUtil.envNotExist(envId));
            }
        });
    }

    private Flux<IdRouteDefinition> doSyncRouteDefinitions(String envId, long seq) {
        final Bson filter = filterForSyncRouteDefinition(envId, seq);
        Bson sort = Sorts.ascending("publishSnapshots." + envId + ".opSeq");
        return apiRepository.find(filter, sort).map(api -> {
            IdRouteDefinition idRouteDefinition = new IdRouteDefinition();
            idRouteDefinition.setId(api.getId());
            idRouteDefinition.setOrgId(api.getOrganization().getId());

            RouteDefinitionPublishSnapshot snapshot = api.getPublishSnapshots().get(envId);
            idRouteDefinition.setSeqNum(snapshot.getOpSeq());

            if (!snapshot.isStatusUnpublished()) {
                idRouteDefinition.setRouteDefinition(snapshot.getRouteDefinition());
            }
            // 未发布，说明是API下线了，以不设置routeDefinition的方式来告知gateway是下线了
            return idRouteDefinition;
        });
    }

    private static Bson filterForSyncRouteDefinition(String envId, long seq) {
        Bson filter = Filters.exists("publishSnapshots." + envId);
        if (seq == 0L) {
            filter = Filters.and(
                    filter,
                    Filters.ne("publishSnapshots." + envId + ".publishStatus", ApiPublishStatus.UNPUBLISHED.name())
            );
        } else {
            filter = Filters.and(
                    filter,
                    Filters.gt("publishSnapshots." + envId + ".opSeq", seq)
            );
        }
        return filter;
    }

    private static SyncState parseSeqValuesToExpectSyncState(Map<String, Long> seqValues) {
        SyncState syncState = new SyncState();
        // env_${envId}_
        // envId是24个字符
        int prefixLen = 29;
        for (Map.Entry<String, Long> entry : seqValues.entrySet()) {
            String rawKey = entry.getKey();
            String key = rawKey.substring(prefixLen);
            if (RgwSequence.API_PUBLISH_ACTION.equals(key)) {
                syncState.setApiOpSeq(entry.getValue() - 1);
            } else if (key.startsWith("org_") && key.endsWith("_binding_action")) {
                String orgId = key.substring(4, 4 + 24);
                syncState.getOrgEnvOpSeqMap().put(orgId, entry.getValue() - 1);
            }
        }
        return syncState;
    }

}
