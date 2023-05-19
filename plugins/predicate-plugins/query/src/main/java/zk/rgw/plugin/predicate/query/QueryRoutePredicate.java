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

package zk.rgw.plugin.predicate.query;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import lombok.Data;

import zk.rgw.common.util.StringUtil;
import zk.rgw.plugin.api.Exchange;
import zk.rgw.plugin.api.predicate.JsonConfRoutePredicatePlugin;
import zk.rgw.plugin.exception.PluginConfException;
import zk.rgw.plugin.util.ExchangeUtil;

@Data
public class QueryRoutePredicate implements JsonConfRoutePredicatePlugin {

    private String param;

    private String regexp;

    @Override
    public void configure(String conf) throws PluginConfException {
        JsonConfRoutePredicatePlugin.super.configure(conf);
        Objects.requireNonNull(param);
    }

    @Override
    public boolean test(Exchange exchange) {
        Map<String, List<String>> queryParams = ExchangeUtil.getQueryParams(exchange);
        if (StringUtil.hasText(regexp)) {
            return Objects.nonNull(param) && queryParams.containsKey(param);
        }
        List<String> values = queryParams.get(param);
        if (Objects.isNull(values) || values.isEmpty()) {
            return false;
        }
        for (String value : values) {
            if (Objects.nonNull(value) && value.matches(regexp)) {
                return true;
            }
        }
        return false;
    }

}
