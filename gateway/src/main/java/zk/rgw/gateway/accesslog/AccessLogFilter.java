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

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import io.netty.handler.codec.http.HttpHeaders;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;

import zk.rgw.common.access.AccessLog;
import zk.rgw.common.definition.AccessLogConf;
import zk.rgw.http.exchange.DefaultExchangeBuilder;
import zk.rgw.http.route.Route;
import zk.rgw.http.utils.RouteUtil;
import zk.rgw.plugin.api.Exchange;
import zk.rgw.plugin.api.filter.Filter;
import zk.rgw.plugin.api.filter.FilterChain;

public class AccessLogFilter implements Filter {

    private final Consumer<AccessLog> accessLogConsumer;

    public AccessLogFilter(Consumer<AccessLog> accessLogConsumer) {
        this.accessLogConsumer = accessLogConsumer;
    }

    @Override
    public Mono<Void> filter(Exchange exchange, FilterChain chain) {
        Route route = RouteUtil.getRoute(exchange);
        if (Objects.isNull(route) || Objects.isNull(route.getAccessLogConf())) {
            return chain.filter(exchange);
        }
        return new AccessLogAuditContext(exchange, chain).run();
    }

    class AccessLogAuditContext {

        private Exchange exchange;

        private final AccessLog accessLog;

        private final FilterChain filterChain;

        private final Route route;

        private final AccessLogConf accessLogConf;

        AccessLogAuditContext(Exchange exchange, FilterChain filterChain) {
            this.exchange = exchange;
            this.filterChain = filterChain;
            this.accessLog = new AccessLog();
            this.route = RouteUtil.getRoute(exchange);
            this.accessLogConf = this.route.getAccessLogConf();
        }

        Mono<Void> run() {
            HttpServerRequest request = exchange.getRequest();

            accessLog.setRequestId(request.requestId());
            accessLog.setReqTimestamp(System.currentTimeMillis());
            accessLog.setApiId(route.getId());

            String clientIp = Objects.requireNonNull(request.remoteAddress()).getAddress().getHostAddress();
            AccessLog.ClientInfo clientInfo = new AccessLog.ClientInfo();
            clientInfo.setIp(clientIp);
            accessLog.setClientInfo(clientInfo);

            AccessLog.RequestInfo requestInfo = new AccessLog.RequestInfo();
            requestInfo.setMethod(request.method().name());
            requestInfo.setUri(request.uri());

            if (accessLogConf.isReqHeadersEnabled()) {
                requestInfo.setHeaders(copyHeaders(request.requestHeaders()));
            }

            accessLog.setRequestInfo(requestInfo);

            if (accessLogConf.isReqBodyEnabled() || accessLogConf.isRespBodyEnabled()) {
                buildNewExchange();
            }

            return filterChain.filter(exchange)
                    .doOnSuccess(ignore -> doAuditInResponseStage())
                    .doOnError(ignore -> doAuditInResponseStage())
                    .doOnCancel(this::doAuditInResponseStage);
        }

        private void buildNewExchange() {
            long reqBodyLimit = accessLogConf.isReqBodyEnabled() ? accessLogConf.getBodyLimit() : 0L;
            BodyAuditableRequest bodyAuditableRequest = new BodyAuditableRequest(exchange.getRequest(), reqBodyLimit);

            long respBodyLimit = accessLogConf.isRespBodyEnabled() ? accessLogConf.getBodyLimit() : 0L;
            BodyAuditableResponse bodyAuditableResponse = new BodyAuditableResponse(exchange.getResponse(), respBodyLimit);

            this.exchange = new DefaultExchangeBuilder(exchange).request(bodyAuditableRequest).response(bodyAuditableResponse).build();
        }

        void doAuditInResponseStage() {
            BodyAuditableRequest bodyAuditableRequest = (BodyAuditableRequest) exchange.getRequest();
            accessLog.getRequestInfo().setBodySize(bodyAuditableRequest.getBodySize());
            byte[] auditRequestBodyBytes = bodyAuditableRequest.getAuditBody();
            try {
                String auditRequestBodyString = new String(auditRequestBodyBytes);
                accessLog.getRequestInfo().setBody(auditRequestBodyString);
                accessLog.getRequestInfo().setBodyBase64(false);
            } catch (Exception exception) {
                accessLog.getRequestInfo().setBody(Base64.getEncoder().encodeToString(auditRequestBodyBytes));
                accessLog.getRequestInfo().setBodyBase64(true);
            }

            AccessLog.ResponseInfo responseInfo = new AccessLog.ResponseInfo();
            responseInfo.setCode(exchange.getResponse().status().code());

            if (accessLogConf.isRespHeadersEnabled()) {
                responseInfo.setHeaders(copyHeaders(exchange.getResponse().responseHeaders()));
            }

            BodyAuditableResponse bodyAuditableResponse = (BodyAuditableResponse) exchange.getResponse();
            responseInfo.setBodySize(bodyAuditableResponse.getBodySize());
            byte[] auditResponseBodyBytes = bodyAuditableResponse.getAuditBody();
            try {
                String auditResponseBodyString = new String(auditResponseBodyBytes);
                responseInfo.setBody(auditResponseBodyString);
                responseInfo.setBodyBase64(false);
            } catch (Exception exception) {
                responseInfo.setBody(Base64.getEncoder().encodeToString(auditResponseBodyBytes));
                responseInfo.setBodyBase64(true);
            }

            accessLog.setResponseInfo(responseInfo);

            accessLog.setExtraInfo(exchange.getAuditInfo());

            accessLog.setRespTimestamp(System.currentTimeMillis());
            accessLog.setMillisCost((int) (accessLog.getRespTimestamp() - accessLog.getReqTimestamp()));

            accessLogConsumer.accept(accessLog);
        }

    }

    private static Map<String, List<String>> copyHeaders(HttpHeaders httpHeaders) {
        Map<String, List<String>> map = new HashMap<>(2 * httpHeaders.size());
        for (String key : httpHeaders.names()) {
            map.put(key, httpHeaders.getAll(key));
        }
        return map;
    }

}
