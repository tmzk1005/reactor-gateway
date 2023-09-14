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
package zk.rgw.plugin.filter.cors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.netty.handler.codec.http.HttpMethod;
import lombok.Getter;
import lombok.Setter;

import zk.rgw.common.util.ObjectUtil;

@Getter
@Setter
public class CorsStrategy {

    public static final String ALL = "*";

    private List<String> allowedOrigins = new ArrayList<>(List.of(ALL));

    private List<String> allowedOriginPatterns = new ArrayList<>(List.of(ALL));

    private List<String> allowedMethods = Arrays.asList(
            HttpMethod.GET.name(),
            HttpMethod.HEAD.name(),
            HttpMethod.POST.name(),
            HttpMethod.PUT.name(),
            HttpMethod.PATCH.name(),
            HttpMethod.DELETE.name(),
            HttpMethod.OPTIONS.name(),
            HttpMethod.TRACE.name()
    );

    private List<String> allowedHeaders = new ArrayList<>(List.of(ALL));

    private List<String> exposedHeaders;

    private boolean allowCredentials = false;

    private Long maxAge;

    /**
     * 是否允许Credentials，当allowedOrigins含有*时，永远要返回false
     */
    public boolean isAllowCredentials() {
        return allowCredentials && !allowedOrigins.contains(ALL);
    }

    public boolean isAllowAnyOrigins() {
        return allowedOrigins.contains(ALL);
    }

    public boolean isAllowedOrigin(String origin) {
        if (ObjectUtil.isEmpty(origin)) {
            return false;
        }
        return isAllowAnyOrigins() || allowedOrigins.contains(trimTrailingSlash(origin));
    }

    public boolean isAllowedMethod(String httpMethod) {
        return allowedMethods.contains(httpMethod);
    }

    public boolean isAllowAnyHeaders() {
        return allowedHeaders.contains(ALL);
    }

    public boolean isAllowedHeader(String headerName) {
        for (String allowedHeader : allowedHeaders) {
            if (allowedHeader.equalsIgnoreCase(headerName)) {
                return true;
            }
        }
        return false;
    }

    private static String trimTrailingSlash(String origin) {
        return origin.endsWith("/") ? origin.substring(0, origin.length() - 1) : origin;
    }

}
