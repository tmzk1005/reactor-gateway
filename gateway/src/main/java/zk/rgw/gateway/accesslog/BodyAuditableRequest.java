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
package zk.rgw.gateway.accesslog;

import lombok.NonNull;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.server.HttpServerRequest;

import zk.rgw.http.exchange.HttpServerRequestDecorator;

public class BodyAuditableRequest extends HttpServerRequestDecorator {

    private final BytesCollector bytesCollector;

    public BodyAuditableRequest(HttpServerRequest decorator, long bodyLimit) {
        super(decorator);
        this.bytesCollector = new BytesCollector(bodyLimit);
    }

    @Override
    public @NonNull ByteBufFlux receive() {
        return ByteBufFlux.fromInbound(super.receive().doOnNext(this.bytesCollector::append));
    }

    public byte[] getAuditBody() {
        return this.bytesCollector.allBytes();
    }

    public long getBodySize() {
        return bytesCollector.getSize();
    }

}