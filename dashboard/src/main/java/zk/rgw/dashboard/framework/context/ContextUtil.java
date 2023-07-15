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

package zk.rgw.dashboard.framework.context;

import java.util.Objects;
import java.util.Set;

import reactor.core.publisher.Mono;

import zk.rgw.dashboard.framework.security.Role;
import zk.rgw.dashboard.web.bean.entity.User;
import zk.rgw.http.context.ReactiveRequestContextHolder;

public class ContextUtil {

    private static final String ATTR_NAME_PRINCIPAL = "__principal";

    public static final Mono<User> ANONYMOUS_USER;

    static {
        User user = new User();
        ANONYMOUS_USER = Mono.just(user);
    }

    private ContextUtil() {
    }

    public static Mono<User> getUser() {
        return ReactiveRequestContextHolder.getContext()
                .flatMap(requestContext -> requestContext.getAttributeOrDefault(ATTR_NAME_PRINCIPAL, ANONYMOUS_USER));
    }

    public static void setUser(Mono<User> principal) {
        Objects.requireNonNull(principal);
        ReactiveRequestContextHolder.getContext().doOnNext(requestContext -> requestContext.getAttributes().put(ATTR_NAME_PRINCIPAL, principal));
    }

    public static Mono<Boolean> hasRoles(Role... roles) {
        return getUser().map(principal -> {
            Set<Role> roleSet = principal.roleSet();
            if (Objects.isNull(roleSet) || roleSet.isEmpty()) {
                return false;
            }
            for (Role r : roles) {
                if (!roleSet.contains(r)) {
                    return false;
                }
            }
            return false;
        }).switchIfEmpty(Mono.just(false));
    }

}
