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
package zk.rgw.dashboard.web.event.listener;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.client.HttpClient;

import zk.rgw.common.event.RgwEvent;
import zk.rgw.common.event.RgwEventListener;
import zk.rgw.common.event.impl.EnvironmentChangedEvent;
import zk.rgw.common.exception.RgwRuntimeException;
import zk.rgw.common.heartbeat.Notification;
import zk.rgw.common.util.JsonUtil;
import zk.rgw.dashboard.web.bean.entity.GatewayNode;
import zk.rgw.dashboard.web.event.ApiPublishingEvent;
import zk.rgw.dashboard.web.repository.GatewayNodeRepository;
import zk.rgw.dashboard.web.repository.factory.RepositoryFactory;

@SuppressWarnings("java:S1075")
@Slf4j
public class DashboardEventListener implements RgwEventListener<RgwEvent> {

    private static final String GATEWAY_API_CONTEXT_PATH = "/__rgw_internal";

    private static final String PATH_NOTIFICATION = "/notification";

    private final GatewayNodeRepository gatewayNodeRepository = RepositoryFactory.get(GatewayNodeRepository.class);

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    private final HttpClient httpClient = HttpClient.create();

    @Override
    public void onEvent(RgwEvent event) {
        if (event instanceof ApiPublishingEvent apiPublishingEvent) {
            handleApiPublishingEvent(apiPublishingEvent);
        } else if (event instanceof EnvironmentChangedEvent environmentChangedEvent) {
            handleEnvironmentChangedEvent(environmentChangedEvent);
        }
    }

    private void handleApiPublishingEvent(ApiPublishingEvent event) {
        log.info(
                "detect an api {} event, envId: {}, apiId: {}",
                event.isAdd() ? "publish" : "unpublish",
                event.getEnvId(),
                event.getRouteDefinition().getId()
        );
        executorService.submit(() -> notifyGatewayNodesApiUpdated(event.getEnvId()));
    }

    private void handleEnvironmentChangedEvent(EnvironmentChangedEvent event) {
        log.info(
                "detect an environment changed event, env id: {}, org id: {}. going to notify gateway nodes.",
                event.getEnvId(), event.getOrgId()
        );
        executorService.submit(() -> notifyGatewayNodesEnvironmentChanged(event));
    }

    private void notifyGatewayNodesApiUpdated(String envId) {
        Notification notification = new Notification();
        notification.setApiUpdated(true);
        notifyByEnvId(envId, notification);
    }

    private void notifyGatewayNodesEnvironmentChanged(EnvironmentChangedEvent event) {
        Notification notification = new Notification();
        notification.setEnvironmentUpdated(true);
        notification.setEnvironmentChangedEvent(event);
        notifyByEnvId(event.getEnvId(), notification);
    }

    private void notifyByEnvId(String envId, Notification notification) {
        gatewayNodeRepository.findAllByEnvId(envId)
                .parallel()
                .flatMap(gatewayNode -> notifyGatewayNode(gatewayNode, notification))
                .subscribe();
    }

    private Mono<Void> notifyGatewayNode(GatewayNode gatewayNode, Notification notification) {
        String jsonStr;
        try {
            jsonStr = JsonUtil.toJson(notification);
        } catch (Exception exception) {
            // Not possible, just throw RTE
            throw new RgwRuntimeException(exception);
        }

        String uri = gatewayNode.getAddress() + GATEWAY_API_CONTEXT_PATH + PATH_NOTIFICATION;
        log.debug("Send notification to gateway node: {} , {}", uri, notification);
        return httpClient.post()
                .uri(uri)
                .send(ByteBufFlux.fromString(Mono.just(jsonStr)))
                .response()
                .doOnNext(httpClientResponse -> {
                    if (httpClientResponse.status().code() != HttpResponseStatus.OK.code()) {
                        log.error("Sent notification to gateway node {} succeed, but received not ok status.", uri);
                    }
                }).then();
    }

}
