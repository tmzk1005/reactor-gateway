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
import zk.rgw.dashboard.framework.validate.PageNum;
import zk.rgw.dashboard.framework.validate.PageSize;
import zk.rgw.dashboard.web.bean.PageData;
import zk.rgw.dashboard.web.bean.vo.AccessLogVo;
import zk.rgw.dashboard.web.service.AccessLogService;
import zk.rgw.dashboard.web.service.factory.ServiceFactory;

@Controller("access-log")
public class AccessLogController {

    private final AccessLogService accessLogService = ServiceFactory.get(AccessLogService.class);

    @SuppressWarnings("java:S107")
    @RequestMapping
    public Mono<PageData<AccessLogVo>> listAccessLogs(
            @RequestParam(name = "asSubscriber", required = false, defaultValue = "false") boolean asSubscriber,
            @PageNum @RequestParam(name = "pageNum", required = false, defaultValue = "1") int pageNum,
            @PageSize @RequestParam(name = "pageSize", required = false, defaultValue = "10") int pageSize,
            @RequestParam(name = "envId") String envId,
            @RequestParam(name = "requestId", required = false) String requestId,
            @RequestParam(name = "apiName", required = false) String apiNameOrId,
            @RequestParam(name = "clientIp", required = false) String clientIp,
            @RequestParam(name = "clientName", required = false) String clientNameOrId,
            @RequestParam(name = "responseStatus", required = false) Integer[] responseStatuses,
            @RequestParam(name = "timeCostMillis", required = false) Integer minTimeCostMillis,
            @RequestParam(name = "minTimeMillis", required = false) Long minTimeMillis,
            @RequestParam(name = "maxTimeMillis", required = false) Long maxTimeMillis
    ) {
        return accessLogService.searchAccessLogs(
                asSubscriber,
                envId, pageNum, pageSize,
                requestId, apiNameOrId, clientIp, clientNameOrId,
                responseStatuses, minTimeCostMillis, minTimeMillis, maxTimeMillis
        );
    }

}
