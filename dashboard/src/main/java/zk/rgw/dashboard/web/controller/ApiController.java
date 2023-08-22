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
import zk.rgw.dashboard.framework.annotation.PathVariable;
import zk.rgw.dashboard.framework.annotation.RequestBody;
import zk.rgw.dashboard.framework.annotation.RequestMapping;
import zk.rgw.dashboard.framework.annotation.RequestParam;
import zk.rgw.dashboard.framework.validate.NotBlank;
import zk.rgw.dashboard.framework.validate.PageNum;
import zk.rgw.dashboard.framework.validate.PageSize;
import zk.rgw.dashboard.web.bean.PageData;
import zk.rgw.dashboard.web.bean.dto.ApiDto;
import zk.rgw.dashboard.web.bean.vo.ApiVo;
import zk.rgw.dashboard.web.service.ApiService;
import zk.rgw.dashboard.web.service.factory.ServiceFactory;

@Controller("api")
public class ApiController {

    private final ApiService apiService = ServiceFactory.get(ApiService.class);

    @RequestMapping(method = RequestMapping.Method.POST)
    public Mono<ApiVo> createApi(@RequestBody ApiDto apiDto) {
        return apiService.createApi(apiDto).map(new ApiVo()::initFromPo);
    }

    @RequestMapping
    public Mono<PageData<ApiVo>> listApis(
            @PageNum @RequestParam(name = "pageNum", required = false, defaultValue = "1") int pageNum,
            @PageSize @RequestParam(name = "pageSize", required = false, defaultValue = "10") int pageSize
    ) {
        return apiService.listApis(pageNum, pageSize).map(page -> page.map(api -> new ApiVo().initFromPo(api)));
    }

    @RequestMapping(path = "/{apiId}")
    public Mono<ApiVo> getApiById(
            @PathVariable("apiId") @NotBlank(message = "参数apiId不能为空") String apiId
    ) {
        return apiService.getApiDetailById(apiId);
    }

    @RequestMapping(path = "/_publish/{apiId}", method = RequestMapping.Method.POST)
    public Mono<ApiVo> publishApi(
            @PathVariable("apiId") @NotBlank(message = "参数apiId不能为空") String apiId,
            @RequestParam(name = "envId") @NotBlank(message = "参数envId不能为空") String envId
    ) {
        return apiService.publishApi(apiId, envId).map(new ApiVo()::initFromPo);
    }

    @RequestMapping(path = "/_unpublish/{apiId}", method = RequestMapping.Method.POST)
    public Mono<ApiVo> unpublishApi(
            @PathVariable("apiId") @NotBlank(message = "参数apiId不能为空") String apiId,
            @RequestParam(name = "envId") @NotBlank(message = "参数envId不能为空") String envId
    ) {
        return apiService.unpublishApi(apiId, envId).map(new ApiVo()::initFromPo);
    }

}
