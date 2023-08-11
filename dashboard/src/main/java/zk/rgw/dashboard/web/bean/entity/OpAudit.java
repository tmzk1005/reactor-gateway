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
import zk.rgw.dashboard.framework.xo.NoDtoPo;

@Getter
@Setter
@Document
public class OpAudit extends NoDtoPo {

    @BsonId
    @BsonRepresentation(BsonType.OBJECT_ID)
    private String id;

    @DocumentReference
    private User user;

    private Instant opTime;

    private boolean succeed;

    private Action action;

    private Object moreInfo;

    public enum Action {
        API_CREATE,
        API_UPDATE,
        API_PUBLISH,
        API_UNPUBLISH,
        API_DELETE,

        ORG_CREATE,
        ORG_UPDATE,
        ORG_DELETE,

        ENV_CREATE,
        ENV_UPDATE,
        EVN_DELETE,

        ENV_VARS_SET,

        SUB_APPLY,
        SUB_APPROVE,
        SUB_REJECT,

        USER_CREATE,
        USER_UPDATE,
        USER_PASSWORD,
        USER_LOGIN,
        USER_ENABLE,
        USER_DISABLE,
        USER_DELETE

    }

}
