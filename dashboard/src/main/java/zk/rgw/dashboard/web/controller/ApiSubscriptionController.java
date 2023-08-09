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
import zk.rgw.dashboard.framework.annotation.RequestMapping;
import zk.rgw.dashboard.framework.annotation.RequestParam;
import zk.rgw.dashboard.framework.validate.NotBlank;
import zk.rgw.dashboard.framework.validate.PageNum;
import zk.rgw.dashboard.framework.validate.PageSize;
import zk.rgw.dashboard.web.bean.PageData;
import zk.rgw.dashboard.web.bean.vo.ApiSubscribeVo;
import zk.rgw.dashboard.web.service.SubscriptionService;
import zk.rgw.dashboard.web.service.factory.ServiceFactory;

@Controller("subscription")
public class ApiSubscriptionController {

    private final SubscriptionService subscribeApi = ServiceFactory.get(SubscriptionService.class);

    @RequestMapping(path = "/_subscribe", method = RequestMapping.Method.POST)
    public Mono<Void> subscribe(
            @RequestParam(name = "apiId") @NotBlank(message = "参数apiId不能为空") String apiId,
            @RequestParam(name = "appId") @NotBlank(message = "参数appId不能为空") String appId
    ) {
        return subscribeApi.applySubscribeApi(apiId, appId);
    }

    @RequestMapping()
    public Mono<PageData<ApiSubscribeVo>> mySubscribes(
            @PageNum @RequestParam(name = "pageNum", required = false, defaultValue = "1") int pageNum,
            @PageSize @RequestParam(name = "pageSize", required = false, defaultValue = "10") int pageSize
    ) {
        return subscribeApi.myApiSubscribes(pageNum, pageSize).map(page -> page.map(po -> new ApiSubscribeVo().initFromPo(po)));
    }

}
