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
import zk.rgw.dashboard.framework.xo.BaseAuditableEntity;
import zk.rgw.dashboard.web.bean.dto.EnvironmentDto;

@Getter
@Setter
@Document(collection = "Environment")
@Index(name = "EnvironmentIndex-name", unique = true, def = "{\"name\": 1}")
public class Environment extends BaseAuditableEntity<EnvironmentDto> {

    @BsonId
    @BsonRepresentation(BsonType.OBJECT_ID)
    private String id;

    private String name;

    @Override
    @SuppressWarnings("unchecked")
    public Environment initFromDto(EnvironmentDto dto) {
        return this;
    }
}
