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
package zk.rgw.dashboard.web.bean.vo;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import org.bson.codecs.pojo.annotations.BsonIgnore;

import zk.rgw.alc.common.AccessLogDocument;
import zk.rgw.common.access.AccessLog;
import zk.rgw.common.util.JsonUtil;

@Getter
@Setter
public class AccessLogVo extends AccessLog {

    private String id;

    private String apiName;

    private String clientAppName;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = JsonUtil.DEFAULT_DATE_TIME_PATTERN)
    @BsonIgnore
    public Instant getReqTimeDisplay() {
        return Instant.ofEpochMilli(getReqTimestamp());
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = JsonUtil.DEFAULT_DATE_TIME_PATTERN)
    @BsonIgnore
    public Instant getRespTimeDisplay() {
        return Instant.ofEpochMilli(getRespTimestamp());
    }

    public static AccessLogVo copyFromAccessLogDocument(AccessLogDocument document) {
        AccessLogVo accessLogVo = new AccessLogVo();
        accessLogVo.setId(document.getId());
        accessLogVo.setRequestId(document.getRequestId());
        accessLogVo.setApiId(document.getApiId());
        accessLogVo.setReqTimestamp(document.getReqTimestamp());
        accessLogVo.setRespTimestamp(document.getRespTimestamp());
        accessLogVo.setMillisCost(document.getMillisCost());
        accessLogVo.setClientInfo(document.getClientInfo());
        accessLogVo.setRequestInfo(document.getRequestInfo());
        accessLogVo.setResponseInfo(document.getResponseInfo());
        accessLogVo.setExtraInfo(document.getExtraInfo());
        accessLogVo.setTags(document.getTags());
        return accessLogVo;
    }

}
