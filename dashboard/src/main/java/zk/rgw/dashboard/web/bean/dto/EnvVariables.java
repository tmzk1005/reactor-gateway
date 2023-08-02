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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import lombok.Getter;
import lombok.Setter;

import zk.rgw.dashboard.framework.validate.ValidatableDto;
import zk.rgw.dashboard.utils.Patters;
import zk.rgw.dashboard.web.bean.KeyValue;

@Getter
@Setter
public class EnvVariables implements ValidatableDto {

    private Set<KeyValue> keyValues = new HashSet<>();

    @Override
    public List<String> validate() {
        if (Objects.isNull(keyValues) || keyValues.isEmpty()) {
            return List.of("未包含有效的环境变量设置");
        }
        List<String> errMessages = new ArrayList<>(1);
        Pattern compiledPattern = Pattern.compile(Patters.IDENTIFIER);
        for (KeyValue keyValue : keyValues) {
            String key = keyValue.getKey();
            if (key.isBlank()) {
                errMessages.add("环境变量名 " + key + " 非法：不能为空。");
                continue;
            }
            if (key.length() > 32) {
                errMessages.add("环境变量名 " + key + " 非法：不能超过32字符长度。");
                continue;
            }
            if (!compiledPattern.matcher(key).matches()) {
                errMessages.add("环境变量名 " + key + " 非法：只能包含字母数字和下划线。");
            }
        }
        return errMessages;
    }

}
