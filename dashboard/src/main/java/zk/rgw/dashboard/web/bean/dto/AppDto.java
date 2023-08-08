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

import lombok.Getter;
import lombok.Setter;

import zk.rgw.dashboard.framework.validate.NotBlank;
import zk.rgw.dashboard.framework.validate.Pattern;
import zk.rgw.dashboard.framework.validate.Size;
import zk.rgw.dashboard.framework.validate.ValidatableDto;
import zk.rgw.dashboard.utils.Patters;

@Getter
@Setter
public class AppDto implements ValidatableDto {

    @NotBlank(message = "应用名称不能为空")
    @Size(min = 3, max = 32, message = "应用名称不能超过32个字符长度，且最少需要3个字符长度")
    @Pattern(regexp = Patters.IDENTIFIER_ZH, message = "应用名称只能包含字母，数字和下划线以及中文字符")
    private String name;

    @Size(max = 256, message = "应用描述不能超过的256个字符长度")
    private String description;

}
