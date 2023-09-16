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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import io.netty.handler.codec.http.QueryStringDecoder;

import zk.rgw.plugin.api.Exchange;

public class ExchangeUtil {

    public static final String QUERY_PARAMS = qualify("queryParams");

    public static final String PATH_PARAMS = qualify("pathParams");

    public static final String ENVIRONMENT_VARS = qualify("environmentVars");

    public static final String AUDIT_INFO = qualify("auditInfo");

    public static final String AUDIT_TAG = qualify("auditTag");

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

    public static void setAuditInfo(Exchange exchange, String key, Object value) {
        Map<String, Object> auditInfo = getAuditInfo(exchange);
        auditInfo.put(key, value);
    }

    public static Map<String, Object> getAuditInfo(Exchange exchange) {
        Map<String, Object> auditInfo = exchange.getAttribute(AUDIT_INFO);
        if (Objects.isNull(auditInfo)) {
            auditInfo = new HashMap<>(4);
            exchange.getAttributes().put(AUDIT_INFO, new HashMap<>());
        }
        return auditInfo;
    }

    public static void addAuditTag(Exchange exchange, String tag) {
        Set<String> tags = getAuditTags(exchange);
        tags.add(tag);
    }

    public static void removeAuditTag(Exchange exchange, String tag) {
        Set<String> tags = getAuditTags(exchange);
        tags.remove(tag);
    }

    public static Set<String> getAuditTags(Exchange exchange) {
        Set<String> tags = exchange.getAttribute(AUDIT_TAG);
        if (Objects.isNull(tags)) {
            tags = new HashSet<>(4);
            exchange.getAttributes().put(AUDIT_TAG, tags);
        }
        return tags;
    }

}
