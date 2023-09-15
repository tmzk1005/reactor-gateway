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

package zk.rgw.gateway.route;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import io.netty.handler.codec.http.HttpMethod;

import zk.rgw.common.definition.AppAuthConf;
import zk.rgw.common.definition.IdRouteDefinition;
import zk.rgw.common.definition.PluginInstanceDefinition;
import zk.rgw.common.definition.RouteDefinition;
import zk.rgw.gateway.GlobalSingletons;
import zk.rgw.gateway.accesslog.AccessLogFilter;
import zk.rgw.gateway.app.AppAuthFilter;
import zk.rgw.gateway.env.EnvironmentPrepareFilter;
import zk.rgw.gateway.plugin.CustomPluginClassLoader;
import zk.rgw.http.plugin.PluginLoadException;
import zk.rgw.http.route.Route;
import zk.rgw.plugin.api.filter.Filter;
import zk.rgw.plugin.api.filter.JsonConfFilterPlugin;
import zk.rgw.plugin.exception.PluginConfException;

public class RouteConverter {

    private RouteConverter() {
    }

    public static Route convertRouteDefinition(IdRouteDefinition idRouteDefinition) throws RouteConvertException {
        Route route = new Route();
        route.setId(idRouteDefinition.getId());

        RouteDefinition routeDefinition = idRouteDefinition.getRouteDefinition();

        route.setAccessLogConf(routeDefinition.getAccessLogConf());

        route.setPath(routeDefinition.getPath());
        Set<HttpMethod> httpMethods = new HashSet<>(4);
        for (String method : routeDefinition.getMethods()) {
            httpMethods.add(HttpMethod.valueOf(method.toUpperCase()));
        }
        route.setMethods(httpMethods);

        // 暂不解析predicateDefinitions

        List<Filter> filters = new ArrayList<>(routeDefinition.getPluginDefinitions().size());

        AccessLogFilter accessLogFilter = GlobalSingletons.get(AccessLogFilter.class);
        if (Objects.nonNull(accessLogFilter)) {
            filters.add(accessLogFilter);
        }

        EnvironmentPrepareFilter environmentPrepareFilter1 = GlobalSingletons.get(EnvironmentPrepareFilter.class);
        if (Objects.nonNull(environmentPrepareFilter1)) {
            filters.add(environmentPrepareFilter1);
        }

        AppAuthConf appAuthConf = routeDefinition.getAppAuthConf();
        route.setAppAuthConf(appAuthConf);
        AppAuthFilter appAuthFilter = GlobalSingletons.get(AppAuthFilter.class);
        if (Objects.nonNull(appAuthFilter) && Objects.nonNull(appAuthConf) && appAuthConf.isEnabled()) {
            filters.add(appAuthFilter);
        }

        for (PluginInstanceDefinition pluginInstanceDefinition : routeDefinition.getPluginDefinitions()) {
            try {
                filters.add(convertPluginDefinition(pluginInstanceDefinition));
            } catch (Exception exception) {
                throw new RouteConvertException(exception.getMessage(), exception);
            }
        }

        route.setFilters(filters);

        // route需要用组织id当作自己的查找环境变量的key
        route.setEnvKey(idRouteDefinition.getOrgId());
        return route;
    }

    public static Filter convertPluginDefinition(PluginInstanceDefinition definition) throws PluginConfException, PluginLoadException {
        ClassLoader classLoader;
        if (definition.isBuiltin()) {
            classLoader = RouteConverter.class.getClassLoader();
        } else {
            classLoader = CustomPluginClassLoader.getInstance(definition.getName(), definition.getVersion());
        }

        final Class<?> clazz;
        try {
            clazz = classLoader.loadClass(definition.getFullClassName());
        } catch (ClassNotFoundException exception) {
            throw new PluginLoadException("Failed to load class " + definition.getFullClassName(), exception);
        }
        final Object object;
        try {
            object = clazz.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException | SecurityException exception) {
            throw new PluginLoadException("", exception);
        }
        JsonConfFilterPlugin filter = (JsonConfFilterPlugin) object;
        filter.configure(definition.getJsonConf());
        return filter;
    }

}
