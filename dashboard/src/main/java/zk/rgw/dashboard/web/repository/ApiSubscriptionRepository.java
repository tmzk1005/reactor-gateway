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
package zk.rgw.dashboard.web.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.mongodb.client.model.Filters;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.bson.types.ObjectId;
import reactor.core.publisher.Mono;

import zk.rgw.dashboard.web.bean.entity.Api;
import zk.rgw.dashboard.web.bean.entity.ApiSubscription;
import zk.rgw.dashboard.web.bean.entity.App;

public class ApiSubscriptionRepository extends AbstractMongodbRepository<ApiSubscription> {

    public ApiSubscriptionRepository(MongoClient mongoClient, MongoDatabase database) {
        super(mongoClient, database, ApiSubscription.class);
    }

    public Mono<ApiSubscription> findByApiId(String apiId) {
        return findOne(Filters.eq("api", new ObjectId(apiId)));
    }

    public Mono<Boolean> isAppSubscribedApi(String apiId, String appId) {
        return findByApiId(apiId).map(apiSubscription -> {
            List<App> apps = apiSubscription.getApps();
            if (Objects.isNull(apps) || apps.isEmpty()) {
                return false;
            }
            for (App oneApp : apps) {
                if (Objects.equals(appId, oneApp.getId())) {
                    return true;
                }
            }
            return false;
        }).switchIfEmpty(Mono.just(false));
    }

    public Mono<ApiSubscription> saveSubscriptionRelationship(Api api, App app) {
        return findByApiId(api.getId())
                .switchIfEmpty(Mono.just(new ApiSubscription()))
                .flatMap(apiSubscription -> {
                    apiSubscription.setApi(api);
                    List<App> apps = apiSubscription.getApps();
                    if (Objects.isNull(apps)) {
                        apps = new ArrayList<>();
                        apiSubscription.setApps(apps);
                    }
                    apps.add(app);
                    return save(apiSubscription);
                });
    }

}
