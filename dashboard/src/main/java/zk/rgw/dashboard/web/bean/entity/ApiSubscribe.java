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
import lombok.Setter;
import org.bson.BsonType;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonRepresentation;

import zk.rgw.dashboard.framework.mongodb.Document;
import zk.rgw.dashboard.framework.mongodb.DocumentReference;
import zk.rgw.dashboard.framework.xo.BaseAuditableEntity;
import zk.rgw.dashboard.framework.xo.NoDto;
import zk.rgw.dashboard.framework.xo.Po;

@Getter
@Setter
@Document
public class ApiSubscribe extends BaseAuditableEntity<NoDto> {

    @BsonId
    @BsonRepresentation(BsonType.OBJECT_ID)
    private String id;

    @DocumentReference
    private Api api;

    @DocumentReference
    private App app;

    @DocumentReference
    private User user;

    @DocumentReference
    private Organization appOrganization;

    @DocumentReference
    private Organization apiOrganization;

    private Instant applyTime;

    private Instant handleTime;

    private State state = State.CREATED;

    @Override
    public <P extends Po<NoDto>> P initFromDto(NoDto dto) {
        throw new UnsupportedOperationException();
    }

    public enum State {

        CREATED,
        PERMITTED,
        REJECTED

    }

}
