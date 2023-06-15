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

package zk.rgw.dashboard.framework.filter.mvc;

import java.util.Objects;

import zk.rgw.common.util.ObjectUtil;

public class StringValueParseUtil {

    private StringValueParseUtil() {
    }

    public static Object parse(Class<?> type, String value) throws IllegalArgumentException {
        if (Objects.isNull(value)) {
            return null;
        }
        if (type.isEnum()) {
            return parseStringToEnum(type, value);
        }
        String typeName = type.getName();
        return switch (typeName) {
            case "int", "java.lang.Integer" -> Integer.parseInt(value);
            case "long", "java.lang.Long" -> Long.parseLong(value);
            case "double", "java.lang.Double" -> Double.parseDouble(value);
            case "boolean", "java.lang.Boolean" -> Boolean.parseBoolean(value);
            // support all number types ? just int long double for now
            default -> value;
        };
    }

    private static <T extends Enum<T>> T parseStringToEnum(Class<?> type, String value) throws IllegalArgumentException {
        if (ObjectUtil.isEmpty(value)) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Class<T> enumType = (Class<T>) type;
        return Enum.valueOf(enumType, value);
    }

}
