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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import org.bson.BsonType;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonRepresentation;

import zk.rgw.common.definition.RouteDefinition;
import zk.rgw.dashboard.framework.mongodb.Document;
import zk.rgw.dashboard.framework.mongodb.DocumentReference;
import zk.rgw.dashboard.framework.mongodb.Index;
import zk.rgw.dashboard.framework.xo.BaseAuditableEntity;
import zk.rgw.dashboard.web.bean.RouteDefinitionPublishSnapshot;
import zk.rgw.dashboard.web.bean.dto.ApiDto;

@Getter
@Setter
@Document
@Index(name = "ApiIndex-name-org", unique = true, def = "{\"name\": 1, \"organization\": 1}")
public class Api extends BaseAuditableEntity<ApiDto> {

    @BsonId
    @BsonRepresentation(BsonType.OBJECT_ID)
    private String id;

    @DocumentReference
    private Organization organization;

    private String name;

    private String description;

    private Set<String> tags;

    private String mdDoc;

    private RouteDefinition routeDefinition;

    private Instant routeDefinitionLastModifiedDate;

    /**
     * 发布到各个环境的快照，Key是环境ID
     */
    private Map<String, RouteDefinitionPublishSnapshot> publishSnapshots = new HashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public Api initFromDto(ApiDto dto) {
        this.name = dto.getName();
        this.description = dto.getDescription();
        this.tags = dto.getTags();
        this.mdDoc = dto.getMdDoc();
        if (!Objects.equals(this.routeDefinition, dto.getRouteDefinition())) {
            this.routeDefinitionLastModifiedDate = Instant.now();
            this.routeDefinition = dto.getRouteDefinition();
        }
        return this;
    }

}
