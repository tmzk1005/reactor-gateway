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
package zk.rgw.dashboard.web.controller;

import reactor.core.publisher.Mono;

import zk.rgw.dashboard.framework.annotation.Controller;
import zk.rgw.dashboard.framework.annotation.RequestBody;
import zk.rgw.dashboard.framework.annotation.RequestMapping;
import zk.rgw.dashboard.framework.exception.BizException;
import zk.rgw.dashboard.web.bean.dto.LoginDto;
import zk.rgw.dashboard.web.bean.vo.UserVo;
import zk.rgw.dashboard.web.service.UserService;
import zk.rgw.dashboard.web.service.factory.ServiceFactory;

@Controller("user")
public class UserController {

    private final UserService userService = ServiceFactory.get(UserService.class);

    @RequestMapping(path = "/_login", method = RequestMapping.Method.POST)
    public Mono<UserVo> login(@RequestBody LoginDto loginDto) {
        return userService.login(loginDto)
                .map(new UserVo()::initFromPo)
                .switchIfEmpty(Mono.error(BizException.of("登录失败!用户名或密码错误.")));
    }

    @RequestMapping(path = "/name")
    public Mono<String> getName() {
        return Mono.just("alice");
    }

}
