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

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.netty.handler.codec.http.HttpHeaderNames;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;

import zk.rgw.dashboard.framework.context.ContextUtil;
import zk.rgw.dashboard.web.bean.entity.User;

public class JwtAuthenticationFilter extends AbstractAuthenticationFilter {

    private final JWTVerifier jwtVerifier;

    public JwtAuthenticationFilter(List<String> noNeedLoginPaths, String hmac256Secret) {
        super(noNeedLoginPaths);
        Objects.requireNonNull(hmac256Secret);
        this.jwtVerifier = JWT.require(Algorithm.HMAC256(hmac256Secret)).build();
    }

    @Override
    protected Mono<User> doAuthorization(HttpServerRequest request) throws AuthenticationException {
        String authorizationKey = request.requestHeaders().get(HttpHeaderNames.AUTHORIZATION);
        if (Objects.isNull(authorizationKey)) {
            return ContextUtil.ANONYMOUS_USER;
        }
        DecodedJWT jwt;
        try {
            jwt = this.jwtVerifier.verify(authorizationKey);
        } catch (JWTVerificationException jwtVerificationException) {
            throw new AuthenticationException("认证失败");
        }
        User user = new User();
        user.setName(jwt.getClaim("name").asString());
        return Mono.just(user);
    }

}
