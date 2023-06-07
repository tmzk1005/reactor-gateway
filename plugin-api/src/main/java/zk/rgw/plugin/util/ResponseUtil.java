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

package zk.rgw.plugin.util;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerResponse;

public class ResponseUtil {

    private ResponseUtil() {
    }

    public static Mono<Void> send(HttpServerResponse response, String message) {
        return send(response, HttpResponseStatus.OK, Shuck.CODE_OK, message);
    }

    public static Mono<Void> send(HttpServerResponse response, int code, String message) {
        return send(response, HttpResponseStatus.OK, code, message);
    }

    public static Mono<Void> send(HttpServerResponse response, HttpResponseStatus httpResponseStatus, int code, String message) {
        String json = Shuck.jsonOf(code, message);
        return send(response, httpResponseStatus, HttpHeaderValues.APPLICATION_JSON.toString(), json);
    }

    public static Mono<Void> send(HttpServerResponse response, HttpResponseStatus httpResponseStatus, String contentType, String content) {
        return response.status(httpResponseStatus)
                .header(HttpHeaderNames.CONTENT_TYPE, contentType)
                .sendString(Mono.just(content))
                .then();
    }

    public static Mono<Void> sendRawJson(HttpServerResponse response, String jsonMessage) {
        return send(response, HttpResponseStatus.OK, HttpHeaderValues.APPLICATION_JSON.toString(), jsonMessage);
    }

    public static Mono<Void> sendStatus(HttpServerResponse response, HttpResponseStatus status) {
        return send(response, status, status.code(), status.reasonPhrase());
    }

    public static Mono<Void> sendStatus(HttpServerResponse response, HttpResponseStatus status, String message) {
        return send(response, status, status.code(), message);
    }

    public static Mono<Void> sendOk(HttpServerResponse response) {
        return sendStatus(response, HttpResponseStatus.OK);
    }

    public static Mono<Void> sendError(HttpServerResponse response) {
        return sendStatus(response, HttpResponseStatus.INTERNAL_SERVER_ERROR);
    }

    public static Mono<Void> sendNotFound(HttpServerResponse response) {
        return sendStatus(response, HttpResponseStatus.NOT_FOUND);
    }

}
