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
package zk.rgw.dashboard.global;

import java.util.HashMap;
import java.util.Map;

import zk.rgw.common.event.EventPublisher;
import zk.rgw.common.event.EventPublisherImpl;
import zk.rgw.common.event.RgwEvent;
import zk.rgw.dashboard.web.event.listener.DashboardEventListener;

public class GlobalSingletons {

    private GlobalSingletons() {
    }

    private static final Map<Class<?>, Object> INSTANCES = new HashMap<>();

    static {
        EventPublisher<RgwEvent> eventPublisher = new EventPublisherImpl<>();
        eventPublisher.registerListener(new DashboardEventListener());
        INSTANCES.put(EventPublisher.class, eventPublisher);
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(Class<T> serviceClass) {
        return (T) INSTANCES.get(serviceClass);
    }

}
