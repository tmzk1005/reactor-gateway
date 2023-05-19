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
package zk.rgw.http.constant;

import java.util.Set;

import io.netty.handler.codec.http.HttpMethod;

import zk.rgw.plugin.api.predicate.RoutePredicate;

public class Constants {

    private Constants() {
    }

    public static final Set<HttpMethod> HTTP_METHODS_ALL = Set.of(
            HttpMethod.GET,
            HttpMethod.POST,
            HttpMethod.PATCH,
            HttpMethod.DELETE,
            HttpMethod.HEAD,
            HttpMethod.PUT,
            HttpMethod.CONNECT,
            HttpMethod.TRACE
    );

    public static final RoutePredicate ROUTE_PREDICATE_TRUE = exchange -> true;

}
