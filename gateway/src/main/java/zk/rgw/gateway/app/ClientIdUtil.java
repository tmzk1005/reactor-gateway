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
package zk.rgw.gateway.app;

import zk.rgw.plugin.api.Exchange;

public class ClientIdUtil {

    private static final String KEY = "__app_auth_succeed_client_id__";

    private ClientIdUtil() {
    }

    public static void setAppAuthSucceedClientId(Exchange exchange, String clientId) {
        exchange.getAttributes().put(KEY, clientId);
    }

    public static String getAppAuthSucceedClientId(Exchange exchange) {
        return exchange.getAttribute(KEY);
    }

}
