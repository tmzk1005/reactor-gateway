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

package zk.rgw.dashboard.web.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import zk.rgw.common.definition.IdRouteDefinition;
import zk.rgw.common.event.impl.RouteDefinitionEvent;

@Getter
@Setter
@NoArgsConstructor
public class ApiPublishingEvent extends RouteDefinitionEvent {

    private String envId;

    public ApiPublishingEvent(IdRouteDefinition routeDefinition, boolean isAdd, String envId) {
        super(routeDefinition, isAdd);
        this.envId = envId;
    }

}
