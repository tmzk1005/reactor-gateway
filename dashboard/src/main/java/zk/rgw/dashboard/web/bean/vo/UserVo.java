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

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import zk.rgw.dashboard.framework.security.Role;
import zk.rgw.dashboard.framework.xo.Vo;
import zk.rgw.dashboard.web.bean.entity.Organization;
import zk.rgw.dashboard.web.bean.entity.User;

@Getter
@Setter
public class UserVo extends TimeAuditableVo implements Vo<User> {

    private String id;

    private String username;

    private String nickname;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Role role;

    private String organizationId;

    private String organizationName;

    private String phone;

    private String email;

    private String address;

    private boolean enabled;

    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getRoleDisplay() {
        if (Objects.isNull(role)) {
            return null;
        }
        switch (role) {
            case SYSTEM_ADMIN -> {
                return "系统管理员";
            }
            case NORMAL_USER -> {
                return "普通用户";
            }
            case ORGANIZATION_ADMIN -> {
                return "组织管理员";
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public UserVo initFromPo(User user) {
        this.setId(user.getId());
        this.setUsername(user.getUsername());
        this.setNickname(user.getNickname());
        this.setRole(user.getRole());
        Organization organization = user.getOrganization();
        if (Objects.nonNull(organization)) {
            this.organizationId = organization.getId();
            this.organizationName = organization.getName();
        }
        this.setPhone(user.getPhone());
        this.setEmail(user.getEmail());
        this.setAddress(user.getAddress());
        this.setEnabled(user.isEnabled());

        copyTimeAuditInfo(user);
        return this;
    }

}
