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
package zk.rgw.common.event;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Slf4j
public class EventPublisherImpl<E extends RgwEvent> implements EventPublisher<E> {

    private final Sinks.Many<E> sink;

    private final Flux<E> eventStream;

    public EventPublisherImpl() {
        this.sink = Sinks.many().multicast().onBackpressureBuffer();
        this.eventStream = this.sink.asFlux();
    }

    @Override
    public synchronized void publishEvent(E rgwEvent) {
        Sinks.EmitResult emitResult = sink.tryEmitNext(rgwEvent);
        if (emitResult.isFailure()) {
            log.error("Failed to publish an event, emit result is {}, event object is {}", emitResult, rgwEvent);
        }
    }

    @Override
    public synchronized void registerListener(RgwEventListener<E> listener) {
        this.eventStream.subscribe(listener::onEvent);
    }

}
