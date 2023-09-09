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

package zk.rgw.plugin.api;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

public interface Exchange extends AttributesHolder {

    HttpServerRequest getRequest();

    HttpServerResponse getResponse();

    interface Builder {

        Builder request(HttpServerRequest request);

        Builder response(HttpServerResponse response);

        Exchange build();

    }

    Builder mutate();

    default void setAuditInfo(String key, Object value) {
        String auditInfoKey = "__audit_info__";
        Map<String, Object> auditInfo = getAttribute(auditInfoKey);
        if (Objects.isNull(auditInfo)) {
            auditInfo = new HashMap<>();
            getAttributes().put(auditInfoKey, auditInfo);
        }

        auditInfo.put(key, value);
    }

    default Map<String, Object> getAuditInfo() {
        String auditInfoKey = "__audit_info__";
        return getAttributeOrDefault(auditInfoKey, new HashMap<>());
    }

}
