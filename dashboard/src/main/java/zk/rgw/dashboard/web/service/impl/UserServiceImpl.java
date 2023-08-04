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

import java.util.Objects;

import com.mongodb.client.model.Filters;
import reactor.core.publisher.Mono;

import zk.rgw.dashboard.framework.security.hash.Pbkdf2PasswordEncoder;
import zk.rgw.dashboard.web.bean.Page;
import zk.rgw.dashboard.web.bean.PageData;
import zk.rgw.dashboard.web.bean.dto.LoginDto;
import zk.rgw.dashboard.web.bean.entity.User;
import zk.rgw.dashboard.web.repository.UserRepository;
import zk.rgw.dashboard.web.repository.factory.RepositoryFactory;
import zk.rgw.dashboard.web.service.UserService;

public class UserServiceImpl implements UserService {

    private final UserRepository userRepository = RepositoryFactory.get(UserRepository.class);

    @Override
    public Mono<User> login(LoginDto loginDto) {
        return userRepository.findOneByUsername(loginDto.getUsername())
                .filter(user -> passwordMatch(loginDto.getPassword(), user.getPassword()));
    }

    @Override
    public Mono<PageData<User>> listUsers(int pageNum, int pageSize) {
        return userRepository.find(Filters.empty(), null, Page.of(pageNum, pageSize));
    }

    private static boolean passwordMatch(String dtoPassword, String hashedPassword) {
        if (Objects.isNull(dtoPassword) || Objects.isNull(hashedPassword)) {
            return false;
        }
        return Pbkdf2PasswordEncoder.getDefaultInstance().matches(dtoPassword, hashedPassword);
    }

}
