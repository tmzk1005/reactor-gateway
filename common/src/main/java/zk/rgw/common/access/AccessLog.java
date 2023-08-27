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
package zk.rgw.common.access;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccessLog {

    private String requestId;

    private long reqTimestamp;

    private long respTimestamp;

    private int millisCost;

    private String apiId;

    private ClientInfo clientInfo;

    private RequestInfo requestInfo;

    private ResponseInfo responseInfo;

    private Map<String, Object> extraInfo;

    @Getter
    @Setter
    public static class ClientInfo {
        private String ip;
        private String appId;
    }

    @Getter
    @Setter
    public static class RequestInfo {
        private String uri;
        private String method;
        private Map<String, List<String>> headers;
        private String body;
        private boolean bodyBase64;
    }

    @Getter
    @Setter
    public static class ResponseInfo {
        private int code;
        private Map<String, List<String>> headers;
        private String body;
        private boolean bodyBase64;
    }

}
