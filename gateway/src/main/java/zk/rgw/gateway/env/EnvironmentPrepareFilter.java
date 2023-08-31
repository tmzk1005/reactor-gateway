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
package zk.rgw.gateway.env;

import java.util.Objects;

import reactor.core.publisher.Mono;

import zk.rgw.http.route.Route;
import zk.rgw.http.utils.RouteUtil;
import zk.rgw.plugin.api.Exchange;
import zk.rgw.plugin.api.filter.Filter;
import zk.rgw.plugin.api.filter.FilterChain;
import zk.rgw.plugin.util.ExchangeUtil;

public class EnvironmentPrepareFilter implements Filter {

    private final EnvironmentManager environmentManager;

    public EnvironmentPrepareFilter(EnvironmentManager environmentManager) {
        this.environmentManager = environmentManager;
    }

    @Override
    public Mono<Void> filter(Exchange exchange, FilterChain chain) {
        Route route = RouteUtil.getRoute(exchange);
        if (Objects.nonNull(route) && Objects.nonNull(route.getEnvKey())) {
            String envKey = route.getEnvKey();
            exchange.getAttributes().put(ExchangeUtil.ENVIRONMENT_VARS, environmentManager.getEnvForOrg(envKey));
        }
        return chain.filter(exchange);
    }

}
