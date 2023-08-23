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

import reactor.core.publisher.Mono;

import zk.rgw.dashboard.framework.security.HasRole;
import zk.rgw.dashboard.framework.security.Role;
import zk.rgw.dashboard.web.bean.PageData;
import zk.rgw.dashboard.web.bean.dto.ApiDto;
import zk.rgw.dashboard.web.bean.entity.Api;
import zk.rgw.dashboard.web.bean.vo.ApiVo;
import zk.rgw.dashboard.web.bean.vo.ReleasedApiVo;

public interface ApiService {

    @HasRole(Role.NORMAL_USER)
    Mono<Api> createApi(ApiDto apiDto);

    Mono<PageData<Api>> listApis(int pageNum, int pageSize);

    Mono<ApiVo> getApiDetailById(String apiId);

    @HasRole(Role.NORMAL_USER)
    Mono<Api> publishApi(String apiId, String envId);

    @HasRole(Role.NORMAL_USER)
    Mono<Api> unpublishApi(String apiId, String envId);

    Mono<PageData<ReleasedApiVo>> listReleasedApiVo(String envId, String searchText, int pageNum, int pageSize);

}
