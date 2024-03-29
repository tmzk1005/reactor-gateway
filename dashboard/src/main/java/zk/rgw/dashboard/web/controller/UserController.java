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
import zk.rgw.dashboard.framework.annotation.PathVariable;
import zk.rgw.dashboard.framework.annotation.RequestBody;
import zk.rgw.dashboard.framework.annotation.RequestMapping;
import zk.rgw.dashboard.framework.annotation.RequestParam;
import zk.rgw.dashboard.framework.exception.BizException;
import zk.rgw.dashboard.framework.validate.PageNum;
import zk.rgw.dashboard.framework.validate.PageSize;
import zk.rgw.dashboard.web.bean.PageData;
import zk.rgw.dashboard.web.bean.dto.LoginDto;
import zk.rgw.dashboard.web.bean.dto.PasswordUpdateDto;
import zk.rgw.dashboard.web.bean.dto.UserDto;
import zk.rgw.dashboard.web.bean.dto.UserModifiableDto;
import zk.rgw.dashboard.web.bean.vo.LoginVo;
import zk.rgw.dashboard.web.bean.vo.UserVo;
import zk.rgw.dashboard.web.service.UserService;
import zk.rgw.dashboard.web.service.factory.ServiceFactory;

@Controller("user")
public class UserController {

    private final UserService userService = ServiceFactory.get(UserService.class);

    @RequestMapping(path = "/_login", method = RequestMapping.Method.POST)
    public Mono<LoginVo> login(@RequestBody LoginDto loginDto) {
        return userService.login(loginDto)
                .map(new LoginVo()::initFromPo)
                .switchIfEmpty(Mono.error(BizException.of("登录失败!用户名或密码错误.")));
    }

    @RequestMapping
    public Mono<PageData<UserVo>> listUsers(
            @PageNum @RequestParam(name = "pageNum", required = false, defaultValue = "1") int pageNum,
            @PageSize @RequestParam(name = "pageSize", required = false, defaultValue = "10") int pageSize
    ) {
        return userService.listUsers(pageNum, pageSize).map(page -> page.map(user -> new UserVo().initFromPo(user)));
    }

    @RequestMapping(method = RequestMapping.Method.POST)
    public Mono<UserVo> createUser(@RequestBody UserDto userDto) {
        return userService.createUser(userDto).map(new UserVo()::initFromPo);
    }

    @RequestMapping(method = RequestMapping.Method.PATCH)
    public Mono<UserVo> updateUser(
            @RequestParam(name = "userId", required = false) String userId,
            @RequestBody UserModifiableDto dto
    ) {
        return userService.updateBaseInfo(userId, dto).map(new UserVo()::initFromPo);
    }

    @RequestMapping(path = "/_enable/{userId}", method = RequestMapping.Method.POST)
    public Mono<Void> enableUser(@PathVariable("userId") String userId) {
        return userService.enableUser(userId);
    }

    @RequestMapping(path = "/_disable/{userId}", method = RequestMapping.Method.POST)
    public Mono<Void> disableUser(@PathVariable("userId") String userId) {
        return userService.disableUser(userId);
    }

    @RequestMapping(path = "/_delete/{userId}", method = RequestMapping.Method.POST)
    public Mono<Void> deleteUser(@PathVariable("userId") String userId) {
        return userService.deleteUser(userId);
    }

    @RequestMapping(path = "/password/_update", method = RequestMapping.Method.POST)
    public Mono<Void> updatePassword(@RequestBody PasswordUpdateDto dto) {
        return userService.updatePassword(dto.getOldPassword(), dto.getNewPassword());
    }

    @RequestMapping(path = "/me")
    public Mono<UserVo> selfDetails() {
        return userService.curSessionUser().map(new UserVo()::initFromPo);
    }

}
