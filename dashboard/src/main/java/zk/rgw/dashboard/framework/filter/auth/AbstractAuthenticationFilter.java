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

package zk.rgw.dashboard.framework.filter.auth;

import java.util.List;
import java.util.Objects;

import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;

import zk.rgw.dashboard.framework.context.ContextUtil;
import zk.rgw.dashboard.web.bean.entity.User;
import zk.rgw.http.path.AntPathMatcher;
import zk.rgw.plugin.api.Exchange;
import zk.rgw.plugin.api.filter.Filter;
import zk.rgw.plugin.api.filter.FilterChain;
import zk.rgw.plugin.util.ResponseUtil;

public abstract class AbstractAuthenticationFilter implements Filter {

    private final List<String> noNeedLoginPaths;

    protected AbstractAuthenticationFilter(List<String> noNeedLoginPaths) {
        this.noNeedLoginPaths = noNeedLoginPaths;
    }

    @Override
    public Mono<Void> filter(Exchange exchange, FilterChain chain) {
        Mono<User> userMono;
        try {
            userMono = doAuthorization(exchange.getRequest());
        } catch (AuthenticationException exception) {
            return ResponseUtil.sendStatus(exchange.getResponse(), HttpResponseStatus.UNAUTHORIZED, exception.getMessage());
        }

        Objects.requireNonNull(userMono);
        return userMono.flatMap(user -> {
            if (Objects.isNull(user.getId()) && Objects.isNull(user.getName())) {
                return handleAnonymousRequest(exchange, chain);
            } else {
                ContextUtil.setUser(userMono);
                return chain.filter(exchange);
            }
        });
    }

    protected abstract Mono<User> doAuthorization(HttpServerRequest request) throws AuthenticationException;

    private Mono<Void> handleAnonymousRequest(Exchange exchange, FilterChain chain) {
        if (noNeedLogin(exchange.getRequest())) {
            return chain.filter(exchange);
        }
        return ResponseUtil.sendStatus(exchange.getResponse(), HttpResponseStatus.UNAUTHORIZED);
    }

    private boolean noNeedLogin(final HttpServerRequest request) {
        for (String pattern : noNeedLoginPaths) {
            if (AntPathMatcher.getDefaultInstance().match(pattern, request.fullPath())) {
                return true;
            }
        }
        return false;
    }

}
