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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PathUtilTest {

    @Test
    void testNormalize() {
        Assertions.assertEquals("/", PathUtil.normalize(""));
        Assertions.assertEquals("/foo/bar", PathUtil.normalize("/foo/bar"));
        Assertions.assertEquals("/foo/{bar}", PathUtil.normalize("/foo/{bar}"));
        Assertions.assertEquals("/foo/{bar}/", PathUtil.normalize("/foo/{bar}/"));
        Assertions.assertEquals("/foo/bar", PathUtil.normalize("/foo/./bar"));
        Assertions.assertEquals("/foo/bar/", PathUtil.normalize("/foo/bar/"));
        Assertions.assertEquals("/foo/bar/", PathUtil.normalize("/foo/./bar/"));
        Assertions.assertEquals("/{bar}/", PathUtil.normalize("/foo/../{bar}/"));
        Assertions.assertEquals("/bar/", PathUtil.normalize("/foo/../bar/"));
    }

    @Test
    void testConstantPrefix() {
        Assertions.assertEquals("/foo/bar", PathUtil.constantPrefix("/foo/bar"));
        Assertions.assertEquals("/foo/bar", PathUtil.constantPrefix("/foo/bar/{name}/info"));
    }

    @Test
    void testRemoveLast() {
        Assertions.assertEquals("/foo", PathUtil.removeLast("/foo/bar"));
        Assertions.assertEquals("", PathUtil.removeLast("/"));
    }

}
