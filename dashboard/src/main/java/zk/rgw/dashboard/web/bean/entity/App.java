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

import java.util.Objects;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import org.bson.BsonType;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonRepresentation;

import zk.rgw.dashboard.framework.mongodb.Document;
import zk.rgw.dashboard.framework.mongodb.DocumentReference;
import zk.rgw.dashboard.framework.mongodb.Index;
import zk.rgw.dashboard.framework.xo.BaseAuditableEntity;
import zk.rgw.dashboard.web.bean.dto.AppDto;

@Getter
@Setter
@Document
@Index(name = "AppIndex-name", unique = true, def = "{\"name\": 1}")
public class App extends BaseAuditableEntity<AppDto> {

    @BsonId
    @BsonRepresentation(BsonType.OBJECT_ID)
    private String id;

    @DocumentReference
    private Organization organization;

    private String name;

    private String description;

    private String key;

    private String secret;

    @SuppressWarnings("unchecked")
    @Override
    public App initFromDto(AppDto dto) {
        this.name = dto.getName();
        this.description = dto.getDescription();
        this.organization = new Organization();
        this.organization.setId(dto.getOrganizationId());
        if (Objects.isNull(this.key)) {
            this.key = UUID.randomUUID().toString();
        }
        if (Objects.isNull(this.secret)) {
            this.secret = UUID.randomUUID().toString();
        }
        return this;
    }

}
