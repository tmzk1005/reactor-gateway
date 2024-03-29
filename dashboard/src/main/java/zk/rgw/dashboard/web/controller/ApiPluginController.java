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
package zk.rgw.dashboard.web.controller;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import zk.rgw.dashboard.framework.annotation.Controller;
import zk.rgw.dashboard.framework.annotation.RequestBody;
import zk.rgw.dashboard.framework.annotation.RequestMapping;
import zk.rgw.dashboard.web.bean.dto.ApiPluginDto;
import zk.rgw.dashboard.web.bean.vo.ApiPluginVo;
import zk.rgw.dashboard.web.service.ApiPluginService;
import zk.rgw.dashboard.web.service.factory.ServiceFactory;

@Controller("plugin")
public class ApiPluginController {

    private final ApiPluginService apiPluginService = ServiceFactory.get(ApiPluginService.class);

    @RequestMapping(method = RequestMapping.Method.POST)
    public Mono<Void> installPlugin(@RequestBody ApiPluginDto apiPluginDto) {
        return apiPluginService.installPlugin(apiPluginDto);
    }

    @RequestMapping(method = RequestMapping.Method.GET)
    public Flux<ApiPluginVo> getAllPlugins() {
        return apiPluginService.getAllPlugins().map(poInstance -> new ApiPluginVo().initFromPo(poInstance));
    }

}
