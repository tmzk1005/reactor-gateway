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
package zk.rgw.dashboard.web.bean.vo;

import lombok.Getter;
import lombok.Setter;

import zk.rgw.dashboard.framework.security.Role;
import zk.rgw.dashboard.framework.security.UserJwtUtil;
import zk.rgw.dashboard.framework.xo.Vo;
import zk.rgw.dashboard.web.bean.entity.User;

@Getter
@Setter
public class LoginVo implements Vo<User> {

    private String id;

    private String username;

    private String nickname;

    private Role role;

    private String organizationId;

    private String organizationName;

    private String jwtToken;

    private int jwtExpireSeconds;

    @SuppressWarnings("unchecked")
    @Override
    public LoginVo initFromPo(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.nickname = user.getNickname();
        this.role = user.getRole();
        this.organizationId = user.getOrganization().getId();
        this.organizationName = user.getOrganization().getName();
        this.jwtToken = UserJwtUtil.encode(user);
        this.jwtExpireSeconds = UserJwtUtil.getJwtExpireSeconds();
        return this;
    }

}
