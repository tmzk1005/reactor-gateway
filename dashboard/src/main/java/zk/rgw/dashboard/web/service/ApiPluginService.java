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
package zk.rgw.dashboard.web.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import zk.rgw.common.definition.PluginInstanceDefinition;
import zk.rgw.dashboard.framework.security.HasRole;
import zk.rgw.dashboard.framework.security.Role;
import zk.rgw.dashboard.web.bean.dto.ApiPluginDto;
import zk.rgw.dashboard.web.bean.entity.ApiPlugin;

public interface ApiPluginService {

    @HasRole(Role.SYSTEM_ADMIN)
    Mono<Void> installPlugin(ApiPluginDto apiPluginDto);

    Flux<ApiPlugin> getAllPlugins();

    Mono<Boolean> checkPluginDefinition(PluginInstanceDefinition definition);

}
