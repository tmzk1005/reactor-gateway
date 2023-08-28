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

import java.util.List;

import reactor.core.publisher.Mono;

import zk.rgw.dashboard.web.bean.Page;
import zk.rgw.dashboard.web.bean.PageData;
import zk.rgw.dashboard.web.bean.vo.AccessLogVo;
import zk.rgw.dashboard.web.repository.AccessLogRepository;
import zk.rgw.dashboard.web.repository.ApiRepository;
import zk.rgw.dashboard.web.repository.factory.RepositoryFactory;
import zk.rgw.dashboard.web.service.AccessLogService;

public class AccessLogServiceImpl implements AccessLogService {

    private final AccessLogRepository accessLogRepository = RepositoryFactory.get(AccessLogRepository.class);

    private final ApiRepository apiRepository = RepositoryFactory.get(ApiRepository.class);

    @Override
    public Mono<PageData<AccessLogVo>> searchAccessLogs(String envId, int pageNum, int pageSize) {
        Mono<PageData<AccessLogVo>> accessLogVosMono = accessLogRepository.searchAccessLogs(envId, Page.of(pageNum, pageSize))
                .map(page -> page.map(AccessLogVo::copyFromAccessLogDocument));

        return accessLogVosMono.flatMap(pageData -> {
            List<AccessLogVo> accessLogVos = pageData.getData();
            List<String> apiIds = accessLogVos.stream().map(AccessLogVo::getApiId).toList();
            return apiRepository.findNamesForIdsAsMap(apiIds).map(map -> {
                for (AccessLogVo accessLogVo : accessLogVos) {
                    accessLogVo.setApiName(map.get(accessLogVo.getApiId()).getName());
                }
                return pageData;
            });
        });
    }

}
