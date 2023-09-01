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

package zk.rgw.gateway.heartbeat;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.Connection;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;

import zk.rgw.common.bootstrap.LifeCycle;
import zk.rgw.common.event.EventPublisher;
import zk.rgw.common.event.RgwEvent;
import zk.rgw.common.event.RgwEventListener;
import zk.rgw.common.event.impl.EnvironmentChangedEvent;
import zk.rgw.common.exception.RgwRuntimeException;
import zk.rgw.common.heartbeat.GwHeartbeatPayload;
import zk.rgw.common.heartbeat.GwHeartbeatResult;
import zk.rgw.common.heartbeat.GwRegisterPayload;
import zk.rgw.common.heartbeat.GwRegisterResult;
import zk.rgw.common.heartbeat.Notification;
import zk.rgw.common.util.JsonUtil;
import zk.rgw.gateway.event.ApiOpSeqBehindEvent;
import zk.rgw.gateway.event.ApiOpSeqUpdateEvent;
import zk.rgw.gateway.event.EnvBehindEvent;
import zk.rgw.gateway.event.NotificationEvent;
import zk.rgw.plugin.util.Shuck;

@SuppressWarnings("java:S1075")
@Slf4j
public class HeartbeatReporter implements LifeCycle, RgwEventListener<RgwEvent> {

    private static final String PATH_REGISTER = "/gateway/_register";
    private static final String PATH_HEARTBEAT = "/gateway/_heartbeat";

    private final String environmentId;

    private final HttpClient httpClient;

    private final String pathRegister;

    private final String pathHeartbeat;

    private final int interval;

    private final ScheduledExecutorService scheduledExecutorService;

    private final String serverSchema;

    private final int serverPort;

    private final GwHeartbeatPayload heartbeatPayload = new GwHeartbeatPayload();

    @Setter
    private EventPublisher<RgwEvent> eventPublisher;

    public HeartbeatReporter(
            String dashboardAddress, String dashboardApiContextPath, String environmentId,
            int interval, String serverSchema, int serverPort
    ) {
        this.environmentId = environmentId;
        this.interval = interval;
        this.serverSchema = serverSchema;
        this.serverPort = serverPort;

        this.pathRegister = dashboardApiContextPath + PATH_REGISTER;
        this.pathHeartbeat = dashboardApiContextPath + PATH_HEARTBEAT;

        this.httpClient = HttpClient.create().headers(
                headers -> headers.add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
        ).baseUrl(dashboardAddress);

        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void start() {
        if (scheduledExecutorService.isShutdown() || scheduledExecutorService.isTerminated()) {
            log.error("Can not start {} because it's already started.", this.getClass().getSimpleName());
            return;
        }
        log.info("Start {}.", this.getClass().getSimpleName());
        register().doOnNext(this.heartbeatPayload::setNodeId)
                .doOnSuccess(
                        unused -> scheduledExecutorService.scheduleAtFixedRate(HeartbeatReporter.this::heartbeat, 0, interval, TimeUnit.SECONDS)
                ).subscribe();
    }

    @Override
    public void stop() {
        if (!scheduledExecutorService.isShutdown()) {
            log.info("Stop {}", this.getClass().getSimpleName());
            scheduledExecutorService.shutdownNow();
        }
    }

    private void heartbeat() {
        log.debug("Send heart beat to dashboard");

        httpClient.post()
                .uri(pathHeartbeat)
                .send(toByteBufFlux(heartbeatPayload))
                .responseConnection(this::handleHeartbeatResp)
                .doOnError(exception -> log.error("Failed to send heartbeat information to dashboard.", exception))
                .subscribe();
    }

    private Mono<String> register() {
        log.info("Send register request to dashboard");

        GwRegisterPayload gwRegisterPayload = new GwRegisterPayload();

        gwRegisterPayload.setServerSchema(serverSchema);
        gwRegisterPayload.setServerPort(serverPort);
        gwRegisterPayload.setEnvId(environmentId);

        return httpClient.post()
                .uri(pathRegister)
                .send(toByteBufFlux(gwRegisterPayload))
                .responseContent()
                .aggregate()
                .asInputStream()
                .map(inputStream -> {
                    try {
                        RegisterResp registerResp = JsonUtil.readValue(inputStream, RegisterResp.class);
                        return registerResp.getData().getNodeId();
                    } catch (IOException ioException) {
                        throw new RgwRuntimeException("Failed to deserialize response of register request sent to dashboard.");
                    }
                })
                .doOnError(exception -> {
                    log.error("Failed to send register request to dashboard,", exception);
                    log.error(
                            "Process is going to shutdown because of failed to register to dashboard, " +
                                    "please make sure dashboard is running, and configured the dashboard address."
                    );
                    System.exit(2);
                });
    }

    @Override
    public void onEvent(RgwEvent event) {
        if (event instanceof NotificationEvent notificationEvent) {
            Notification notification = notificationEvent.getNotification();
            if (notification.isEnvironmentUpdated()) {
                EnvironmentChangedEvent environmentChangedEvent = notification.getEnvironmentChangedEvent();
                heartbeatPayload.getSyncState().getOrgEnvOpSeqMap().put(environmentChangedEvent.getOrgId(), environmentChangedEvent.getOpSeq());
            }
        } else if (event instanceof ApiOpSeqUpdateEvent apiOpSeqUpdateEvent) {
            heartbeatPayload.getSyncState().setApiOpSeq(apiOpSeqUpdateEvent.getValue());
        }
    }

    private Mono<Void> handleHeartbeatResp(HttpClientResponse clientResponse, Connection connection) {
        HttpResponseStatus status = clientResponse.status();
        if (HttpResponseStatus.OK.code() != status.code()) {
            log.error("Heartbeat response status not ok!");
            return Mono.empty();
        }
        return connection.inbound().receive().aggregate().asString().flatMap(jsonStr -> {
            HeartbeatResp heartbeatResp;
            try {
                heartbeatResp = JsonUtil.readValue(jsonStr, HeartbeatResp.class);
            } catch (Exception exception) {
                log.error("Failed to deserialize to HeartbeatResp.class", exception);
                return Mono.empty();
            }
            return handleHeartbeatResult(heartbeatResp.getData());
        });
    }

    private Mono<Void> handleHeartbeatResult(GwHeartbeatResult heartbeatResult) {
        heartbeatPayload.setSyncState(heartbeatResult.getSyncState());

        if (heartbeatResult.isApiBehind()) {
            eventPublisher.publishEvent(new ApiOpSeqBehindEvent());
        }
        if (heartbeatResult.isEnvBehind()) {
            eventPublisher.publishEvent(new EnvBehindEvent(heartbeatResult.getEnvironments()));
        }

        return Mono.empty();
    }

    private static Publisher<? extends ByteBuf> toByteBufFlux(Object data) {
        try {
            String jsonContent = JsonUtil.toJson(data);
            return ByteBufFlux.fromString(Mono.just(jsonContent));
        } catch (Exception exception) {
            throw new RgwRuntimeException(exception);
        }
    }

    static class RegisterResp extends Shuck<GwRegisterResult> {
    }

    static class HeartbeatResp extends Shuck<GwHeartbeatResult> {
    }

}
