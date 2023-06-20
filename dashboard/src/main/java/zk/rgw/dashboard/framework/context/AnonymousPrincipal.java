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

import lombok.Getter;

import zk.rgw.dashboard.framework.security.PrincipalWithRoles;
import zk.rgw.dashboard.framework.security.Role;

@Getter
public class AnonymousPrincipal implements PrincipalWithRoles {

    private static final String DEFAULT_NAME = "__anonymous__";

    private final String name;

    public AnonymousPrincipal() {
        this(DEFAULT_NAME);
    }

    public AnonymousPrincipal(String name) {
        this.name = name;
    }

    @Override
    public Role[] getRoles() {
        return new Role[0];
    }

}
