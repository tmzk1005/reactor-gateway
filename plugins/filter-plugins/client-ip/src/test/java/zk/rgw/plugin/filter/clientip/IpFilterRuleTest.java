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

class IpFilterRuleTest {

    static final String IPS = "192.168.1.1-192.168.1.100;"
            + "127.0.0.100-127.0.0.200;"
            + "1.2.3.4;"
            + "6.7.8.9;"
            + "10.0.50.100/16;"
            + "20.99.180.100/20";

    static final IpFilterRule RULE_ACCEPT = new IpFilterRule(new IpFilterRuleConf(IPS));

    static final IpFilterRule RULE_REJECT = new IpFilterRule(new IpFilterRuleConf(IPS, IpFilterRuleType.REJECT));

    @Test
    void testAcceptTrue() {
        Assertions.assertTrue(RULE_ACCEPT.acceptIp("192.168.1.1"));
        Assertions.assertTrue(RULE_ACCEPT.acceptIp("192.168.1.50"));
        Assertions.assertTrue(RULE_ACCEPT.acceptIp("192.168.1.100"));

        Assertions.assertTrue(RULE_ACCEPT.acceptIp("127.0.0.100"));
        Assertions.assertTrue(RULE_ACCEPT.acceptIp("127.0.0.150"));
        Assertions.assertTrue(RULE_ACCEPT.acceptIp("127.0.0.200"));

        Assertions.assertTrue(RULE_ACCEPT.acceptIp("1.2.3.4"));
        Assertions.assertTrue(RULE_ACCEPT.acceptIp("6.7.8.9"));

        Assertions.assertTrue(RULE_ACCEPT.acceptIp("10.0.0.0"));
        Assertions.assertTrue(RULE_ACCEPT.acceptIp("10.0.255.255"));

        Assertions.assertTrue(RULE_ACCEPT.acceptIp("20.96.0.0"));
        Assertions.assertTrue(RULE_ACCEPT.acceptIp("20.96.255.255"));
        Assertions.assertTrue(RULE_ACCEPT.acceptIp("20.111.255.255"));
    }

    @Test
    void testAcceptFalse() {
        // 验证上下边界条件
        Assertions.assertFalse(RULE_ACCEPT.acceptIp("192.168.1.0"));
        Assertions.assertFalse(RULE_ACCEPT.acceptIp("192.168.1.101"));

        Assertions.assertFalse(RULE_ACCEPT.acceptIp("127.0.0.99"));
        Assertions.assertFalse(RULE_ACCEPT.acceptIp("127.0.0.201"));

        Assertions.assertFalse(RULE_ACCEPT.acceptIp("1.2.3.5"));
        Assertions.assertFalse(RULE_ACCEPT.acceptIp("6.7.8.5"));

        Assertions.assertFalse(RULE_ACCEPT.acceptIp("9.255.255.255"));
        Assertions.assertFalse(RULE_ACCEPT.acceptIp("10.1.255.255"));

        Assertions.assertFalse(RULE_ACCEPT.acceptIp("20.95.0.0"));
        Assertions.assertFalse(RULE_ACCEPT.acceptIp("20.95.255.255"));
        Assertions.assertFalse(RULE_ACCEPT.acceptIp("20.112.255.255"));
    }

    @Test
    void testRejectFalse() {
        Assertions.assertFalse(RULE_REJECT.acceptIp("192.168.1.1"));
        Assertions.assertFalse(RULE_REJECT.acceptIp("192.168.1.50"));
        Assertions.assertFalse(RULE_REJECT.acceptIp("192.168.1.100"));

        Assertions.assertFalse(RULE_REJECT.acceptIp("127.0.0.100"));
        Assertions.assertFalse(RULE_REJECT.acceptIp("127.0.0.150"));
        Assertions.assertFalse(RULE_REJECT.acceptIp("127.0.0.200"));

        Assertions.assertFalse(RULE_REJECT.acceptIp("1.2.3.4"));
        Assertions.assertFalse(RULE_REJECT.acceptIp("6.7.8.9"));

        Assertions.assertFalse(RULE_REJECT.acceptIp("10.0.0.0"));
        Assertions.assertFalse(RULE_REJECT.acceptIp("10.0.255.255"));

        Assertions.assertFalse(RULE_REJECT.acceptIp("20.96.0.0"));
        Assertions.assertFalse(RULE_REJECT.acceptIp("20.96.255.255"));
        Assertions.assertFalse(RULE_REJECT.acceptIp("20.111.255.255"));
    }

    @Test
    void testRejectTrue() {
        Assertions.assertTrue(RULE_REJECT.acceptIp("192.168.1.0"));
        Assertions.assertTrue(RULE_REJECT.acceptIp("192.168.1.101"));

        Assertions.assertTrue(RULE_REJECT.acceptIp("127.0.0.99"));
        Assertions.assertTrue(RULE_REJECT.acceptIp("127.0.0.201"));

        Assertions.assertTrue(RULE_REJECT.acceptIp("1.2.3.5"));
        Assertions.assertTrue(RULE_REJECT.acceptIp("6.7.8.5"));

        Assertions.assertTrue(RULE_REJECT.acceptIp("9.255.255.255"));
        Assertions.assertTrue(RULE_REJECT.acceptIp("10.1.255.255"));

        Assertions.assertTrue(RULE_REJECT.acceptIp("20.95.0.0"));
        Assertions.assertTrue(RULE_REJECT.acceptIp("20.95.255.255"));
        Assertions.assertTrue(RULE_REJECT.acceptIp("20.112.255.255"));
    }

}
