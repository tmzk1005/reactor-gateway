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
package zk.rgw.plugin.filter.modifyresponsebody;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.NettyOutbound;
import reactor.netty.http.server.HttpServerResponse;
import reactor.util.annotation.NonNull;

import zk.rgw.common.exception.RgwRuntimeException;
import zk.rgw.common.util.GzipCompressUtil;
import zk.rgw.common.util.JsonUtil;
import zk.rgw.plugin.api.Exchange;
import zk.rgw.plugin.api.HttpServerResponseDecorator;
import zk.rgw.plugin.api.filter.FilterChain;
import zk.rgw.plugin.api.filter.JsonConfFilterPlugin;
import zk.rgw.plugin.exception.PluginConfException;
import zk.rgw.plugin.util.ExchangeUtil;

@Slf4j
public class ModifyResponseBodyFilter implements JsonConfFilterPlugin {

    private static final String TAG = "修改响应体";

    private static final String ENGINE_NAME = "groovy";

    private static final String FUNC_NAME = "convert";

    @Getter
    @Setter
    private String convertFuncDef;

    @JsonIgnore
    private ScriptEngine scriptEngine;

    @Override
    public Mono<Void> filter(Exchange exchange, FilterChain chain) {
        MyResponseDecorator myResponseDecorator = new MyResponseDecorator(exchange.getResponse());
        Exchange modifiedExchange = exchange.mutate().response(myResponseDecorator).build();
        return chain.filter(modifiedExchange).doOnSuccess(ignore -> ExchangeUtil.addAuditTag(exchange, TAG));
    }

    @Override
    public void afterConfigured() throws PluginConfException {
        scriptEngine = new ScriptEngineManager().getEngineByName(ENGINE_NAME);
        try {
            scriptEngine.eval(convertFuncDef);
        } catch (ScriptException exception) {
            String msg = "响应体修改插件配置失败：解析groovy方法定义失败";
            log.error("{}", msg, exception);
            throw new PluginConfException(msg);
        }
    }

    private class MyResponseDecorator extends HttpServerResponseDecorator {

        private boolean modified = false;

        MyResponseDecorator(HttpServerResponse delegator) {
            super(delegator);
        }

        @Override
        public @NonNull NettyOutbound send(@NonNull Publisher<? extends ByteBuf> dataStream) {
            if (!modified) {
                ByteBufFlux byteBufFlux = modifyResponseBody(dataStream);
                modified = true;
                return super.send(byteBufFlux);
            } else {
                return super.send(dataStream);
            }
        }

        @Override
        public @NonNull NettyOutbound send(@NonNull Publisher<? extends ByteBuf> dataStream, @NonNull Predicate<ByteBuf> predicate) {
            if (!modified) {
                ByteBufFlux byteBufFlux = modifyResponseBody(dataStream);
                modified = true;
                return super.send(byteBufFlux, predicate);
            } else {
                return super.send(dataStream, predicate);
            }
        }

        private ByteBufFlux modifyResponseBody(Publisher<? extends ByteBuf> dataStream) {
            responseHeaders().remove(HttpHeaderNames.CONTENT_LENGTH);
            responseHeaders().set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
            Mono<byte[]> modifiedBytesMono = ByteBufFlux.fromInbound(dataStream).retain().aggregate().asByteArray().map(this::convert);
            return ByteBufFlux.fromInbound(modifiedBytesMono);
        }

        private boolean isRawResponseBodyGzip() {
            String contentEncoding = responseHeaders().get(HttpHeaderNames.CONTENT_ENCODING);
            return Objects.nonNull(contentEncoding) && HttpHeaderValues.GZIP.contentEquals(contentEncoding);
        }

        private byte[] convert(byte[] rawBodyData) throws RgwRuntimeException {
            Map<String, String> headers = new HashMap<>();
            responseHeaders().forEach(entry -> headers.put(entry.getKey(), entry.getValue()));
            scriptEngine.put("responseHeaders", headers);

            boolean isGzip = isRawResponseBodyGzip();
            scriptEngine.put("rawResponseBody", isGzip ? GzipCompressUtil.decompress(rawBodyData) : rawBodyData);

            Object result;

            try {
                result = ((Invocable) scriptEngine).invokeFunction(FUNC_NAME);
            } catch (Exception exception) {
                String msg = "响应体修改插件异常：执行convert方法异常";
                log.error("{}", msg, exception);
                throw new RgwRuntimeException(msg);
            }

            byte[] modifiedBytes;

            if (result instanceof CharSequence charSequence) {
                modifiedBytes = charSequence.toString().getBytes(StandardCharsets.UTF_8);
            } else if (result instanceof byte[] bytes) {
                modifiedBytes = bytes;
            } else {
                try {
                    modifiedBytes = JsonUtil.toJson(result).getBytes(StandardCharsets.UTF_8);
                } catch (JsonProcessingException exception) {
                    String msg = "响应体修改插件异常：脚本结果Json序列化失败";
                    log.error("{}", msg, exception);
                    throw new RgwRuntimeException(msg);
                }
            }

            return isGzip ? GzipCompressUtil.compress(modifiedBytes) : modifiedBytes;
        }

    }

}
