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

import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;

import zk.rgw.common.util.ObjectUtil;
import zk.rgw.dashboard.framework.context.ContextUtil;
import zk.rgw.dashboard.web.bean.Page;
import zk.rgw.dashboard.web.bean.PageData;
import zk.rgw.dashboard.web.bean.entity.User;
import zk.rgw.dashboard.web.bean.vo.AccessLogVo;
import zk.rgw.dashboard.web.repository.AccessLogRepository;
import zk.rgw.dashboard.web.repository.ApiRepository;
import zk.rgw.dashboard.web.repository.AppRepository;
import zk.rgw.dashboard.web.repository.factory.RepositoryFactory;
import zk.rgw.dashboard.web.service.AccessLogService;

@Slf4j
public class AccessLogServiceImpl implements AccessLogService {

    private final AccessLogRepository accessLogRepository = RepositoryFactory.get(AccessLogRepository.class);

    private final ApiRepository apiRepository = RepositoryFactory.get(ApiRepository.class);

    private final AppRepository appRepository = RepositoryFactory.get(AppRepository.class);

    @SuppressWarnings({ "java:S107", "java:S3776" })
    @Override
    public Mono<PageData<AccessLogVo>> searchAccessLogs(
            boolean asSubscriber,
            String envId, int pageNum, int pageSize,
            String requestId, String apiNameOrId, String clientIp, String clientNameOrId,
            Integer[] responseStatuses, Integer minTimeCostMillis, Long minTimeMillis, Long maxTimeMillis
    ) {
        return decideApiIdsAndAppIds(asSubscriber, apiNameOrId, clientNameOrId).flatMap(
                tuple3 -> {
                    List<String> apiIdList = tuple3.getT1();
                    List<String> appIdList = tuple3.getT2();
                    Boolean isAdmin = tuple3.getT3();
                    if (!ObjectUtil.isEmpty(apiNameOrId) && apiIdList.isEmpty()) {
                        // 需要用api字段过滤，但是选出来api list是空，说明没有符合条件的数据
                        return Mono.just(PageData.empty(pageSize));
                    }
                    if (!ObjectUtil.isEmpty(clientNameOrId) && appIdList.isEmpty()) {
                        // 需要用app字段过滤，但是选出来app list是空，说明没有符合条件的数据
                        return Mono.just(PageData.empty(pageSize));
                    }
                    if (Boolean.FALSE.equals(isAdmin) && !asSubscriber && apiIdList.isEmpty()) {
                        // 非管理员，非订阅者身份，但是选出来api list是空，说明没有符合条件的数据
                        return Mono.just(PageData.empty(pageSize));
                    }
                    if (Boolean.FALSE.equals(isAdmin) && asSubscriber && appIdList.isEmpty()) {
                        // 非管理员，订阅者身份，但是选出来app list是空，说明没有符合条件的数据
                        return Mono.just(PageData.empty(pageSize));
                    }

                    if (Boolean.TRUE.equals(isAdmin)) {
                        if (ObjectUtil.isEmpty(apiNameOrId)) {
                            // 是管理员，且不需要用api过滤，api list置空，提高后续repository层查询性能
                            apiIdList = List.of();
                        }
                        if (!ObjectUtil.isEmpty(appIdList)) {
                            // 是管理员，且不需要用app过滤，app list置空，提高后续repository层查询性能
                            // appIdList可能是不可变的,不能用clear方法,因此新建个空的list
                            appIdList = List.of();
                        }
                    }

                    return searchAccessLogs(
                            envId, Page.of(pageNum, pageSize),
                            requestId, apiIdList, clientIp, appIdList, responseStatuses,
                            minTimeCostMillis, minTimeMillis, maxTimeMillis
                    );
                }
        );
    }

    private Mono<Tuple3<List<String>, List<String>, Boolean>> decideApiIdsAndAppIds(boolean asSubscriber, String apiNameOrId, String clientNameOrId) {
        // 1. 管理员可以查看所有
        // 2. 从API发布者的角度，他可以查看所有apiId是属于他的访问日志，而不管appId是否属于他
        // 3. 从API订阅者的角度，他可以查看所有appId是属于他的方式日志，而不管apiId是否属于他
        return Mono.zip(
                decideApiIds(apiNameOrId, asSubscriber),
                decideAppIds(clientNameOrId, asSubscriber),
                ContextUtil.getUser().map(User::isSystemAdmin)
        );
    }

    private Mono<List<String>> decideApiIds(String apiNameOrId, boolean asSubscriber) {
        // 查询api时，如果作为订阅者，不需要根据所属过滤, 即：不是订阅者，则需要根据所属组织过滤
        boolean filterByOrg = !asSubscriber;
        try {
            ObjectId apiId = new ObjectId(apiNameOrId);
            // No exception, is api id
            return apiRepository.isIdBelongsCurrentUser(apiId, filterByOrg).map(yes -> Boolean.TRUE.equals(yes) ? List.of(apiNameOrId) : List.of());
        } catch (Exception exception) {
            // Exception, is api name
            return apiRepository.findIdsBelongsCurrentUserFilterByName(apiNameOrId, filterByOrg).collectList();
        }
    }

    private Mono<List<String>> decideAppIds(String appNameOrId, boolean asSubscriber) {
        // 查询app时，如果作为订阅者，需要根据所属过滤
        try {
            ObjectId appId = new ObjectId(appNameOrId);
            // No exception, is app id
            return appRepository.isIdBelongsCurrentUser(appId, asSubscriber).map(yes -> Boolean.TRUE.equals(yes) ? List.of(appNameOrId) : List.of());
        } catch (Exception exception) {
            // Exception, is app name, or is empty
            if (!asSubscriber) {
                return Mono.just(List.of());
            } else {
                return appRepository.findIdsBelongsCurrentUserFilterByName(appNameOrId, true).collectList();
            }
        }
    }

    @SuppressWarnings("java:S107")
    private Mono<PageData<AccessLogVo>> searchAccessLogs(
            String envId, Page page,
            String requestId, List<String> apiIds, String clientIp, List<String> appIds,
            Integer[] responseStatuses, Integer minTimeCostMillis, Long minTimeMillis, Long maxTimeMillis
    ) {
        Mono<PageData<AccessLogVo>> accessLogVosMono = accessLogRepository.searchAccessLogs(
                envId, page,
                requestId, apiIds, clientIp, appIds, responseStatuses, minTimeCostMillis, minTimeMillis, maxTimeMillis
        ).map(pageData -> pageData.map(AccessLogVo::copyFromAccessLogDocument));

        return accessLogVosMono.flatMap(pageData -> {
            List<AccessLogVo> accessLogVos = pageData.getData();
            List<String> apiIdList = accessLogVos.stream().map(AccessLogVo::getApiId).toList();
            return apiRepository.findNamesForIdsAsMap(apiIdList).map(map -> {
                for (AccessLogVo accessLogVo : accessLogVos) {
                    accessLogVo.setApiName(map.get(accessLogVo.getApiId()).getName());
                }
                return pageData;
            });
        });
    }

}
