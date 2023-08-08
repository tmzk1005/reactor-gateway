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
import zk.rgw.dashboard.web.bean.dto.AppDto;
import zk.rgw.dashboard.web.bean.entity.App;
import zk.rgw.dashboard.web.bean.entity.Organization;
import zk.rgw.dashboard.web.repository.AppRepository;
import zk.rgw.dashboard.web.repository.factory.RepositoryFactory;
import zk.rgw.dashboard.web.service.AppService;

public class AppServiceImpl implements AppService {

    private final AppRepository appRepository = RepositoryFactory.get(AppRepository.class);

    @Override
    public Mono<App> createApp(AppDto appDto) {
        return ContextUtil.getUser().flatMap(
                user -> appRepository.existOneByNameAndOrg(appDto.getName(), user.getOrganizationId())
                        .flatMap(exist -> {
                            if (Boolean.TRUE.equals(exist)) {
                                return Mono.error(BizException.of("相同组织下已经存在具有名为" + appDto.getName() + "的应用"));
                            } else {
                                Organization organization = new Organization();
                                organization.setId(user.getOrganizationId());
                                App app = new App().initFromDto(appDto);
                                app.setOrganization(organization);
                                return appRepository.insert(app);
                            }
                        })
        );
    }

}
