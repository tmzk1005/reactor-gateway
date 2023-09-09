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
package zk.rgw.plugin.filter.modifyrequestbody;

import java.nio.charset.StandardCharsets;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.server.HttpServerRequest;
import reactor.util.annotation.NonNull;

import zk.rgw.common.exception.RgwRuntimeException;
import zk.rgw.common.util.JsonUtil;
import zk.rgw.plugin.api.Exchange;
import zk.rgw.plugin.api.HttpServerRequestDecorator;
import zk.rgw.plugin.api.filter.FilterChain;
import zk.rgw.plugin.api.filter.JsonConfFilterPlugin;

@Slf4j
public class ModifyRequestBodyFilter implements JsonConfFilterPlugin {

    private static final String ENGINE_NAME = "groovy";

    @Getter
    @Setter
    private String script;

    @Override
    public Mono<Void> filter(Exchange exchange, FilterChain chain) {
        Exchange modifiedRequest = exchange.mutate().request(new MyRequestDecorator(exchange.getRequest())).build();
        return chain.filter(modifiedRequest);
    }

    private class MyRequestDecorator extends HttpServerRequestDecorator {

        public MyRequestDecorator(HttpServerRequest decorator) {
            super(decorator);
        }

        @Override
        public @NonNull ByteBufFlux receive() {
            Mono<byte[]> modifiedBytesMono = super.receive().retain().aggregate().asByteArray().map(this::convert);
            return ByteBufFlux.fromInbound(modifiedBytesMono);
        }

        private byte[] convert(byte[] rawBodyData) throws RgwRuntimeException {
            ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName(ENGINE_NAME);
            Object object;
            try {
                object = scriptEngine.eval(script);
            } catch (ScriptException exception) {
                String msg = "请求体修改插件异常：执行请求体转换逻辑失败";
                log.error("{}", msg, exception);
                throw new RgwRuntimeException(msg);
            }
            if (object instanceof CharSequence charSequence) {
                return charSequence.toString().getBytes(StandardCharsets.UTF_8);
            } else if (object instanceof byte[] bytes) {
                return bytes;
            } else {
                try {
                    return JsonUtil.toJson(object).getBytes(StandardCharsets.UTF_8);
                } catch (JsonProcessingException exception) {
                    String msg = "请求体修改插件异常：脚本结果Json序列化失败";
                    log.error("{}", msg, exception);
                    throw new RgwRuntimeException(msg);
                }
            }
        }

    }

}
