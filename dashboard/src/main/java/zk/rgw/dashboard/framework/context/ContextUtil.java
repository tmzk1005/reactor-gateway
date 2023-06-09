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

import zk.rgw.dashboard.framework.security.PrincipalWithRoles;
import zk.rgw.dashboard.framework.security.Role;
import zk.rgw.http.context.ReactiveRequestContextHolder;

public class ContextUtil {

    private static final String ATTR_NAME_PRINCIPAL = "__principal";

    public static final Mono<PrincipalWithRoles> ANONYMOUS_PRINCIPAL = Mono.just(new AnonymousPrincipal());

    private ContextUtil() {
    }

    public static Mono<PrincipalWithRoles> getPrincipal() {
        return ReactiveRequestContextHolder.getContext()
                .flatMap(requestContext -> requestContext.getAttributeOrDefault(ATTR_NAME_PRINCIPAL, ANONYMOUS_PRINCIPAL));
    }

    public static void setPrincipal(Mono<PrincipalWithRoles> principal) {
        Objects.requireNonNull(principal);
        ReactiveRequestContextHolder.getContext().doOnNext(requestContext -> requestContext.getAttributes().put(ATTR_NAME_PRINCIPAL, principal));
    }

    public static Mono<Boolean> hasRoles(Role... roles) {
        return getPrincipal().map(principal -> {
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
