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

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

import zk.rgw.dashboard.framework.mongodb.Document;
import zk.rgw.dashboard.framework.mongodb.DocumentReference;
import zk.rgw.dashboard.framework.xo.BaseAuditableEntity;
import zk.rgw.dashboard.framework.xo.NoDto;

@Getter
@Setter
@Document(collection = "EnvBinding")
public class EnvBinding extends BaseAuditableEntity<NoDto> {

    private String id;

    @DocumentReference
    private Environment environment;

    @DocumentReference
    private Organization organization;

    private Map<String, String> variables = new HashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public EnvBinding initFromDto(NoDto dto) {
        throw new UnsupportedOperationException();
    }

}
