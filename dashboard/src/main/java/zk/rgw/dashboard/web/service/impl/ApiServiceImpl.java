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
package zk.rgw.dashboard.web.service.impl;

import reactor.core.publisher.Mono;

import zk.rgw.dashboard.framework.context.ContextUtil;
import zk.rgw.dashboard.framework.exception.BizException;
import zk.rgw.dashboard.web.bean.dto.ApiDto;
import zk.rgw.dashboard.web.bean.entity.Api;
import zk.rgw.dashboard.web.bean.entity.Organization;
import zk.rgw.dashboard.web.repository.ApiRepository;
import zk.rgw.dashboard.web.repository.factory.RepositoryFactory;
import zk.rgw.dashboard.web.service.ApiService;

public class ApiServiceImpl implements ApiService {

    private final ApiRepository apiRepository = RepositoryFactory.get(ApiRepository.class);

    @Override
    public Mono<Api> createApi(ApiDto apiDto) {
        return ContextUtil.getUser().flatMap(
                user -> apiRepository.existOneByNameAndOrg(apiDto.getName(), user.getOrganizationId())
                        .flatMap(exist -> {
                            if (Boolean.TRUE.equals(exist)) {
                                return Mono.error(BizException.of("相同组织下已经存在具有名为" + apiDto.getName() + "的API"));
                            } else {
                                Organization organization = new Organization();
                                organization.setId(user.getOrganizationId());
                                Api api = new Api().initFromDto(apiDto);
                                api.setOrganization(organization);
                                return apiRepository.insert(api);
                            }
                        })
        );
    }

}
