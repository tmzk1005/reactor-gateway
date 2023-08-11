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

package zk.rgw.dashboard.utils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class BeanUtil {

    private BeanUtil() {
    }

    public static List<String> allFieldNames(Class<?> clazz) {
        return allFieldNames0(clazz).stream().distinct().toList();
    }

    private static List<String> allFieldNames0(Class<?> clazz) {
        List<String> fieldNames = new ArrayList<>();
        Arrays.stream(clazz.getDeclaredFields()).map(Field::getName).forEach(fieldNames::add);
        if (!clazz.equals(Object.class)) {
            fieldNames.addAll(allFieldNames0(clazz.getSuperclass()));
        }
        return fieldNames;
    }

    public static List<String> allFieldNamesExclude(Class<?> clazz, Set<String> excludes) {
        List<String> fieldNames = new ArrayList<>();
        allFieldNames0(clazz).stream().distinct().filter(fn -> !excludes.contains(fn)).forEach(fieldNames::add);
        return fieldNames;
    }

}
