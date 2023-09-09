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
package zk.rgw.common.util;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class GzipCompressUtilTest {

    @Test
    void test() {
        String content = "hello";
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        byte[] compressed = GzipCompressUtil.compress(bytes);
        byte[] decompress = GzipCompressUtil.decompress(compressed);
        Assertions.assertEquals(content, new String(decompress));
    }

}
