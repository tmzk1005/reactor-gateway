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

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;

import zk.rgw.common.definition.IdRouteDefinition;
import zk.rgw.common.event.EventPublisher;
import zk.rgw.common.event.RgwEvent;
import zk.rgw.common.util.ObjectUtil;
import zk.rgw.dashboard.framework.context.ContextUtil;
import zk.rgw.dashboard.framework.exception.BizException;
import zk.rgw.dashboard.global.GlobalSingletons;
import zk.rgw.dashboard.utils.ErrorMsgUtil;
import zk.rgw.dashboard.web.bean.ApiPublishStatus;
import zk.rgw.dashboard.web.bean.Page;
import zk.rgw.dashboard.web.bean.PageData;
import zk.rgw.dashboard.web.bean.RouteDefinitionPublishSnapshot;
import zk.rgw.dashboard.web.bean.RouteDefinitionPublishSnapshotDisplay;
import zk.rgw.dashboard.web.bean.dto.ApiDto;
import zk.rgw.dashboard.web.bean.entity.Api;
import zk.rgw.dashboard.web.bean.entity.Environment;
import zk.rgw.dashboard.web.bean.entity.Organization;
import zk.rgw.dashboard.web.bean.entity.RgwSequence;
import zk.rgw.dashboard.web.bean.entity.User;
import zk.rgw.dashboard.web.bean.vo.ApiVo;
import zk.rgw.dashboard.web.bean.vo.ReleasedApiVo;
import zk.rgw.dashboard.web.bean.vo.SimpleEnvironmentVo;
import zk.rgw.dashboard.web.event.ApiPublishingEvent;
import zk.rgw.dashboard.web.repository.ApiRepository;
import zk.rgw.dashboard.web.repository.EnvironmentRepository;
import zk.rgw.dashboard.web.repository.OrganizationRepository;
import zk.rgw.dashboard.web.repository.RgwSequenceRepository;
import zk.rgw.dashboard.web.repository.UserRepository;
import zk.rgw.dashboard.web.repository.factory.RepositoryFactory;
import zk.rgw.dashboard.web.service.ApiService;

public class ApiServiceImpl implements ApiService {

    private final ApiRepository apiRepository = RepositoryFactory.get(ApiRepository.class);

    private final EnvironmentRepository environmentRepository = RepositoryFactory.get(EnvironmentRepository.class);

    private final OrganizationRepository organizationRepository = RepositoryFactory.get(OrganizationRepository.class);

    private final UserRepository userRepository = RepositoryFactory.get(UserRepository.class);

    private final RgwSequenceRepository rgwSequenceRepository = RepositoryFactory.get(RgwSequenceRepository.class);

    private final EventPublisher<RgwEvent> eventPublisher;

    @SuppressWarnings("unchecked")
    public ApiServiceImpl() {
        this.eventPublisher = GlobalSingletons.get(EventPublisher.class);
    }

    @Override
    public Mono<Api> createApi(ApiDto apiDto) {
        return ContextUtil.getUser().flatMap(
                user -> apiRepository.existOneByNameAndOrg(apiDto.getName(), user.getOrganization().getId())
                        .flatMap(exist -> {
                            if (Boolean.TRUE.equals(exist)) {
                                return Mono.error(BizException.of("相同组织下已经存在具有名为" + apiDto.getName() + "的API"));
                            } else {
                                Organization organization = new Organization();
                                organization.setId(user.getOrganization().getId());
                                Api api = new Api().initFromDto(apiDto);
                                api.setOrganization(organization);
                                return apiRepository.insert(api);
                            }
                        })
        );
    }

    @Override
    public Mono<Api> updateApi(String apiId, ApiDto apiDto) {
        Mono<Api> oldApiMono = apiRepository.findOneById(apiId)
                .switchIfEmpty(Mono.error(BizException.of(ErrorMsgUtil.apiNotExist(apiId))));
        Mono<User> userMono = ContextUtil.getUser();

        return Mono.zip(oldApiMono, userMono).flatMap(tuple2 -> {
            Api oldApi = tuple2.getT1();
            User user = tuple2.getT2();

            if (!user.getOrganization().getId().equals(oldApi.getOrganization().getId())) {
                return Mono.error(BizException.of(ErrorMsgUtil.noApiRights(apiId)));
            }

            if (!oldApi.getName().equals(apiDto.getName())) {
                return apiRepository.existOneByNameAndOrg(apiDto.getName(), user.getOrganization().getId())
                        .flatMap(exist -> {
                            if (Boolean.TRUE.equals(exist)) {
                                return Mono.error(BizException.of("相同组织下已经存在具有名为" + apiDto.getName() + "的API"));
                            } else {
                                return doUpdateApiAndSave(oldApi, apiDto);
                            }
                        });
            }
            return doUpdateApiAndSave(oldApi, apiDto);
        });
    }

    private Mono<Api> doUpdateApiAndSave(Api api, ApiDto apiDto) {
        int hash1 = api.getRouteDefinition().hashCode();
        api.initFromDto(apiDto);
        int hash2 = api.getRouteDefinition().hashCode();
        if (hash1 != hash2) {
            api.setRouteDefinitionLastModifiedDate(Instant.now());
        }
        return apiRepository.save(api);
    }

    @Override
    public Mono<PageData<Api>> listApis(
            int pageNum, int pageSize,
            String apiId, String name, String method, String path,
            String[] tags, boolean tagModeIsAnd, String description
    ) {
        if (!ObjectUtil.isEmpty(apiId)) {
            try {
                new ObjectId(apiId);
            } catch (Exception exception) {
                return Mono.just(PageData.empty(pageSize));
            }
        }

        return ContextUtil.getUser().map(user -> {
            Bson finalFilter;

            List<Bson> filters = createFilter(apiId, name, method, path, tags, tagModeIsAnd, description);

            if (!user.isSystemAdmin()) {
                filters.add(Filters.eq("organization", new ObjectId(user.getOrganization().getId())));
            }

            if (filters.size() == 1) {
                finalFilter = filters.get(0);
            } else if (filters.size() > 1) {
                finalFilter = Filters.and(filters);
            } else {
                finalFilter = Filters.empty();
            }
            return finalFilter;
        }).flatMap(filter -> apiRepository.find(filter, null, Page.of(pageNum, pageSize)));
    }

    private List<Bson> createFilter(
            String apiId, String name, String method, String path,
            String[] tags, boolean tagModeIsAnd, String description
    ) {
        List<Bson> filters = new ArrayList<>();

        if (!ObjectUtil.isEmpty(apiId)) {
            filters.add(Filters.eq("_id", new ObjectId(apiId)));
        }

        if (!ObjectUtil.isEmpty(name)) {
            filters.add(Filters.regex("name", name, "i"));
        }

        if (!ObjectUtil.isEmpty(method)) {
            filters.add(Filters.all("routeDefinition.methods", method));
        }

        if (!ObjectUtil.isEmpty(path)) {
            filters.add(Filters.regex("routeDefinition.path", path, "i"));
        }

        if (!ObjectUtil.isEmpty(tags)) {
            if (tagModeIsAnd) {
                filters.add(Filters.all("tags", tags));
            } else {
                filters.add(Filters.in("tags", tags));
            }
        }

        if (!ObjectUtil.isEmpty(description)) {
            filters.add(Filters.regex("description", description, "i"));
        }

        return filters;
    }

    @Override
    public Mono<ApiVo> getApiDetailById(String apiId) {
        Mono<Api> apiMono = apiRepository.findOneById(apiId).switchIfEmpty(Mono.error(BizException.of(ErrorMsgUtil.apiNotExist(apiId))));
        Mono<User> userMono = ContextUtil.getUser();
        Mono<ApiVo> apiVoMono = Mono.zip(apiMono, userMono).map(tuple2 -> {
            Api api = tuple2.getT1();
            if (!Objects.equals(api.getOrganization().getId(), tuple2.getT2().getOrganization().getId())) {
                throw BizException.of(ErrorMsgUtil.noApiRights(apiId));
            } else {
                return api;
            }
        }).flatMap(api -> {
            // 为了填充Org name
            Mono<Organization> orgMono = organizationRepository.findOneById(api.getOrganization().getId());
            return orgMono.map(org -> {
                api.setOrganization(org);
                return api;
            });
        }).map(api -> new ApiVo().initFromPo(api));

        return apiVoMono.flatMap(this::fillMorePublishSnapshotsInfo);
    }

    private Mono<ApiVo> fillMorePublishSnapshotsInfo(final ApiVo apiVo) {
        Map<String, RouteDefinitionPublishSnapshotDisplay> publishSnapshots = apiVo.getPublishSnapshots();

        List<String> userIdList = publishSnapshots.values().stream().map(RouteDefinitionPublishSnapshot::getPublisherId).toList();
        Set<String> userIds = new HashSet<>(userIdList);
        Mono<Map<String, User>> userMapMono = userRepository.getMapByIdIn(userIds);

        Mono<List<Environment>> allEnvMono = environmentRepository.findAll().collectList();

        return Mono.zip(userMapMono, allEnvMono).doOnNext(tuple2 -> {
            Map<String, User> userMap = tuple2.getT1();
            List<Environment> allEnv = tuple2.getT2();

            for (Environment env : allEnv) {
                String envId = env.getId();
                if (publishSnapshots.containsKey(envId)) {
                    String userId = publishSnapshots.get(envId).getPublisherId();
                    publishSnapshots.get(envId).setPublisherName(userMap.get(userId).getNickname());
                    publishSnapshots.get(envId).setEnv(new SimpleEnvironmentVo(envId, env.getName()));
                } else {
                    // 没有发布到此环境，仍然填充一些信息供前端渲染
                    RouteDefinitionPublishSnapshotDisplay snapshotDisplay = new RouteDefinitionPublishSnapshotDisplay();
                    snapshotDisplay.setEnv(new SimpleEnvironmentVo(envId, env.getName()));
                    snapshotDisplay.setPublishStatus(ApiPublishStatus.UNPUBLISHED);
                    publishSnapshots.put(envId, snapshotDisplay);
                }
            }
        }).thenReturn(apiVo);
    }

    @Override
    public Mono<Api> publishApi(String apiId, String envId) {
        return prepareApiForOperate(apiId, envId).flatMap(tuple3 -> doPublishApi(tuple3.getT1(), tuple3.getT2(), tuple3.getT3()))
                .doOnSuccess(api -> emitEvent(api, envId, true));
    }

    private void emitEvent(Api api, String envId, boolean isPublish) {
        IdRouteDefinition idRouteDefinition = new IdRouteDefinition(api.getId(), api.getOrganization().getId());
        idRouteDefinition.setSeqNum(api.getPublishSnapshots().get(envId).getOpSeq());
        if (isPublish) {
            idRouteDefinition.setRouteDefinition(api.getPublishSnapshots().get(envId).getRouteDefinition());
        }
        ApiPublishingEvent apiPublishingEvent = new ApiPublishingEvent(idRouteDefinition, isPublish, envId);
        eventPublisher.publishEvent(apiPublishingEvent);
    }

    private Mono<Tuple3<Api, User, Environment>> prepareApiForOperate(String apiId, String envId) {
        try {
            new ObjectId(apiId);
        } catch (Exception exception) {
            return Mono.error(BizException.of(ErrorMsgUtil.apiNotExist(apiId)));
        }
        try {
            new ObjectId(envId);
        } catch (Exception exception) {
            return Mono.error(BizException.of(ErrorMsgUtil.envNotExist(apiId)));
        }
        Mono<Api> apiMono = apiRepository.findOneById(apiId).switchIfEmpty(Mono.error(BizException.of(ErrorMsgUtil.apiNotExist(apiId))));
        Mono<User> userMono = ContextUtil.getUser();
        Mono<Environment> envMono = environmentRepository.findOneById(envId).switchIfEmpty(Mono.error(BizException.of(ErrorMsgUtil.envNotExist(envId))));
        return Mono.zip(apiMono, userMono, envMono).map(tuple3 -> {
            if (!Objects.equals(tuple3.getT1().getOrganization().getId(), tuple3.getT2().getOrganization().getId())) {
                throw BizException.of(ErrorMsgUtil.noApiRights(apiId));
            } else {
                return tuple3;
            }
        });
    }

    private Mono<Api> doPublishApi(Api api, User user, Environment environment) {
        String envId = environment.getId();
        Map<String, RouteDefinitionPublishSnapshot> publishSnapshots = api.getPublishSnapshots();
        if (Objects.isNull(publishSnapshots)) {
            publishSnapshots = new HashMap<>();
            api.setPublishSnapshots(publishSnapshots);
        } else if (publishSnapshots.containsKey(envId) && publishSnapshots.get(envId).isStatusPublished()) {
            // 已经处于发布状态，且API的信息其实并没有改变
            return Mono.error(BizException.of("无需重复发布，API已处于最新发布状态"));
        }

        RouteDefinitionPublishSnapshot publishSnapshot = new RouteDefinitionPublishSnapshot();
        publishSnapshot.setRouteDefinition(api.getRouteDefinition());
        publishSnapshot.setPublisherId(user.getId());
        publishSnapshot.setPublishStatus(ApiPublishStatus.PUBLISHED);
        publishSnapshot.setLastModifiedDate(Instant.now());

        publishSnapshots.put(envId, publishSnapshot);

        String seqName = RgwSequence.generateNameForApiPublishing(envId);
        return rgwSequenceRepository.next(seqName).flatMap(seq -> {
            publishSnapshot.setOpSeq(seq);
            return apiRepository.save(api);
        });
    }

    @Override
    public Mono<Api> unpublishApi(String apiId, String envId) {
        return prepareApiForOperate(apiId, envId).flatMap(tuple3 -> doUnpublishApi(tuple3.getT1(), tuple3.getT2(), tuple3.getT3()))
                .doOnSuccess(api -> emitEvent(api, envId, false));
    }

    private Mono<Api> doUnpublishApi(Api api, User user, Environment environment) {
        String envId = environment.getId();
        Map<String, RouteDefinitionPublishSnapshot> publishSnapshots = api.getPublishSnapshots();
        if (Objects.isNull(publishSnapshots) || !publishSnapshots.containsKey(envId) || publishSnapshots.get(envId).isStatusUnpublished()) {
            return Mono.error(BizException.of("API处于未发布状态"));
        }
        RouteDefinitionPublishSnapshot publishSnapshot = publishSnapshots.get(envId);
        publishSnapshot.setPublishStatus(ApiPublishStatus.UNPUBLISHED);
        publishSnapshot.setLastModifiedDate(Instant.now());
        publishSnapshot.setPublisherId(user.getId());

        String seqName = RgwSequence.generateNameForApiPublishing(envId);
        return rgwSequenceRepository.next(seqName).flatMap(seq -> {
            publishSnapshot.setOpSeq(seq);
            return apiRepository.save(api);
        });
    }

    @Override
    public Mono<PageData<ReleasedApiVo>> listReleasedApiVo(String envId, String searchText, int pageNum, int pageSize) {
        Bson filter = Filters.and(
                Filters.exists("publishSnapshots." + envId),
                Filters.eq("publishSnapshots." + envId + ".publishStatus", "PUBLISHED")
        );
        if (Objects.nonNull(searchText) && !searchText.isBlank()) {
            String options = "i";
            Bson searchFilter = Filters.or(
                    Filters.regex("name", searchText, options),
                    Filters.regex("tags", searchText, options),
                    Filters.regex("description", searchText, options)
            );
            filter = Filters.and(filter, searchFilter);
        }
        return apiRepository.find(filter, null, Page.of(pageNum, pageSize)).map(page -> page.map(api -> new ReleasedApiVo(api, envId)));
    }

}
