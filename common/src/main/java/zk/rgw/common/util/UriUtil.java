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
package zk.rgw.common.util;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

import io.netty.handler.codec.http.HttpHeaderNames;
import reactor.netty.http.server.HttpServerRequest;

public class UriUtil {

    private UriUtil() {
    }

    public static URI resolveBaseUrl(HttpServerRequest request) throws URISyntaxException {
        String scheme = request.scheme();
        String host = request.requestHeaders().get(HttpHeaderNames.HOST);

        if (Objects.isNull(host)) {
            InetSocketAddress localAddress = Objects.requireNonNull(request.hostAddress());
            return new URI(scheme, null, localAddress.getHostString(), localAddress.getPort(), null, null, null);
        }
        final int portIndex;
        if (host.startsWith("[")) {
            portIndex = host.indexOf(':', host.indexOf(']'));
        } else {
            portIndex = host.indexOf(':');
        }
        if (portIndex != -1) {
            try {
                return new URI(
                        scheme, null, host.substring(0, portIndex),
                        Integer.parseInt(host.substring(portIndex + 1)), null, null, null
                );
            } catch (NumberFormatException ex) {
                throw new URISyntaxException(host, "Unable to parse port", portIndex);
            }
        }
        return new URI(scheme, host, null, null);
    }

    public static int getPort(String scheme) {
        if ("http".equals(scheme) || "ws".equals(scheme)) {
            return 80;
        } else if ("https".equals(scheme) || "wss".equals(scheme)) {
            return 443;
        }
        return -1;
    }

}
