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

package zk.rgw.dashboard.web.service;

import reactor.core.publisher.Mono;

import zk.rgw.dashboard.framework.security.HasRole;
import zk.rgw.dashboard.framework.security.Role;
import zk.rgw.dashboard.web.bean.PageData;
import zk.rgw.dashboard.web.bean.dto.LoginDto;
import zk.rgw.dashboard.web.bean.dto.UserDto;
import zk.rgw.dashboard.web.bean.dto.UserModifiableDto;
import zk.rgw.dashboard.web.bean.entity.User;

public interface UserService {

    Mono<User> login(LoginDto loginDto);

    Mono<User> curSessionUser();

    @HasRole({ Role.SYSTEM_ADMIN, Role.ORGANIZATION_ADMIN })
    Mono<PageData<User>> listUsers(int pageNum, int pageSize);

    @HasRole(Role.SYSTEM_ADMIN)
    Mono<User> createUser(UserDto userDto);

    Mono<User> updateBaseInfo(String userId, UserModifiableDto userModifiableDto);

    @HasRole({ Role.SYSTEM_ADMIN, Role.ORGANIZATION_ADMIN })
    Mono<Void> enableUser(String userId);

    @HasRole({ Role.SYSTEM_ADMIN, Role.ORGANIZATION_ADMIN })
    Mono<Void> disableUser(String userId);

    @HasRole(Role.SYSTEM_ADMIN)
    Mono<Void> deleteUser(String userId);

    Mono<Void> updatePassword(String oldPassword, String newPassword);

}
