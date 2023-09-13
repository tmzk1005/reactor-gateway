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
package zk.rgw.plugin.filter.proxyhttp;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("此测试类是为了手动执行，学习ScriptEngin的API")
class ScriptEnginTest {

    @Test
    void test() throws Exception {
        ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName("groovy");
        scriptEngine.put("name", "alice");
        scriptEngine.eval("def getName() { return name }");

        Invocable invocable = (Invocable) scriptEngine;

        Object result = invocable.invokeFunction("getName");
        Assertions.assertTrue(result instanceof String);
        Assertions.assertEquals("alice", result);

        scriptEngine.put("name", "bob");
        result = invocable.invokeFunction("getName");
        Assertions.assertEquals("bob", result);

    }

}
