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
import java.util.HashMap;
import java.util.Map;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.HttpHeaderNames;
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

    private static final String FUNC_NAME = "convert";

    @Getter
    @Setter
    private String convertFuncDef;

    @Getter
    @Setter
    private boolean useObjectMapper;

    @Getter
    @Setter
    private boolean useRequestHeaders;

    @Override
    public Mono<Void> filter(Exchange exchange, FilterChain chain) {
        MyRequestDecorator myRequestDecorator = new MyRequestDecorator(exchange.getRequest());
        Exchange modifiedExchange = exchange.mutate().request(myRequestDecorator).build();
        return myRequestDecorator.modifyBodyAndUpdateContentLength().then(chain.filter(modifiedExchange));
    }

    private class MyRequestDecorator extends HttpServerRequestDecorator {

        private byte[] bodyBytes;

        MyRequestDecorator(HttpServerRequest decorator) {
            super(decorator);
        }

        Mono<Void> modifyBodyAndUpdateContentLength() {
            Mono<byte[]> modifiedBytesMono = super.receive().retain().aggregate().asByteArray().map(this::convert).doOnNext(
                    bytes -> {
                        requestHeaders().set(HttpHeaderNames.CONTENT_LENGTH, bytes.length);
                        this.bodyBytes = bytes;
                    }
            );
            return modifiedBytesMono.then();
        }

        @Override
        public @NonNull ByteBufFlux receive() {
            return ByteBufFlux.fromInbound(Mono.just(bodyBytes));
        }

        private byte[] convert(byte[] rawBodyData) throws RgwRuntimeException {
            ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName(ENGINE_NAME);

            if (useObjectMapper) {
                scriptEngine.put("objectMapper", new ObjectMapper());
            }

            if (useRequestHeaders) {
                Map<String, String> requestHeaders = new HashMap<>();
                requestHeaders().forEach(entry -> requestHeaders.put(entry.getKey(), entry.getValue()));
                scriptEngine.put("requestHeaders", requestHeaders);
            }

            scriptEngine.put("rawRequestBody", rawBodyData);

            try {
                scriptEngine.eval(convertFuncDef);
            } catch (ScriptException exception) {
                String msg = "请求体修改插件异常：解析groovy方法定义失败";
                log.error("{}", msg, exception);
                throw new RgwRuntimeException(msg);
            }

            Invocable invocable = (Invocable) scriptEngine;

            Object result;

            try {
                result = invocable.invokeFunction(FUNC_NAME);
            } catch (Exception exception) {
                String msg = "请求体修改插件异常：执行convert方法异常";
                log.error("{}", msg, exception);
                throw new RgwRuntimeException(msg);
            }

            if (result instanceof CharSequence charSequence) {
                return charSequence.toString().getBytes(StandardCharsets.UTF_8);
            } else if (result instanceof byte[] bytes) {
                return bytes;
            } else {
                try {
                    return JsonUtil.toJson(result).getBytes(StandardCharsets.UTF_8);
                } catch (JsonProcessingException exception) {
                    String msg = "请求体修改插件异常：脚本结果Json序列化失败";
                    log.error("{}", msg, exception);
                    throw new RgwRuntimeException(msg);
                }
            }
        }

    }

}
