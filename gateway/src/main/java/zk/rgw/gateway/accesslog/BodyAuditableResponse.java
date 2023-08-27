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

import io.netty.buffer.ByteBuf;
import lombok.NonNull;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.netty.NettyOutbound;
import reactor.netty.http.server.HttpServerResponse;

import zk.rgw.http.exchange.HttpServerResponseDecorator;

public class BodyAuditableResponse extends HttpServerResponseDecorator {

    private final BytesCollector bytesCollector = new BytesCollector();

    public BodyAuditableResponse(HttpServerResponse delegator) {
        super(delegator);
    }

    @Override
    public @NonNull NettyOutbound send(@NonNull Publisher<? extends ByteBuf> dataStream) {
        return super.send(Flux.from(dataStream).doOnNext(this.bytesCollector::append));
    }

    public byte[] getAuditBody() {
        return this.bytesCollector.allBytes();
    }

}
