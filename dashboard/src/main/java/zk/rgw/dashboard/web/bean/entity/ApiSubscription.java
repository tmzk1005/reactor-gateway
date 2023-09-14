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

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.BsonType;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonRepresentation;

import zk.rgw.dashboard.framework.mongodb.Document;
import zk.rgw.dashboard.framework.mongodb.DocumentReference;
import zk.rgw.dashboard.framework.mongodb.Index;
import zk.rgw.dashboard.framework.xo.BaseAuditableEntity;
import zk.rgw.dashboard.framework.xo.NoDto;
import zk.rgw.dashboard.framework.xo.Po;

@Getter
@Setter
@Document
@Index(name = "ApiSubscriptionIndex-api", unique = true, def = "{\"api\": 1}")
@NoArgsConstructor
public class ApiSubscription extends BaseAuditableEntity<NoDto> {

    @BsonId
    @BsonRepresentation(BsonType.OBJECT_ID)
    private String id;

    @DocumentReference
    private Api api;

    @DocumentReference
    private List<App> apps;

    private long opSeq;

    @Override
    public <P extends Po<NoDto>> P initFromDto(NoDto dto) {
        throw new UnsupportedOperationException();
    }

}
