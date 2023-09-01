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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class EnvNameExtractUtilTest {

    @Test
    void test() {
        List<String> envNames = EnvNameExtractUtil.extract("hello world");
        Assertions.assertEquals(0, envNames.size());

        envNames = EnvNameExtractUtil.extract("http://{domain}/foo/bar");
        Assertions.assertEquals(1, envNames.size());
        Assertions.assertEquals("domain", envNames.get(0));

        envNames = EnvNameExtractUtil.extract("http://{domain}/foo/{bar}");
        Assertions.assertEquals(2, envNames.size());
        Assertions.assertEquals("domain", envNames.get(0));
        Assertions.assertEquals("bar", envNames.get(1));
    }

}
