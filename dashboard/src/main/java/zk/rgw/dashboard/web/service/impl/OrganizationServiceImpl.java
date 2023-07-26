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

import zk.rgw.dashboard.framework.exception.BizException;
import zk.rgw.dashboard.web.bean.dto.OrganizationDto;
import zk.rgw.dashboard.web.bean.entity.Organization;
import zk.rgw.dashboard.web.repository.OrganizationRepository;
import zk.rgw.dashboard.web.repository.factory.RepositoryFactory;
import zk.rgw.dashboard.web.service.OrganizationService;

public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository = RepositoryFactory.get(OrganizationRepository.class);

    @Override
    public Mono<Organization> createOrganization(OrganizationDto organizationDto) {
        return organizationRepository.existOneByName(organizationDto.getName()).flatMap(exists -> {
            if (exists) {
                return Mono.error(new BizException("已经存在具有相同名称的组织"));
            } else {
                Organization organization = new Organization();
                organization.setName(organizationDto.getName());
                return organizationRepository.save(organization);
            }
        });
    }

}
