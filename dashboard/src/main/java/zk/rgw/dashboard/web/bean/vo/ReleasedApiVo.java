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

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import zk.rgw.common.util.JsonUtil;
import zk.rgw.dashboard.web.bean.RouteDefinitionPublishSnapshot;
import zk.rgw.dashboard.web.bean.entity.Api;

@Getter
@Setter
@NoArgsConstructor
public class ReleasedApiVo {

    private String id;

    private String name;

    private String description;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = JsonUtil.DEFAULT_DATE_TIME_PATTERN)
    private Instant releaseTime;

    private Set<String> tags;

    private Set<String> methods;

    private String path;

    public ReleasedApiVo(Api api, String envId) {
        this.id = api.getId();
        this.name = api.getName();
        this.description = api.getDescription();
        this.tags = api.getTags();
        Map<String, RouteDefinitionPublishSnapshot> publishSnapshots = api.getPublishSnapshots();
        if (Objects.nonNull(publishSnapshots) && publishSnapshots.containsKey(envId)) {
            this.releaseTime = publishSnapshots.get(envId).getLastModifiedDate();
            this.methods = publishSnapshots.get(envId).getRouteDefinition().getMethods();
            this.path = publishSnapshots.get(envId).getRouteDefinition().getPath();
        }
    }

}
