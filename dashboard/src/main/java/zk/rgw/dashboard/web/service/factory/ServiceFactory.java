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
package zk.rgw.dashboard.web.service.factory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import zk.rgw.common.util.ObjectUtil;
import zk.rgw.dashboard.framework.context.ContextUtil;
import zk.rgw.dashboard.framework.exception.AccessDeniedException;
import zk.rgw.dashboard.framework.security.HasRole;
import zk.rgw.dashboard.framework.security.Role;
import zk.rgw.dashboard.web.service.OrganizationService;
import zk.rgw.dashboard.web.service.UserService;
import zk.rgw.dashboard.web.service.impl.OrganizationServiceImpl;
import zk.rgw.dashboard.web.service.impl.UserServiceImpl;

@Slf4j
public class ServiceFactory {

    private static final Map<Class<?>, Object> SERVICES = new HashMap<>();

    static {
        SERVICES.put(UserService.class, new SecurityProxy<>(UserService.class, new UserServiceImpl()).getProxy());
        SERVICES.put(OrganizationService.class, new SecurityProxy<>(OrganizationService.class, new OrganizationServiceImpl()).getProxy());
    }

    private ServiceFactory() {
    }

    @SuppressWarnings("unchecked")
    public static <S> S get(Class<S> serviceClass) {
        return (S) SERVICES.get(serviceClass);
    }

    private static class SecurityProxy<S> implements InvocationHandler {

        private final Class<S> serviceInterface;

        private final S serviceNormalInstance;

        private final Map<Method, Role[]> rolesCache = new ConcurrentHashMap<>();

        public SecurityProxy(Class<S> serviceInterface, S serviceNormalInstance) {
            this.serviceInterface = serviceInterface;
            this.serviceNormalInstance = serviceNormalInstance;
        }

        @SuppressWarnings("unchecked")
        public S getProxy() {
            return (S) Proxy.newProxyInstance(serviceNormalInstance.getClass().getClassLoader(), new Class[] { serviceInterface }, this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (!rolesCache.containsKey(method)) {
                rolesCache.put(method, parseNeedRoles(method));
            }
            final Role[] needRoles = rolesCache.get(method);
            if (ObjectUtil.isEmpty(needRoles)) {
                return method.invoke(serviceNormalInstance, args);
            }
            Mono<Boolean> hasRolesMono = ContextUtil.hasRoles(needRoles).map(hasRoles -> {
                if (!hasRoles) {
                    throw new AccessDeniedException("没有访问权限!");
                }
                return true;
            });
            final Class<?> returnType = method.getReturnType();
            if (Mono.class.isAssignableFrom(returnType)) {
                return hasRolesMono.then((Mono<?>) method.invoke(serviceNormalInstance, args));
            } else if (Flux.class.isAssignableFrom(returnType)) {
                return hasRolesMono.thenMany((Flux<?>) method.invoke(serviceNormalInstance, args));
            } else {
                log.warn(
                        "Method {} of interface {} does not has a Mono of Flux return type, so no roles check will be apply!",
                        method.getName(), serviceInterface.getName()
                );
                return method.invoke(serviceNormalInstance, args);
            }
        }

        private static Role[] parseNeedRoles(Method method) {
            final HasRole annotation = method.getAnnotation(HasRole.class);
            if (Objects.isNull(annotation)) {
                return new Role[0];
            }
            final Role[] value = annotation.value();
            if (ObjectUtil.isEmpty(value)) {
                return new Role[0];
            }
            return value;
        }

    }

}
