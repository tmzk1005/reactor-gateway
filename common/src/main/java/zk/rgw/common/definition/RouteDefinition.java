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

package zk.rgw.common.definition;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import lombok.Data;

@Data
public class RouteDefinition {

    // 业务上会使用此类的 equals 方法来判断2个 RouteDefinition 对象是否相等，进入判断API的逻辑有没有更新
    // 因此必须加上 lombok.Data 注解
    // 每个字段，如果欧式自定义类型，比如 AccessLogConf, AppAuthConf,
    // PredicateDefinition, PluginInstanceDefinition,都要使用 lombok.Data 注解

    private Set<String> methods;

    private String path;

    private AccessLogConf accessLogConf;

    private AppAuthConf appAuthConf;

    private List<PredicateDefinition> predicateDefinitions = new ArrayList<>(1);

    private List<PluginInstanceDefinition> pluginDefinitions = new ArrayList<>(4);

}
