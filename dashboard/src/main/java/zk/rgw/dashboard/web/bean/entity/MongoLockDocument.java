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
import zk.rgw.dashboard.framework.mongodb.Index;

@Getter
@Setter
@NoArgsConstructor
@Index(name = "MongoLockDocument-lockName", unique = true, def = "{\"lockName\": 1}", expireSeconds = 1800)
@Document(collection = "MongoLock")
public class MongoLockDocument {

    @BsonId
    @BsonRepresentation(BsonType.OBJECT_ID)
    private String id;

    private String lockName;

    public MongoLockDocument(String lockName) {
        this.lockName = lockName;
    }

}
