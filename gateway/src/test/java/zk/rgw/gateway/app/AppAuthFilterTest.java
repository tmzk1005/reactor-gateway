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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import zk.rgw.gateway.sdk.app.AppAuthInfo;
import zk.rgw.gateway.sdk.app.HttpRequestSigner;

@Disabled("暂时只实现开发阶段手工测试验证")
class AppAuthFilterTest {

    @Test
    void test() throws Exception {
        String method = "GET";
        String path = "/test";
        String uri = "http://127.0.0.1:8000" + path;

        String key = "key_change_me";
        String secret = "secret_change_me";

        HttpRequestSigner httpRequestSigner = new HttpRequestSigner(key, secret);
        String token = httpRequestSigner.signRequest(method, path, Map.of(), null).asString();

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(uri))
                .header(AppAuthInfo.APP_AUTH_HEADER_NAME, token)
                .method(method, HttpRequest.BodyPublishers.noBody())
                .build();
        httpClient.send(request, responseInfo -> {
            Assertions.assertEquals(200, responseInfo.statusCode());
            return HttpResponse.BodySubscribers.ofByteArray();
        });
    }

}
