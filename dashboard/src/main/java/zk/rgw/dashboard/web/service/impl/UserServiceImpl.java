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

package zk.rgw.dashboard.web.service.impl;

import reactor.core.publisher.Mono;

import zk.rgw.dashboard.web.bean.Role;
import zk.rgw.dashboard.web.bean.dto.LoginDto;
import zk.rgw.dashboard.web.bean.entity.User;
import zk.rgw.dashboard.web.service.UserService;

public class UserServiceImpl implements UserService {

    @Override
    public Mono<User> login(LoginDto loginDto) {
        String expectName = "alice";
        if (!expectName.equals(loginDto.getUsername())) {
            return Mono.empty();
        }
        User user = new User();
        user.setName(expectName);
        user.setNickname(expectName.toUpperCase());
        user.setRole(Role.NORMAL_USER);
        user.setOrganizationId("123");
        return Mono.just(user);
    }

}
