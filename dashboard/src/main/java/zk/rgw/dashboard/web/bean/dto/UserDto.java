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

package zk.rgw.dashboard.web.bean.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;

import zk.rgw.dashboard.framework.security.Role;
import zk.rgw.dashboard.framework.validate.NotBlank;
import zk.rgw.dashboard.framework.validate.Pattern;
import zk.rgw.dashboard.framework.validate.Size;
import zk.rgw.dashboard.framework.validate.ValidatableDto;
import zk.rgw.dashboard.utils.Patters;

@Getter
@Setter
public class UserDto implements ValidatableDto {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 32, message = "用户名不能超过32个字符长度，且最少需要3个字符长度")
    @Pattern(regexp = Patters.IDENTIFIER, message = "组织名称只能包含字母，数字和下划线")
    private String username;

    @NotBlank(message = "用户昵称不能为空")
    @Size(min = 3, max = 32, message = "用户昵称不能超过32个字符长度，且最少需要3个字符长度")
    @Pattern(regexp = Patters.IDENTIFIER_ZH, message = "用户昵称只能包含字母，数字和下划线，以及中文字符")
    private String nickname;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 32, message = "密码不能超过32个字符长度，且最少需要8个字符长度")
    private String password;

    private Role role;

    @NotBlank(message = "组织ID不能为空")
    private String organizationId;

    @Size(max = 24, message = "用户电话号码不能超过24个字符长度")
    private String phone;

    @Size(max = 56, message = "用户email不能超过56个字符长度")
    private String email;

    @Size(max = 256, message = "用户地址不能超过256个字符长度")
    private String address;

    @Override
    public List<String> validate() {
        List<String> errMessages = new ArrayList<>(1);
        if (Objects.isNull(role)) {
            errMessages.add("用户角色不能为空");
        }
        try {
            new ObjectId(organizationId);
        } catch (Exception exception) {
            errMessages.add("组织ID非法：不是一个合法的ObjectId");
        }
        return errMessages;
    }
}
