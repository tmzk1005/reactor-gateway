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
package zk.rgw.dashboard.web.bean;

import java.time.Instant;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.bson.codecs.pojo.annotations.BsonIgnore;

import zk.rgw.common.definition.RouteDefinition;
import zk.rgw.common.util.JsonUtil;

@Getter
@Setter
public class RouteDefinitionPublishSnapshot {

    private String publisherId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = JsonUtil.DEFAULT_DATE_TIME_PATTERN)
    private Instant lastModifiedDate;

    private ApiPublishStatus publishStatus = ApiPublishStatus.UNPUBLISHED;

    private RouteDefinition routeDefinition;

    /**
     * 操作序列号，多节点全局唯一且递增，用来实现和网关节点严格增量同步路由信息
     */
    private long opSeq;

    @JsonIgnore
    @BsonIgnore
    public boolean isStatusPublished() {
        return Objects.equals(ApiPublishStatus.PUBLISHED, publishStatus);
    }

    @JsonIgnore
    @BsonIgnore
    public boolean isStatusUnpublished() {
        return Objects.equals(ApiPublishStatus.UNPUBLISHED, publishStatus);
    }

}
