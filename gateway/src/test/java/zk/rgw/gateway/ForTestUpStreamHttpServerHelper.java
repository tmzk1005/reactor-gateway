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

package zk.rgw.gateway;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import com.sun.net.httpserver.HttpServer;
import io.netty.handler.codec.http.HttpHeaderNames;

import zk.rgw.common.exception.RgwRuntimeException;

public class ForTestUpStreamHttpServerHelper {

    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 6000;

    private ForTestUpStreamHttpServerHelper() {
    }

    public static HttpServer create(String path, String respContentType, int respCode, String respBody) {
        HttpServer httpServer;
        try {
            httpServer = HttpServer.create(new InetSocketAddress(DEFAULT_HOST, DEFAULT_PORT), 2);
        } catch (IOException ioException) {
            throw new RgwRuntimeException(ioException);
        }
        httpServer.createContext(path, exchange -> {
            final OutputStream responseBody = exchange.getResponseBody();
            if (Objects.nonNull(respContentType)) {
                exchange.getResponseHeaders().add(HttpHeaderNames.CONTENT_TYPE.toString(), respContentType);
            }
            final byte[] bytes = respBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(respCode, bytes.length);
            responseBody.write(bytes);
            responseBody.flush();
            responseBody.close();
        });
        return httpServer;
    }

}
