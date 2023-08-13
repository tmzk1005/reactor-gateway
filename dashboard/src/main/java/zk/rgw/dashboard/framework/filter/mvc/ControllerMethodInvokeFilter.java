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

package zk.rgw.dashboard.framework.filter.mvc;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import zk.rgw.common.exception.RgwRuntimeException;
import zk.rgw.dashboard.web.controller.ApiController;
import zk.rgw.dashboard.web.controller.ApiPluginController;
import zk.rgw.dashboard.web.controller.ApiSubscriptionController;
import zk.rgw.dashboard.web.controller.AppController;
import zk.rgw.dashboard.web.controller.EnvironmentController;
import zk.rgw.dashboard.web.controller.GatewayNodeController;
import zk.rgw.dashboard.web.controller.OrganizationController;
import zk.rgw.dashboard.web.controller.UserController;
import zk.rgw.http.path.PathUtil;
import zk.rgw.plugin.api.Exchange;
import zk.rgw.plugin.api.filter.Filter;
import zk.rgw.plugin.api.filter.FilterChain;
import zk.rgw.plugin.util.ResponseUtil;

@Slf4j
public class ControllerMethodInvokeFilter implements Filter {

    private final String apiContextPath;

    private Map<String, ControllerMeta> controllers;

    public ControllerMethodInvokeFilter(String apiContextPath) {
        this.apiContextPath = PathUtil.normalize(apiContextPath);
        initControllers();
    }

    private void initControllers() {
        controllers = new HashMap<>();
        addController(UserController.class);
        addController(OrganizationController.class);
        addController(EnvironmentController.class);
        addController(ApiController.class);
        addController(AppController.class);
        addController(ApiSubscriptionController.class);
        addController(GatewayNodeController.class);
        addController(ApiPluginController.class);
    }

    private void addController(Class<?> controllerClass) {
        ControllerMeta controllerMeta = new ControllerMeta(controllerClass);
        String controllerName = controllerMeta.getName();
        if (!controllerName.startsWith(PathUtil.SLASH)) {
            controllerName = PathUtil.SLASH + controllerName;
        }
        if (controllers.containsKey(controllerName)) {
            throw new RgwRuntimeException("There more then one controllers named " + controllerName);
        }
        controllers.put(controllerName, controllerMeta);
    }

    @Override
    public Mono<Void> filter(Exchange exchange, FilterChain chain) {
        String reqPath = exchange.getRequest().fullPath();
        if (!reqPath.startsWith(apiContextPath)) {
            return ResponseUtil.sendNotFound(exchange.getResponse());
        }
        reqPath = reqPath.substring(apiContextPath.length());

        String prefix = PathUtil.getFirstPart(reqPath);

        if (!controllers.containsKey(prefix)) {
            return ResponseUtil.sendNotFound(exchange.getResponse());
        }
        ControllerMeta controllerMeta = controllers.get(prefix);
        reqPath = PathUtil.normalize(reqPath.substring(prefix.length()));

        MethodMeta methodMeta = controllerMeta.getMethod(exchange.getRequest().method(), reqPath);

        if (Objects.isNull(methodMeta)) {
            return ResponseUtil.sendNotFound(exchange.getResponse());
        }

        return methodMeta.invoke(exchange, reqPath);
    }

}
