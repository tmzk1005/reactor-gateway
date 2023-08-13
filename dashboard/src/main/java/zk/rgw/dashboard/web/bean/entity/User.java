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

package zk.rgw.dashboard.web.bean.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.BsonType;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.bson.codecs.pojo.annotations.BsonRepresentation;

import zk.rgw.dashboard.framework.mongodb.Document;
import zk.rgw.dashboard.framework.mongodb.DocumentReference;
import zk.rgw.dashboard.framework.mongodb.Index;
import zk.rgw.dashboard.framework.security.Role;
import zk.rgw.dashboard.framework.xo.BaseAuditableEntity;
import zk.rgw.dashboard.web.bean.dto.UserDto;

@Getter
@Setter
@Document(collection = "User")
@Index(name = "UserIndex-username", unique = true, def = "{\"username\": 1}")
@NoArgsConstructor
public class User extends BaseAuditableEntity<UserDto> {

    @BsonId
    @BsonRepresentation(BsonType.OBJECT_ID)
    private String id;

    private String username;

    private String nickname;

    private String password;

    private Role role;

    @DocumentReference
    private Organization organization;

    private String phone;

    private String email;

    private String address;

    private boolean enabled = true;

    private boolean deleted = false;

    @SuppressWarnings("unchecked")
    @Override
    public User initFromDto(UserDto dto) {
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setNickname(dto.getNickname());
        user.setPassword(dto.getPassword());
        user.setOrganization(new Organization(dto.getOrganizationId()));
        user.setRole(dto.getRole());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setAddress(dto.getAddress());
        return user;
    }

    @BsonIgnore
    public boolean isSystemAdmin() {
        return Role.SYSTEM_ADMIN == this.role;
    }

}
