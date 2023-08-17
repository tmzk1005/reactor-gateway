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

import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import org.bson.BsonDocument;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import reactor.core.publisher.Mono;

import zk.rgw.dashboard.framework.context.ContextUtil;
import zk.rgw.dashboard.framework.exception.AccessDeniedException;
import zk.rgw.dashboard.framework.exception.BizException;
import zk.rgw.dashboard.framework.security.hash.Pbkdf2PasswordEncoder;
import zk.rgw.dashboard.utils.BeanUtil;
import zk.rgw.dashboard.web.bean.Page;
import zk.rgw.dashboard.web.bean.PageData;
import zk.rgw.dashboard.web.bean.dto.LoginDto;
import zk.rgw.dashboard.web.bean.dto.UserDto;
import zk.rgw.dashboard.web.bean.dto.UserModifiableDto;
import zk.rgw.dashboard.web.bean.entity.User;
import zk.rgw.dashboard.web.repository.OrganizationRepository;
import zk.rgw.dashboard.web.repository.UserRepository;
import zk.rgw.dashboard.web.repository.factory.RepositoryFactory;
import zk.rgw.dashboard.web.service.UserService;

public class UserServiceImpl implements UserService {

    private static final List<Bson> LOOKUP;

    static {
        // exclude id 是因为在mongodb实际的字段名是 _id
        // exclude organization 是因为这个字段由 Projections.computed 而来
        final List<String> projectionsInclude = BeanUtil.allFieldNamesExclude(User.class, Set.of("id", "organization"));
        projectionsInclude.add("_id");
        LOOKUP = List.of(
                Aggregates.lookup("Organization", "organization", "_id", "organizationLookup"),
                Aggregates.project(
                        Projections.fields(
                                Projections.include(projectionsInclude),
                                Projections.computed("organization", BsonDocument.parse("{\"$first\": \"$organizationLookup\"}"))
                        )
                )
        );
    }

    private final UserRepository userRepository = RepositoryFactory.get(UserRepository.class);

    private final OrganizationRepository organizationRepository = RepositoryFactory.get(OrganizationRepository.class);

    @Override
    public Mono<User> login(LoginDto loginDto) {
        return userRepository.findOneByUsername(loginDto.getUsername()).map(user -> {
            if (!user.isEnabled()) {
                throw new AccessDeniedException("用户被禁用");
            }
            return user;
        }).filter(user -> passwordMatch(loginDto.getPassword(), user.getPassword()))
                .flatMap(
                        user -> organizationRepository.findOneById(user.getOrganization().getId())
                                .map(organization -> {
                                    user.setOrganization(organization);
                                    return user;
                                })
                );
    }

    @Override
    public Mono<User> curSessionUser() {
        return ContextUtil.getUser().flatMap(user -> userRepository.findOneById(user.getId(), LOOKUP));
    }

    @Override
    public Mono<PageData<User>> listUsers(int pageNum, int pageSize) {
        Bson notDeleted = Filters.eq("deleted", false);

        Mono<Bson> filterByRole = ContextUtil.getUser().map(user -> {
            if (user.isSystemAdmin()) {
                return notDeleted;
            } else if (user.isOrgAdmin()) {
                return Filters.and(notDeleted, Filters.eq("organization", new ObjectId(user.getOrganization().getId())));
            } else {
                throw new AccessDeniedException();
            }
        });

        return filterByRole.flatMap(filter -> userRepository.find(filter, null, Page.of(pageNum, pageSize), LOOKUP));
    }

    @Override
    public Mono<User> createUser(UserDto userDto) {
        return organizationRepository.existsById(userDto.getOrganizationId()).flatMap(orgExists -> {
            if (Boolean.FALSE.equals(orgExists)) {
                return Mono.error(BizException.of("不存在ID为" + userDto.getOrganizationId() + "的组织"));
            } else {
                return userRepository.existByUsername(userDto.getUsername()).flatMap(useExists -> {
                    if (Boolean.TRUE.equals(useExists)) {
                        return Mono.error(BizException.of("已经存在username为" + userDto.getUsername() + "的用户"));
                    } else {
                        User user = new User().initFromDto(userDto);
                        String hashedPassword = Pbkdf2PasswordEncoder.getDefaultInstance().encode(userDto.getPassword());
                        user.setPassword(hashedPassword);
                        return userRepository.insert(user);
                    }
                });
            }
        });
    }

    @Override
    public Mono<User> updateBaseInfo(final String userId, final UserModifiableDto userModifiableDto) {
        Mono<String> finalUserIdMono = ContextUtil.getUser().map(user -> {
            if (Objects.nonNull(userId)) {
                if (!user.isSystemAdmin() && !userId.equals(user.getId())) {
                    throw new AccessDeniedException();
                }
                return userId;
            } else {
                return user.getId();
            }
        });

        return finalUserIdMono.flatMap(
                theUserId -> findUserById(theUserId).flatMap(
                        user -> {
                            user.setNickname(userModifiableDto.getNickname());
                            user.setPhone(userModifiableDto.getPhone());
                            user.setEmail(userModifiableDto.getEmail());
                            user.setAddress(userModifiableDto.getAddress());
                            return userRepository.save(user);
                        }
                )
        );
    }

    private Mono<User> findUserById(String userId) {
        return userRepository.findOneById(userId)
                .filter(user -> !user.isDeleted())
                .switchIfEmpty(Mono.error(BizException.of("用户不存在")));
    }

    @Override
    public Mono<Void> enableUser(String userId) {
        return setUserEnabledStatus(userId, true);
    }

    @Override
    public Mono<Void> disableUser(String userId) {
        return setUserEnabledStatus(userId, false);
    }

    public Mono<Void> setUserEnabledStatus(String userId, boolean enabled) {
        return Mono.zip(
                ContextUtil.getUser(),
                findUserById(userId)
        ).flatMap(tuple2 -> {
            User sessionUser = tuple2.getT1();
            User opUser = tuple2.getT2();

            if (sessionUser.isOrgAdmin() && !sessionUser.getOrganization().getId().equals(opUser.getOrganization().getId())) {
                throw new AccessDeniedException();
            }

            if (opUser.isSystemAdmin() && "admin".equals(opUser.getUsername())) {
                throw new BizException("系统管理员admin不能被禁用");
            }
            opUser.setEnabled(enabled);
            return userRepository.save(opUser);
        }).then();
    }

    @Override
    public Mono<Void> deleteUser(String userId) {
        return findUserById(userId).flatMap(user -> {
            if (user.isSystemAdmin() && "admin".equals(user.getUsername())) {
                throw new BizException("系统管理员admin不能被删除");
            }
            user.setDeleted(true);
            return userRepository.save(user);
        }).then();
    }

    @Override
    public Mono<Void> updatePassword(String oldPassword, String newPassword) {
        return ContextUtil.getUser()
                .flatMap(user -> userRepository.findOneById(user.getId()))
                .flatMap(user -> {
                    if (!user.isEnabled()) {
                        throw new AccessDeniedException("用户被禁用");
                    }
                    if (!passwordMatch(oldPassword, user.getPassword())) {
                        throw new BizException("旧密码错误");
                    }
                    String hashedPassword = Pbkdf2PasswordEncoder.getDefaultInstance().encode(newPassword);
                    user.setPassword(hashedPassword);
                    return userRepository.save(user);
                }).then();
    }

    private static boolean passwordMatch(String dtoPassword, String hashedPassword) {
        if (Objects.isNull(dtoPassword) || Objects.isNull(hashedPassword)) {
            return false;
        }
        return Pbkdf2PasswordEncoder.getDefaultInstance().matches(dtoPassword, hashedPassword);
    }

}
