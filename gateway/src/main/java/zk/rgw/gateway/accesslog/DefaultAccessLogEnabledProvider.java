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

import java.util.HashMap;
import java.util.Map;

public class DefaultAccessLogEnabledProvider implements AccessLogEnabledProvider {

    private final Map<String, Boolean> status = new HashMap<>();

    @Override
    public boolean isAccessLogEnabled(String routeId) {
        return Boolean.TRUE.equals(status.get(routeId));
    }

    @Override
    public void enable(String routeId) {
        status.put(routeId, true);
    }

    @Override
    public void disable(String routeId) {
        status.remove(routeId);
    }

}
