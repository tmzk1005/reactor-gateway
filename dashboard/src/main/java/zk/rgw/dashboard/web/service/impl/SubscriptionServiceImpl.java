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
import java.util.List;
import java.util.Objects;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import org.bson.BsonDocument;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;

import zk.rgw.dashboard.framework.context.ContextUtil;
import zk.rgw.dashboard.framework.exception.BizException;
import zk.rgw.dashboard.utils.ErrorMsgUtil;
import zk.rgw.dashboard.web.bean.Page;
import zk.rgw.dashboard.web.bean.PageData;
import zk.rgw.dashboard.web.bean.entity.Api;
import zk.rgw.dashboard.web.bean.entity.ApiSubscribe;
import zk.rgw.dashboard.web.bean.entity.App;
import zk.rgw.dashboard.web.bean.entity.Organization;
import zk.rgw.dashboard.web.bean.entity.User;
import zk.rgw.dashboard.web.repository.ApiRepository;
import zk.rgw.dashboard.web.repository.ApiSubscribeRepository;
import zk.rgw.dashboard.web.repository.AppRepository;
import zk.rgw.dashboard.web.repository.factory.RepositoryFactory;
import zk.rgw.dashboard.web.service.SubscriptionService;

public class SubscriptionServiceImpl implements SubscriptionService {

    private static final List<Bson> LOOKUP = List.of(
            Aggregates.lookup("Api", "api", "_id", "apiLookup"),
            Aggregates.lookup("App", "app", "_id", "appLookup"),
            Aggregates.lookup("User", "user", "_id", "userLookup"),
            Aggregates.lookup("Organization", "appOrganization", "_id", "appOrganizationLookup"),
            Aggregates.lookup("Organization", "apiOrganization", "_id", "apiOrganizationLookup"),
            Aggregates.project(
                    Projections.fields(
                            Projections.include(
                                    "_id", "state", "applyTime", "handleTime"
                            ),
                            Projections.computed("api", BsonDocument.parse("{\"$first\": \"$apiLookup\"}")),
                            Projections.computed("app", BsonDocument.parse("{\"$first\": \"$appLookup\"}")),
                            Projections.computed("user", BsonDocument.parse("{\"$first\": \"$userLookup\"}")),
                            Projections.computed("appOrganization", BsonDocument.parse("{\"$first\": \"$appOrganizationLookup\"}")),
                            Projections.computed("apiOrganization", BsonDocument.parse("{\"$first\": \"$apiOrganizationLookup\"}"))
                    )
            )
    );

    private final ApiRepository apiRepository = RepositoryFactory.get(ApiRepository.class);

    private final AppRepository appRepository = RepositoryFactory.get(AppRepository.class);

    private final ApiSubscribeRepository apiSubscribeRepository = RepositoryFactory.get(ApiSubscribeRepository.class);

    @Override
    public Mono<Void> applySubscribeApi(String apiId, String appId) {
        return prepareForApiSubscription(apiId, appId)
                .flatMap(
                        tuple3 -> apiSubscribeRepository.existSameUnhandled(apiId, appId)
                                .flatMap(exists -> {
                                    if (Boolean.TRUE.equals(exists)) {
                                        return Mono.error(BizException.of("相同的订阅申请正在审批中，请勿重复申请!"));
                                    }

                                    App app = tuple3.getT1();
                                    Api api = tuple3.getT2();
                                    User user = tuple3.getT3();

                                    final ApiSubscribe apiSubscribe = new ApiSubscribe();
                                    apiSubscribe.setApp(app);
                                    apiSubscribe.setApi(api);
                                    apiSubscribe.setUser(user);

                                    apiSubscribe.setAppOrganization(new Organization(user.getOrganizationId()));

                                    apiSubscribe.setApiOrganization(api.getOrganization());

                                    apiSubscribe.setState(ApiSubscribe.State.CREATED);
                                    apiSubscribe.setApplyTime(Instant.now());
                                    return apiSubscribeRepository.insert(apiSubscribe);
                                })
                ).then();
    }

    private Mono<Tuple3<App, Api, User>> prepareForApiSubscription(String apiId, String appId) {
        Mono<App> appMono = appRepository.findOneById(appId).switchIfEmpty(Mono.error(BizException.of(ErrorMsgUtil.appNotExist(appId))));
        Mono<Api> apiMono = apiRepository.findOneById(apiId).switchIfEmpty(Mono.error(BizException.of(ErrorMsgUtil.apiNotExist(apiId))));
        Mono<User> userMono = ContextUtil.getUser();
        return Mono.zip(appMono, apiMono, userMono).map(tuple3 -> {
            if (!Objects.equals(tuple3.getT1().getOrganization().getId(), tuple3.getT3().getOrganizationId())) {
                throw BizException.of(ErrorMsgUtil.noAppRights(appId));
            }
            return tuple3;
        });
    }

    @Override
    public Mono<PageData<ApiSubscribe>> myApiSubscribes(boolean asSubscriber, int pageNum, int pageSize) {
        return ContextUtil.getUser().map(User::getOrganizationId).flatMap(curUserOrgId -> {
            Bson orgFilter;
            if (asSubscriber) {
                orgFilter = Filters.eq("appOrganization", new ObjectId(curUserOrgId));
            } else {
                orgFilter = Filters.eq("apiOrganization", new ObjectId(curUserOrgId));
            }
            Bson sorts = Sorts.descending("applyTime");
            return apiSubscribeRepository.find(orgFilter, sorts, Page.of(pageNum, pageSize), LOOKUP);
        });
    }

}
