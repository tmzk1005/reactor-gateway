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
package zk.rgw.plugin.filter.cors;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import reactor.netty.http.server.HttpServerRequest;

import zk.rgw.common.exception.RgwRuntimeException;
import zk.rgw.common.util.UriUtil;

public class CorsUtil {

    private CorsUtil() {

    }

    /**
     * 是否是CORS预检请求
     *
     * @param request http请求
     * @return boolean值： 是否是CORS预检请求
     */
    public static boolean isCorsPreFlightRequest(HttpServerRequest request) {
        final HttpHeaders headers = request.requestHeaders();
        return request.method() == io.netty.handler.codec.http.HttpMethod.OPTIONS
                && headers.contains(HttpHeaderNames.ORIGIN)
                && headers.contains(HttpHeaderNames.ACCESS_CONTROL_REQUEST_METHOD);
    }

    /**
     * 是否是CORS请求
     *
     * @param request http请求
     * @return boolean值：是否是CORS请求
     */
    public static boolean isCorsRequest(HttpServerRequest request) {
        return request.requestHeaders().contains(HttpHeaderNames.ORIGIN) && !isSameOrigin(request);
    }

    public static boolean isSameOrigin(HttpServerRequest request) {
        // 暂时简单实现，只判断有没有ORIGIN头，不判断其值是否真的和当前服务同源。
        String origin = request.requestHeaders().get(HttpHeaderNames.ORIGIN);
        if (Objects.isNull(origin)) {
            return true;
        }
        URI uri;
        try {
            uri = UriUtil.resolveBaseUrl(request);
        } catch (URISyntaxException exception) {
            // 不应该发生，简单抛出RuntimeException
            throw new RgwRuntimeException("Failed to parse base url from request.", exception);
        }
        URI originUri;
        try {
            originUri = new URI(origin);
        } catch (URISyntaxException exception) {
            // 解析为uri失败，当作是null，return true
            return true;
        }

        return uri.getScheme().equals(originUri.getScheme()) &&
                uri.getHost().equals(originUri.getHost()) &&
                getPort(uri.getScheme(), uri.getPort()) == getPort(originUri.getScheme(), originUri.getPort());
    }

    private static int getPort(String scheme, int port) {
        if (port == -1) {
            return UriUtil.getPort(scheme);
        }
        return port;
    }

}
