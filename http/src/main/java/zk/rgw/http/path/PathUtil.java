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

package zk.rgw.http.path;

import java.nio.file.Paths;

public class PathUtil {

    public static final String SLASH = "/";

    private PathUtil() {
    }

    public static String normalize(String path) {
        String newPath = Paths.get(path).normalize().toString();
        if (newPath.length() == 0) {
            return SLASH;
        }
        if (!newPath.startsWith(SLASH)) {
            newPath = SLASH + newPath;
        }
        return newPath;
    }

    public static String constantPrefix(String normalizePath) {
        int slashIndex = 0;
        boolean foundWildcardChar = false;
        for (int i = 0; i < normalizePath.length(); ++i) {
            char c = normalizePath.charAt(i);
            if (c == '*' || c == '?' || c == '{') {
                foundWildcardChar = true;
                break;
            } else if (c == '/') {
                slashIndex = i;
            }
        }
        if (!foundWildcardChar) {
            return normalizePath;
        }
        if (slashIndex == 0) {
            return SLASH;
        }
        return normalizePath.substring(0, slashIndex);
    }

    public static String removeLast(String normalizePath) {
        if ("".equals(normalizePath)) {
            return "";
        }
        int index = normalizePath.length() - 1;
        while (index > 0 && normalizePath.charAt(index) != '/') {
            --index;
        }
        return normalizePath.substring(0, index);
    }

}
