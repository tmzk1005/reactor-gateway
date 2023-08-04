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
package zk.rgw.dashboard.web.bean.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import io.netty.handler.codec.http.HttpMethod;
import lombok.Getter;
import lombok.Setter;

import zk.rgw.common.definition.RouteDefinition;
import zk.rgw.dashboard.framework.validate.NotBlank;
import zk.rgw.dashboard.framework.validate.Pattern;
import zk.rgw.dashboard.framework.validate.Size;
import zk.rgw.dashboard.framework.validate.ValidatableDto;
import zk.rgw.dashboard.utils.Patters;
import zk.rgw.http.constant.Constants;

@Getter
@Setter
public class ApiDto implements ValidatableDto {

    @NotBlank(message = "API名称不能为空")
    @Size(min = 3, max = 32, message = "API名称不能超过32个字符长度，且最少需要3个字符长度")
    @Pattern(regexp = Patters.IDENTIFIER_ZH, message = "API名称只能包含字母，数字和下划线")
    private String name;

    private String description;

    @NotBlank(message = "API标签不能为空")
    @Size(min = 3, max = 32, message = "API标签不能超过32个字符长度，且最少需要3个字符长度")
    @Pattern(regexp = Patters.IDENTIFIER_ZH, message = "API标签只能包含字母，数字和下划线")
    private Set<String> tags;

    private RouteDefinition routeDefinition;

    @Override
    public List<String> validate() {
        List<String> errMessages = new ArrayList<>(1);
        if (Objects.isNull(routeDefinition)) {
            errMessages.add("routeDefinition不能为空");
            return errMessages;
        }
        if (Objects.isNull(routeDefinition.getPluginDefinitions()) || routeDefinition.getPluginDefinitions().isEmpty()) {
            errMessages.add("routeDefinition的pluginDefinitions不能为空");
        }
        if (Objects.isNull(routeDefinition.getPath()) || routeDefinition.getPath().isBlank()) {
            errMessages.add("routeDefinition的path不能为空");
        }
        if (Objects.isNull(routeDefinition.getMethods()) || routeDefinition.getMethods().isEmpty()) {
            errMessages.add("routeDefinition的methods不能为空");
        } else {
            for (String method : routeDefinition.getMethods()) {
                if (!Constants.HTTP_METHODS_ALL.contains(HttpMethod.valueOf(method.toUpperCase()))) {
                    errMessages.add(method + "不是一个合法的HTTP请求方法");
                }
            }
        }
        return errMessages;
    }

}
