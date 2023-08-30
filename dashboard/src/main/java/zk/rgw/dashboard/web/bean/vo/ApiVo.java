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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import zk.rgw.common.definition.RouteDefinition;
import zk.rgw.common.util.JsonUtil;
import zk.rgw.dashboard.framework.xo.Vo;
import zk.rgw.dashboard.web.bean.RouteDefinitionPublishSnapshot;
import zk.rgw.dashboard.web.bean.RouteDefinitionPublishSnapshotDisplay;
import zk.rgw.dashboard.web.bean.entity.Api;

@Getter
@Setter
public class ApiVo extends AuditableVo implements Vo<Api> {

    private String id;

    private SimpleOrganizationVo organization;

    private String name;

    private String description;

    private Set<String> tags;

    private String mdDoc;

    private RouteDefinition routeDefinition;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = JsonUtil.DEFAULT_DATE_TIME_PATTERN)
    private Instant routeDefinitionLastModifiedDate;

    private Map<String, RouteDefinitionPublishSnapshotDisplay> publishSnapshots;

    @SuppressWarnings("unchecked")
    @Override
    public ApiVo initFromPo(Api poInstance) {
        this.id = poInstance.getId();
        this.organization = new SimpleOrganizationVo().initFromPo(poInstance.getOrganization());
        this.name = poInstance.getName();
        this.description = poInstance.getDescription();
        this.tags = poInstance.getTags();
        this.mdDoc = poInstance.getMdDoc();
        this.routeDefinition = poInstance.getRouteDefinition();
        this.routeDefinitionLastModifiedDate = poInstance.getRouteDefinitionLastModifiedDate();

        this.publishSnapshots = new HashMap<>();
        for (Map.Entry<String, RouteDefinitionPublishSnapshot> entry : poInstance.getPublishSnapshots().entrySet()) {
            this.publishSnapshots.put(entry.getKey(), new RouteDefinitionPublishSnapshotDisplay(entry.getValue()));
        }

        this.copyOperatorAuditableInfo(poInstance);
        this.copyTimeAuditInfo(poInstance);
        return this;
    }

}
