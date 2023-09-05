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
package zk.rgw.dashboard.web.bean.entity;

import java.time.Instant;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.BsonType;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonRepresentation;

import zk.rgw.dashboard.framework.mongodb.Document;
import zk.rgw.dashboard.framework.mongodb.Index;

/**
 * 维护访问日志归档进度信息，latestArchiveTime应该是一个整分钟，存在的值表示所表示的那一分钟的时间有归档
 */
@Getter
@Setter
@Document(collection = "ArchiveProgress")
@Index(name = "ArchiveProgress-envIdAndLatestArchiveTime", unique = true, def = "{\"envIdAndLatestArchiveTime\": 1}", expireSeconds = 3600 * 24 * 2)
@NoArgsConstructor
public class ArchiveProgress {

    @BsonId
    @BsonRepresentation(BsonType.OBJECT_ID)
    private String id;

    /**
     * 环境和最后归档时间记录在一个字段
     * 因为mongodb的TTL index不支持多字段联合索引
     */
    private String envIdAndLatestArchiveTime;

    public ArchiveProgress(String envId, Instant latestArchiveTime) {
        this.envIdAndLatestArchiveTime = envId + "_" + latestArchiveTime.toString();
    }

}
