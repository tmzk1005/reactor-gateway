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

import reactor.core.publisher.Mono;

import zk.rgw.dashboard.framework.annotation.Controller;
import zk.rgw.dashboard.framework.annotation.RequestBody;
import zk.rgw.dashboard.framework.annotation.RequestMapping;
import zk.rgw.dashboard.framework.annotation.RequestParam;
import zk.rgw.dashboard.framework.validate.PageNum;
import zk.rgw.dashboard.framework.validate.PageSize;
import zk.rgw.dashboard.web.bean.PageData;
import zk.rgw.dashboard.web.bean.dto.AppDto;
import zk.rgw.dashboard.web.bean.vo.AppVo;
import zk.rgw.dashboard.web.service.AppService;
import zk.rgw.dashboard.web.service.factory.ServiceFactory;

@Controller("app")
public class AppController {

    private final AppService appService = ServiceFactory.get(AppService.class);

    @RequestMapping(method = RequestMapping.Method.POST)
    public Mono<AppVo> createApp(@RequestBody AppDto appDto) {
        return appService.createApp(appDto).map(new AppVo()::initFromPo);
    }

    @RequestMapping
    public Mono<PageData<AppVo>> listApps(
            @PageNum @RequestParam(name = "pageNum", required = false, defaultValue = "1") int pageNum,
            @PageSize @RequestParam(name = "pageSize", required = false, defaultValue = "10") int pageSize
    ) {
        return appService.listApps(pageNum, pageSize).map(page -> page.map(app -> new AppVo().initFromPo(app)));
    }

}
