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

package zk.rgw.http.route;

import java.util.List;
import java.util.Set;

import io.netty.handler.codec.http.HttpMethod;
import lombok.Getter;
import lombok.Setter;

import zk.rgw.common.definition.AccessLogConf;
import zk.rgw.http.constant.Constants;
import zk.rgw.plugin.api.filter.Filter;
import zk.rgw.plugin.api.predicate.RoutePredicate;

@Getter
@Setter
public class Route {

    private String id;

    private Set<HttpMethod> methods = Constants.HTTP_METHODS_ALL;

    private String path;

    private RoutePredicate predicate = Constants.ROUTE_PREDICATE_TRUE;

    private List<Filter> filters;

    private AccessLogConf accessLogConf;

}
