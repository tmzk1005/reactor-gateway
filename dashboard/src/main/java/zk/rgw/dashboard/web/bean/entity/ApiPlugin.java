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

import lombok.Getter;
import lombok.Setter;
import org.bson.BsonType;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonRepresentation;

import zk.rgw.dashboard.framework.mongodb.Document;
import zk.rgw.dashboard.framework.mongodb.Index;
import zk.rgw.dashboard.framework.validate.NotBlank;
import zk.rgw.dashboard.framework.validate.Pattern;
import zk.rgw.dashboard.framework.validate.Size;
import zk.rgw.dashboard.framework.xo.BaseAuditableEntity;
import zk.rgw.dashboard.utils.Patters;
import zk.rgw.dashboard.web.bean.dto.ApiPluginDto;

@Getter
@Setter
@Document
@Index(name = "ApiPlugin-name-version", unique = true, def = "{\"name\": 1, \"version\": 1}")
public class ApiPlugin extends BaseAuditableEntity<ApiPluginDto> {

    @BsonId
    @BsonRepresentation(BsonType.OBJECT_ID)
    private String id;

    @NotBlank(message = "插件名称不能为空")
    @Size(min = 3, max = 32, message = "插件名称不能超过32个字符长度，且最少需要3个字符长度")
    @Pattern(regexp = Patters.IDENTIFIER_ZH, message = "API名称只能包含字母，数字和下划线以及中文字符")
    private String name;

    private String fullClassName;

    private String version;

    private String jsonSchema;

    private boolean builtin;

    private String installerUri;

    @BsonRepresentation(BsonType.OBJECT_ID)
    private String organizationId;

    private boolean tail;

    @SuppressWarnings("unchecked")
    @Override
    public ApiPlugin initFromDto(ApiPluginDto dto) {
        this.name = dto.getName();
        this.fullClassName = dto.getFullClassName();
        this.version = dto.getVersion();
        this.jsonSchema = dto.getJsonSchema();
        this.tail = dto.isTail();

        this.builtin = false;
        return this;
    }

}
