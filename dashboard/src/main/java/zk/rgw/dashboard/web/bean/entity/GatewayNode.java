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
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.BsonType;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonRepresentation;

import zk.rgw.dashboard.framework.mongodb.Document;
import zk.rgw.dashboard.framework.mongodb.DocumentReference;
import zk.rgw.dashboard.framework.mongodb.Index;
import zk.rgw.dashboard.framework.xo.NoDtoPo;

@Getter
@Setter
@Document
@Index(name = "GatewayNode-address", unique = true, def = "{\"address\": 1}")
@NoArgsConstructor
public class GatewayNode extends NoDtoPo {

    @BsonId
    @BsonRepresentation(BsonType.OBJECT_ID)
    private String id;

    @DocumentReference
    private Environment environment;

    private String address;

    private long heartbeat;

    public GatewayNode(String address, Environment environment) {
        this.address = address;
        this.environment = environment;
        this.heartbeat = System.currentTimeMillis();
    }

}
