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

package zk.rgw.plugin.filter.clientip;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class IpUtilTest {

    @Test
    void testCheckIpV4() {
        Assertions.assertTrue(IpUtil.isIpV4("1.2.3.4"));
        Assertions.assertTrue(IpUtil.isIpV4("127.0.0.1"));
        Assertions.assertTrue(IpUtil.isIpV4("0.0.0.0"));
        Assertions.assertTrue(IpUtil.isIpV4("255.255.255.255"));

        Assertions.assertFalse(IpUtil.isIpV4("127.0.0.256"));
        Assertions.assertFalse(IpUtil.isIpV4("256.0.0.1"));
        Assertions.assertFalse(IpUtil.isIpV4("127.256.0.1"));
        Assertions.assertFalse(IpUtil.isIpV4("127.0.256.1"));
    }

    @Test
    void testCheckIpV4Subnet() {
        Assertions.assertTrue(IpUtil.isIpV4Subnet("1.2.3.4/0"));
        Assertions.assertTrue(IpUtil.isIpV4Subnet("1.2.3.4/20"));
        Assertions.assertTrue(IpUtil.isIpV4Subnet("1.2.3.4/32"));

        Assertions.assertFalse(IpUtil.isIpV4Subnet("1.2.3.4/-1"));
        Assertions.assertFalse(IpUtil.isIpV4Subnet("1.2.3.4/33"));
    }

    @Test
    void testCheckIpV4Range() {
        Assertions.assertTrue(IpUtil.isIpV4Range("127.0.0.1-127.0.0.100"));

        Assertions.assertFalse(IpUtil.isIpV4Range("127.0.0.1-127.0.0.256"));
        Assertions.assertFalse(IpUtil.isIpV4Range("127.0.0.256-128.0.0.1"));
    }

}
