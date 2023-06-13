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
package zk.rgw.dashboard.filter.auth;

import java.time.Instant;
import java.util.Base64;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("此测试类是为了手动测试JWT类库")
class JwtLibTest {

    @Test
    void test() {
        String secret = "123";
        Algorithm algorithm = Algorithm.HMAC256(secret);

        String jwtToken = JWT.create()
                .withExpiresAt(Instant.now().plusSeconds(500))
                .withClaim("name", "alice")
                .withClaim("age", 18).sign(algorithm);

        JWTVerifier jwtVerifier = JWT.require(algorithm).build();

        DecodedJWT decodedJWT = jwtVerifier.verify(jwtToken);

        String payload = decodedJWT.getPayload();
        byte[] decode = Base64.getDecoder().decode(payload);
        System.out.println(new String(decode));
    }

}
