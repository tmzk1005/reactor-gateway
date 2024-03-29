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
package zk.rgw.dashboard.web.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import zk.rgw.common.definition.IdRouteDefinition;
import zk.rgw.common.definition.SubscriptionRelationship;
import zk.rgw.common.heartbeat.GwHeartbeatPayload;
import zk.rgw.common.heartbeat.GwHeartbeatResult;
import zk.rgw.common.heartbeat.GwRegisterResult;
import zk.rgw.dashboard.framework.annotation.Controller;
import zk.rgw.dashboard.framework.annotation.RequestBody;
import zk.rgw.dashboard.framework.annotation.RequestMapping;
import zk.rgw.dashboard.framework.annotation.RequestParam;
import zk.rgw.dashboard.web.bean.RegisterPayload;
import zk.rgw.dashboard.web.bean.entity.GatewayNode;
import zk.rgw.dashboard.web.bean.vo.GatewayNodeVo;
import zk.rgw.dashboard.web.service.GatewayNodeService;
import zk.rgw.dashboard.web.service.factory.ServiceFactory;
import zk.rgw.http.context.ReactiveRequestContextHolder;

@Controller("gateway")
public class GatewayNodeController {

    private final GatewayNodeService gatewayNodeService = ServiceFactory.get(GatewayNodeService.class);

    @RequestMapping(path = "/_heartbeat", method = RequestMapping.Method.POST)
    public Mono<GwHeartbeatResult> heartbeat(@RequestBody GwHeartbeatPayload gwHeartbeatPayload) {
        return gatewayNodeService.handleHeartbeat(gwHeartbeatPayload);
    }

    @RequestMapping(path = "/_register", method = RequestMapping.Method.POST)
    public Mono<GwRegisterResult> register(@RequestBody RegisterPayload registerPayload) {
        return ReactiveRequestContextHolder.getRequest().flatMap(request -> {
            String ip = Objects.requireNonNull(request.remoteAddress()).getAddress().getHostAddress();
            registerPayload.setServerIp(ip);
            return gatewayNodeService.handleRegister(registerPayload);
        });
    }

    @RequestMapping(path = "/nodes")
    public Mono<Map<String, List<GatewayNodeVo>>> getAllNodes(@RequestParam(name = "envId", required = false) String envId) {
        return gatewayNodeService.getNodes(envId).collectList().map(nodeList -> {
            Map<String, List<GatewayNodeVo>> map = new HashMap<>();
            for (GatewayNode node : nodeList) {
                String nodeEnvId = node.getEnvironment().getId();
                if (!map.containsKey(nodeEnvId)) {
                    map.put(nodeEnvId, new ArrayList<>());
                }
                map.get(nodeEnvId).add(new GatewayNodeVo().initFromPo(node));
            }
            return map;
        });
    }

    @RequestMapping(path = "/_sync")
    public Flux<IdRouteDefinition> syncRouteDefinitions(
            @RequestParam(name = "envId") String envId,
            @RequestParam(name = "seq", required = false, defaultValue = "0") long seq
    ) {
        return gatewayNodeService.syncRouteDefinitions(envId, seq);
    }

    @RequestMapping(path = "/_sync-app")
    public Flux<SubscriptionRelationship> syncApps(
            @RequestParam(name = "seq", required = false, defaultValue = "0") long seq
    ) {
        return gatewayNodeService.syncApiSubscriptions(seq);
    }

}
