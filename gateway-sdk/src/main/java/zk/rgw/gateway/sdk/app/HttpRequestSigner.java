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

package zk.rgw.gateway.sdk.app;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import zk.rgw.gateway.sdk.util.Signer;

/**
 * 对Http请求信息签名
 */
public class HttpRequestSigner {

    private final String key;

    private final String secret;

    public HttpRequestSigner(String key, String secret) {
        this.key = key;
        this.secret = secret;
    }

    public AppAuthInfo signRequest(String method, String uriPath, Map<String, String> headers, Object body, Date signTime)
            throws NoSuchAlgorithmException, InvalidKeyException, IOException {
        Signer signer = new Signer(this.secret);
        String canonicalString = convertRequestToCanonicalString(method, uriPath, headers);
        if (Objects.isNull(signTime)) {
            signTime = new Date();
        }
        signer.update(canonicalString)
                .update(key)
                .update(secret)
                .update(AppAuthInfo.formatSignTime(signTime));

        if (Objects.nonNull(body)) {
            if (body instanceof InputStream inputStream) {
                byte[] buffer = new byte[1024];
                int read;
                while ((read = inputStream.read(buffer)) > -1) {
                    signer.update(buffer, 0, read);
                }
            } else if (body instanceof String bodyStr) {
                signer.update(bodyStr);
            } else if (body instanceof byte[] bytes) {
                signer.update(bytes);
            } else {
                throw new IllegalArgumentException("body should be input stream or string or byte array");
            }
        }

        String signature = signer.finishUpdate();
        List<String> headerNames = new ArrayList<>(headers.keySet());
        return new AppAuthInfo(key, signTime, headerNames, signature);
    }

    public AppAuthInfo signRequest(String method, String uri, Map<String, String> headers, Object body)
            throws NoSuchAlgorithmException, InvalidKeyException, IOException {
        return signRequest(method, uri, headers, body, null);
    }

    private static String convertRequestToCanonicalString(String method, String uri, Map<String, String> headers) {
        StringBuilder sb = new StringBuilder();
        sb.append(method.toUpperCase()).append('\n');
        sb.append(uri).append('\n');

        if (Objects.nonNull(headers) && !headers.isEmpty()) {
            List<String> headerNames = new ArrayList<>(headers.keySet());
            headerNames.sort(String::compareTo);
            for (String headerName : headerNames) {
                sb.append(headerName).append(":").append(headers.get(headerName));
            }
        }
        return sb.toString();
    }

}
