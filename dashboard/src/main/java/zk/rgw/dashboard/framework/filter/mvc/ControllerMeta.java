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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.handler.codec.http.HttpMethod;
import lombok.Getter;

import zk.rgw.common.exception.RgwRuntimeException;
import zk.rgw.dashboard.framework.annotation.Controller;
import zk.rgw.dashboard.framework.annotation.RequestMapping;
import zk.rgw.http.path.AntPathMatcher;
import zk.rgw.http.path.PathUtil;

public class ControllerMeta {

    private static final AntPathMatcher ANT_PATH_MATCHER = AntPathMatcher.getDefaultInstance();

    private final Class<?> controllerClass;

    @Getter
    private final String name;

    @Getter
    private final Object controller;

    @Getter
    private final Map<HttpMethod, Map<String, MethodMeta>> methods = new ConcurrentHashMap<>();

    public ControllerMeta(Class<?> controllerClass) {
        this.controllerClass = controllerClass;
        try {
            Constructor<?> constructor = this.controllerClass.getConstructor();
            this.controller = constructor.newInstance();
        } catch (ReflectiveOperationException roe) {
            throw new RgwRuntimeException("Failed to create instance for class " + controllerClass.getName(), roe);
        }
        this.name = parseName();
        parseRequestMappingMethods();
    }

    public MethodMeta getMethod(HttpMethod httpMethod, String requestPath) {
        final Map<String, MethodMeta> map = methods.get(httpMethod);
        if (Objects.isNull(map)) {
            return null;
        }
        for (Map.Entry<String, MethodMeta> entry : map.entrySet()) {
            final String pathPattern = entry.getKey();
            if (!ANT_PATH_MATCHER.isPattern(pathPattern)) {
                if (pathPattern.equals(requestPath)) {
                    return entry.getValue();
                } else {
                    continue;
                }
            }
            if (ANT_PATH_MATCHER.match(pathPattern, requestPath)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private void parseRequestMappingMethods() {
        final Method[] allPublicMethods = controllerClass.getMethods();
        for (Method method : allPublicMethods) {
            final RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
            if (Objects.isNull(requestMapping)) {
                continue;
            }
            final HttpMethod httpMethod = HttpMethod.valueOf(requestMapping.method().name());
            methods.putIfAbsent(httpMethod, new ConcurrentHashMap<>());
            String path = requestMapping.path();
            path = PathUtil.normalize(path);
            final Map<String, MethodMeta> path2MethodMetas = methods.get(httpMethod);
            if (path2MethodMetas.containsKey(path)) {
                throw new RgwRuntimeException("Method route path " + path + "conflict.");
            }
            path2MethodMetas.put(path, new MethodMeta(controller, method, path));
        }
    }

    private String parseName() {
        Controller annotation = controllerClass.getAnnotation(Controller.class);
        if (Objects.isNull(annotation)) {
            throw new RgwRuntimeException(controllerClass.getName() + " has not @Controller annotation");
        }
        return annotation.value();
    }

}
