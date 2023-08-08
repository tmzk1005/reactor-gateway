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
package zk.rgw.dashboard.web.bean.vo;

import java.util.Objects;

import lombok.Getter;
import lombok.Setter;

import zk.rgw.dashboard.framework.xo.Vo;
import zk.rgw.dashboard.web.bean.entity.App;

@Getter
@Setter
public class AppVo extends TimeAuditableVo implements Vo<App> {

    private String id;

    private String name;

    private String organizationId;

    private String description;

    private String key;

    private String secret;

    @SuppressWarnings("unchecked")
    @Override
    public AppVo initFromPo(App app) {
        this.id = app.getId();
        this.name = app.getName();
        if (Objects.nonNull(app.getOrganization())) {
            this.organizationId = app.getOrganization().getId();
        }
        this.description = app.getDescription();
        this.key = app.getKey();
        this.secret = app.getSecret();
        return this;
    }

}
