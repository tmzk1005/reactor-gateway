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
package zk.rgw.dashboard.web.bean;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import zk.rgw.dashboard.web.bean.vo.SimpleEnvironmentVo;

@Getter
@Setter
@NoArgsConstructor
public class RouteDefinitionPublishSnapshotDisplay extends RouteDefinitionPublishSnapshot {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String publisherName;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private SimpleEnvironmentVo env;

    @JsonProperty
    public String getPublishStatusDisplay() {
        if (getPublishStatus() == ApiPublishStatus.PUBLISHED) {
            return "已发布";
        } else if (getPublishStatus() == ApiPublishStatus.UNPUBLISHED) {
            return "未发布";
        } else if (getPublishStatus() == ApiPublishStatus.NOT_UPDATED) {
            return "未更新";
        }
        return "";
    }

    public RouteDefinitionPublishSnapshotDisplay(RouteDefinitionPublishSnapshot snapshot) {
        this.setPublisherId(snapshot.getPublisherId());
        this.setRouteDefinition(snapshot.getRouteDefinition());
        this.setPublishStatus(snapshot.getPublishStatus());
        this.setLastModifiedDate(snapshot.getLastModifiedDate());
    }

}
