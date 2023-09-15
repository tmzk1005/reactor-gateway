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

package zk.rgw.gateway.app;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;

import zk.rgw.common.definition.AppDefinition;
import zk.rgw.common.util.ObjectUtil;
import zk.rgw.gateway.sdk.app.AppAuthInfo;
import zk.rgw.gateway.sdk.app.HttpRequestSigner;
import zk.rgw.http.utils.RouteUtil;
import zk.rgw.plugin.api.Exchange;
import zk.rgw.plugin.api.filter.Filter;
import zk.rgw.plugin.api.filter.FilterChain;
import zk.rgw.plugin.util.ResponseUtil;

@Slf4j
public class AppAuthFilter implements Filter {

    private static final int SIGN_EXPIRE_TIME_SECONDS = 600;

    private final AppSubscribeRouteManager appSubscribeRouteManager;

    public AppAuthFilter(AppSubscribeRouteManager appSubscribeRouteManager) {
        this.appSubscribeRouteManager = appSubscribeRouteManager;
    }

    @Override
    public Mono<Void> filter(Exchange exchange, FilterChain chain) {
        String authorization = exchange.getRequest().requestHeaders().get(AppAuthInfo.APP_AUTH_HEADER_NAME);
        if (Objects.isNull(authorization)) {
            return ResponseUtil.sendStatus(exchange.getResponse(), HttpResponseStatus.UNAUTHORIZED, "APP认证失败，无认证头X-Rgw-Authorization");
        }

        String routeId = RouteUtil.getRoute(exchange).getId();
        Map<String, AppDefinition> apps = appSubscribeRouteManager.getForRouteId(routeId);
        if (ObjectUtil.isEmpty(apps) || !apps.containsKey(routeId)) {
            return ResponseUtil.sendStatus(exchange.getResponse(), HttpResponseStatus.UNAUTHORIZED, "订阅关系可能未同步，请稍后再尝试");
        }

        final AppAuthInfo appAuthInfo = AppAuthInfo.parseAuthorization(authorization);
        if (Objects.isNull(appAuthInfo)) {
            return ResponseUtil.sendStatus(exchange.getResponse(), HttpResponseStatus.UNAUTHORIZED, "X-Rgw-Authorization非法，解析失败");
        }

        Date signTime = appAuthInfo.getSignTime();
        final Calendar calendar = Calendar.getInstance();
        Date curTime = new Date();
        calendar.setTime(curTime);
        calendar.add(Calendar.SECOND, -SIGN_EXPIRE_TIME_SECONDS);
        if (signTime.before(calendar.getTime())) {
            return ResponseUtil.sendStatus(exchange.getResponse(), HttpResponseStatus.UNAUTHORIZED, "X-Rgw-Authorization签名过期");
        }

        final String accessKey = appAuthInfo.getAccessKey();
        final AppDefinition appDefinition = apps.get(accessKey);
        if (Objects.isNull(appDefinition)) {
            return ResponseUtil.sendStatus(exchange.getResponse(), HttpResponseStatus.UNAUTHORIZED, "未订阅此API");
        }

        final HttpServerRequest request = exchange.getRequest();
        HttpRequestSigner httpRequestSigner = new HttpRequestSigner(accessKey, appDefinition.getSecret());

        Map<String, String> partOfHeaders = extractHeaders(request.requestHeaders(), appAuthInfo.getHeaderNames());

        AppAuthInfo expectAppAuthInfo;
        try {
            expectAppAuthInfo = httpRequestSigner.signRequest(request.method().name(), request.uri(), partOfHeaders);
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            log.error("Failed to check signature of request.", exception);
            return ResponseUtil.sendStatus(exchange.getResponse(), HttpResponseStatus.INTERNAL_SERVER_ERROR, "校验请求签名异常");
        }

        if (Objects.equals(appAuthInfo.getSignature(), expectAppAuthInfo.getSignature())) {
            return chain.filter(exchange);
        } else {
            return ResponseUtil.sendStatus(exchange.getResponse(), HttpResponseStatus.UNAUTHORIZED, "X-Rgw-Authorization签名未通过校验");
        }
    }

    private static Map<String, String> extractHeaders(HttpHeaders httpHeaders, List<String> headerNames) {
        Map<String, String> partOfHeaders = new HashMap<>();
        for (String headerName : headerNames) {
            String headerValue = httpHeaders.get(headerName);
            if (Objects.nonNull(headerValue)) {
                partOfHeaders.put(headerName, headerValue);
            }
        }
        return partOfHeaders;
    }

}
