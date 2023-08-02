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

import com.mongodb.client.model.Filters;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import zk.rgw.dashboard.framework.exception.BizException;
import zk.rgw.dashboard.web.bean.dto.EnvironmentDto;
import zk.rgw.dashboard.web.bean.entity.Environment;
import zk.rgw.dashboard.web.repository.EnvironmentRepository;
import zk.rgw.dashboard.web.repository.factory.RepositoryFactory;
import zk.rgw.dashboard.web.service.EnvironmentService;

public class EnvironmentServiceImpl implements EnvironmentService {

    private final EnvironmentRepository environmentRepository = RepositoryFactory.get(EnvironmentRepository.class);

    @Override
    public Mono<Environment> createEnvironment(EnvironmentDto environmentDto) {
        return environmentRepository.existOneByName(environmentDto.getName()).flatMap(exists -> {
            if (exists) {
                return Mono.error(new BizException("已经存在具有相同名称的环境"));
            } else {
                Environment environment = new Environment();
                environment.setName(environmentDto.getName());
                return environmentRepository.insert(environment);
            }
        });
    }

    @Override
    public Flux<Environment> getEnvironments() {
        return environmentRepository.find(Filters.empty());
    }

}
