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

import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.netty.handler.codec.http.QueryStringDecoder;

import zk.rgw.plugin.api.Exchange;

public class ExchangeUtil {

    public static final String QUERY_PARAMS = qualify("queryParams");

    public static final String PATH_PARAMS = qualify("pathParams");

    public static final String ENVIRONMENT_VARS = qualify("environmentVars");

    private ExchangeUtil() {
    }

    private static String qualify(String attr) {
        return ExchangeUtil.class.getName() + "." + attr;
    }

    public static Map<String, List<String>> getQueryParams(Exchange exchange) {
        Map<String, List<String>> params = exchange.getAttribute(QUERY_PARAMS);
        if (Objects.isNull(params)) {
            params = new QueryStringDecoder(exchange.getRequest().uri()).parameters();
            exchange.getAttributes().put(QUERY_PARAMS, params);
        }
        return params;
    }

    public static Map<String, String> getEnvironment(Exchange exchange) {
        return exchange.getAttributeOrDefault(ENVIRONMENT_VARS, Map.of());
    }

}
