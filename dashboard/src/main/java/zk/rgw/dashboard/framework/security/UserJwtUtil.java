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
package zk.rgw.dashboard.framework.security;

import java.time.Instant;
import java.util.Objects;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.Getter;

import zk.rgw.dashboard.framework.filter.auth.AuthenticationException;
import zk.rgw.dashboard.web.bean.entity.User;

public class UserJwtUtil {

    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_USERNAME = "username";

    public static final String CLAIM_NICKNAME = "nickname";

    public static final String CLAIM_ORGANIZATION_ID = "organizationId";
    public static final String CLAIM_ROLE = "role";

    @Getter
    private static Algorithm algorithm;

    @Getter
    private static JWTVerifier jwtVerifier;

    @Getter
    private static int jwtExpireSeconds = 60 * 60;

    private UserJwtUtil() {
    }

    public static void init(String secret, int jwtExpireSeconds) {
        algorithm = Algorithm.HMAC256(secret);
        jwtVerifier = JWT.require(algorithm).build();
        UserJwtUtil.jwtExpireSeconds = jwtExpireSeconds;
    }

    public static String encode(User user) {
        Objects.requireNonNull(algorithm);
        return JWT.create()
                .withExpiresAt(Instant.now().plusSeconds(jwtExpireSeconds))
                .withClaim(CLAIM_USER_ID, user.getId())
                .withClaim(CLAIM_USERNAME, user.getUsername())
                .withClaim(CLAIM_NICKNAME, user.getNickname())
                .withClaim(CLAIM_ORGANIZATION_ID, user.getOrganizationId())
                .withClaim(CLAIM_ROLE, user.getRole().toString())
                .sign(algorithm);
    }

    public static User decode(String jwtToken) throws AuthenticationException {
        Objects.requireNonNull(jwtVerifier);
        DecodedJWT jwt;
        try {
            jwt = jwtVerifier.verify(jwtToken);
        } catch (JWTVerificationException jwtVerificationException) {
            throw new AuthenticationException("认证失败");
        }
        User user = new User();
        user.setId(jwt.getClaim(CLAIM_USER_ID).asString());
        user.setUsername(jwt.getClaim(CLAIM_USERNAME).asString());
        user.setNickname(jwt.getClaim(CLAIM_NICKNAME).asString());
        user.setOrganizationId(jwt.getClaim(CLAIM_ORGANIZATION_ID).asString());
        try {
            user.setRole(Role.valueOf(jwt.getClaim(CLAIM_ROLE).asString()));
            Objects.requireNonNull(user.getId());
            Objects.requireNonNull(user.getUsername());
            Objects.requireNonNull(user.getOrganizationId());
            Objects.requireNonNull(user.getRole());
        } catch (Exception exception) {
            throw new AuthenticationException("认证失败");
        }
        return user;
    }

}
