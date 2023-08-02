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

import java.util.function.Function;

import com.mongodb.client.model.Filters;
import lombok.extern.slf4j.Slf4j;
import org.bson.conversions.Bson;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import zk.rgw.dashboard.framework.context.ContextUtil;
import zk.rgw.dashboard.framework.exception.AccessDeniedException;
import zk.rgw.dashboard.framework.exception.BizException;
import zk.rgw.dashboard.web.bean.dto.EnvironmentDto;
import zk.rgw.dashboard.web.bean.entity.EnvBinding;
import zk.rgw.dashboard.web.bean.entity.Environment;
import zk.rgw.dashboard.web.bean.entity.Organization;
import zk.rgw.dashboard.web.repository.EnvBindingRepository;
import zk.rgw.dashboard.web.repository.EnvironmentRepository;
import zk.rgw.dashboard.web.repository.OrganizationRepository;
import zk.rgw.dashboard.web.repository.factory.RepositoryFactory;
import zk.rgw.dashboard.web.service.EnvironmentService;

@Slf4j
public class EnvironmentServiceImpl implements EnvironmentService {

    private final EnvironmentRepository environmentRepository = RepositoryFactory.get(EnvironmentRepository.class);

    private final EnvBindingRepository envBindingRepository = RepositoryFactory.get(EnvBindingRepository.class);

    private final OrganizationRepository organizationRepository = RepositoryFactory.get(OrganizationRepository.class);

    @Override
    public Mono<Environment> createEnvironment(EnvironmentDto environmentDto) {
        return environmentRepository.existOneByName(environmentDto.getName()).flatMap(exists -> {
            if (Boolean.TRUE.equals(exists)) {
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

    @Override
    public Mono<EnvBinding> getOneEnvBinding(String envId, String orgId) {
        return ContextUtil.getUser().flatMap(user -> {
            if (!user.isSystemAdmin() && !user.getOrganizationId().equals(orgId)) {
                return Mono.error(new AccessDeniedException("无权访问其他组织数据"));
            }

            return Mono.zip(
                    environmentRepository.findOneById(envId),
                    organizationRepository.findOneById(orgId)
            ).switchIfEmpty(Mono.error(BizException.of("指定的环境或者组织不存在")))
                    .flatMap((Function<Tuple2<Environment, Organization>, Mono<EnvBinding>>) tuple -> {
                        Bson filter = Filters.and(
                                Filters.eq("environment", tuple.getT1().getId()),
                                Filters.eq("organization", tuple.getT2().getId())
                        );
                        return envBindingRepository.findOne(filter)
                                .switchIfEmpty(Mono.just(new EnvBinding()))
                                .doOnNext(envBinding -> {
                                    envBinding.setEnvironment(tuple.getT1());
                                    envBinding.setOrganization(tuple.getT2());
                                });
                    });
        });
    }

}
