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

import java.util.Objects;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import zk.rgw.common.definition.IdRouteDefinition;
import zk.rgw.dashboard.framework.exception.BizException;
import zk.rgw.dashboard.utils.ErrorMsgUtil;
import zk.rgw.dashboard.web.bean.ApiPublishStatus;
import zk.rgw.dashboard.web.bean.GwHeartbeatPayload;
import zk.rgw.dashboard.web.bean.GwRegisterPayload;
import zk.rgw.dashboard.web.bean.RouteDefinitionPublishSnapshot;
import zk.rgw.dashboard.web.bean.entity.GatewayNode;
import zk.rgw.dashboard.web.bean.vo.GwRegisterResult;
import zk.rgw.dashboard.web.repository.ApiRepository;
import zk.rgw.dashboard.web.repository.EnvironmentRepository;
import zk.rgw.dashboard.web.repository.GatewayNodeRepository;
import zk.rgw.dashboard.web.repository.factory.RepositoryFactory;
import zk.rgw.dashboard.web.service.GatewayNodeService;

public class GatewayNodeServiceImpl implements GatewayNodeService {

    private final GatewayNodeRepository gatewayNodeRepository = RepositoryFactory.get(GatewayNodeRepository.class);

    private final EnvironmentRepository environmentRepository = RepositoryFactory.get(EnvironmentRepository.class);

    private final ApiRepository apiRepository = RepositoryFactory.get(ApiRepository.class);

    @Override
    public Mono<GwRegisterResult> handleRegister(GwRegisterPayload gwRegisterPayload) {
        String envId = gwRegisterPayload.getEnvId();
        Mono<Void> checkEnvMono = environmentRepository.findOneById(envId)
                .switchIfEmpty(Mono.error(BizException.of(ErrorMsgUtil.envNotExist(envId)))).then();

        Mono<GwRegisterResult> saveNodeMono = gatewayNodeRepository.findOneByAddress(gwRegisterPayload.getAddress())
                .switchIfEmpty(Mono.just(new GatewayNode(gwRegisterPayload.getAddress(), gwRegisterPayload.getEnvId())))
                .doOnNext(node -> {
                    node.setEnvId(gwRegisterPayload.getEnvId());
                    node.setHeartbeat(System.currentTimeMillis());
                }).flatMap(gatewayNodeRepository::save).map(gatewayNode -> new GwRegisterResult(gatewayNode.getId()));

        return checkEnvMono.then(saveNodeMono);
    }

    @Override
    public Mono<Void> handleHeartbeat(GwHeartbeatPayload gwHeartbeatPayload) {
        return gatewayNodeRepository.findOneById(gwHeartbeatPayload.getNodeId())
                .switchIfEmpty(Mono.error(BizException.of("网关节点未注册")))
                .doOnNext(node -> node.setHeartbeat(System.currentTimeMillis()))
                .flatMap(gatewayNodeRepository::save)
                .then();
    }

    @Override
    public Flux<GatewayNode> getNodes(String envId) {
        Bson filter;
        if (Objects.isNull(envId) || envId.isEmpty()) {
            filter = Filters.empty();
        } else {
            try {
                ObjectId envObjId = new ObjectId(envId);
                filter = Filters.eq("envId", envObjId);
            } catch (Exception exception) {
                return Flux.error(BizException.of(ErrorMsgUtil.envNotExist(envId)));
            }
        }
        return gatewayNodeRepository.find(filter);
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

            idRouteDefinition.setTimestamp(snapshot.getLastModifiedDate().toEpochMilli());
            idRouteDefinition.setRouteDefinition(snapshot.getRouteDefinition());
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

}
