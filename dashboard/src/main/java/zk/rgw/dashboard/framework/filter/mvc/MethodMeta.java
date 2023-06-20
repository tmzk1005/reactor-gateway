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

package zk.rgw.dashboard.framework.filter.mvc;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import zk.rgw.common.exception.RgwRuntimeException;
import zk.rgw.common.util.JsonUtil;
import zk.rgw.dashboard.framework.annotation.PathVariable;
import zk.rgw.dashboard.framework.annotation.RequestBody;
import zk.rgw.dashboard.framework.annotation.RequestParam;
import zk.rgw.dashboard.framework.annotation.ResponseStatus;
import zk.rgw.dashboard.framework.exception.BadRequestException;
import zk.rgw.dashboard.framework.exception.BizException;
import zk.rgw.dashboard.framework.validate.ParameterValidator;
import zk.rgw.http.path.AntPathMatcher;
import zk.rgw.plugin.api.Exchange;
import zk.rgw.plugin.util.ResponseUtil;
import zk.rgw.plugin.util.Shuck;

@Slf4j
public class MethodMeta {

    private final Method method;

    private final Object controller;

    private final String mappingPath;

    private int requestBodyParamIndex = -1;

    private ArgValueSupplier[] argValueSuppliers;

    public MethodMeta(Object controller, Method method, String mappingPath) {
        this.controller = controller;
        this.method = method;
        this.mappingPath = mappingPath;
        initArgValueSuppliers();
    }

    private void initArgValueSuppliers() {
        Parameter[] parameters = method.getParameters();
        argValueSuppliers = new ArgValueSupplier[parameters.length];
        if (parameters.length == 0) {
            return;
        }
        for (int i = 0; i < parameters.length; ++i) {
            PathVariable pathVariableAnn;
            RequestParam requestParamAnn;
            Parameter parameter = parameters[i];
            Class<?> type = parameter.getType();
            if ((pathVariableAnn = parameter.getAnnotation(PathVariable.class)) != null) {
                argValueSuppliers[i] = new PathVariableValueSupplier(pathVariableAnn.value(), type);
            } else if ((requestParamAnn = parameter.getAnnotation(RequestParam.class)) != null) {
                String defaultValue = requestParamAnn.defaultValue();
                if (RequestParam.NULL.equals(defaultValue)) {
                    defaultValue = null;
                }
                boolean required = requestParamAnn.required();
                argValueSuppliers[i] = new RequestParameterValueSupplier(requestParamAnn.name(), type, required, defaultValue);
            } else if (parameter.getAnnotation(RequestBody.class) != null) {
                if (requestBodyParamIndex != -1) {
                    // 有1个以上被@RequestBody修饰的参数,不可以
                    String msg = "More then one @RequestBody used in controller method " + name();
                    throw new IllegalStateException(msg);
                }
                requestBodyParamIndex = i;
                argValueSuppliers[i] = new RequestBodyValueSupplier(type);
            } else if (type.isAssignableFrom(HttpServerRequest.class)) {
                argValueSuppliers[i] = (req, ignore1, ignore2) -> req;
            } else {
                String msg = "Controller method " + name() + " has unsupported parameter type.";
                throw new IllegalStateException(msg);
            }
        }
    }

    public Mono<Void> invoke(Exchange exchange, String requestPath) {
        return parseOutArgs(exchange, requestPath).map(theArgs -> {
            if (theArgs.length > 0) {
                // 校验参数
                ParameterValidator parameterValidator = new ParameterValidator();
                parameterValidator.validate(method.getParameters(), theArgs);
                if (parameterValidator.hasError()) {
                    return Mono.error(new BadRequestException(parameterValidator.getErrorMessage()));
                }
            }
            try {
                return method.invoke(controller, theArgs);
            } catch (Exception exception) {
                return Mono.error(exception);
            }
        }).flatMap(invokeResult -> this.handleInvokeResult(invokeResult, exchange.getResponse()))
                .onErrorResume(throwable -> this.handleException(throwable, exchange));
    }

    private Mono<Object[]> parseOutArgs(Exchange exchange, String requestPath) {
        HttpServerRequest request = exchange.getRequest();
        Map<String, String> pathVariables = AntPathMatcher.getDefaultInstance().extractUriTemplateVariables(mappingPath, requestPath);
        Map<String, List<String>> requestParameters = new QueryStringDecoder(request.uri()).parameters();

        final Object[] args = new Object[argValueSuppliers.length];
        Mono<Object[]> argsMono;

        try {
            for (int i = 0; i < argValueSuppliers.length; ++i) {
                if (i != requestBodyParamIndex) {
                    args[i] = argValueSuppliers[i].get(request, pathVariables, requestParameters);
                }
            }
        } catch (BadRequestException exception) {
            return Mono.error(exception);
        }

        if (requestBodyParamIndex >= 0) {
            Mono<?> requestBodyMono;
            try {
                requestBodyMono = (Mono<?>) argValueSuppliers[requestBodyParamIndex].get(request, null, null);
            } catch (BadRequestException exception) {
                return Mono.error(exception);
            }
            argsMono = requestBodyMono.map(body -> {
                args[requestBodyParamIndex] = body;
                return args;
            });
        } else {
            argsMono = Mono.just(args);
        }
        return argsMono;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Mono<Void> handleInvokeResult(Object invokeResult, HttpServerResponse response) {
        Object finalInvokeResult = invokeResult;
        if (finalInvokeResult instanceof Flux<?> flux) {
            finalInvokeResult = flux.collectList();
        }
        if (finalInvokeResult instanceof Mono mono) {
            // 需要把Mono里的东西序列化为json， 但是这个mono在controller方法返回了Mono<Void>时是empty的，这种情况返回一个RestShuck结构
            return mono.switchIfEmpty(ResponseUtil.sendOk(response))
                    .flatMap(data -> {
                        String jsonContent;
                        try {
                            jsonContent = JsonUtil.toJson(Shuck.success(data));
                        } catch (JsonProcessingException exception) {
                            throw new RgwRuntimeException("", exception);
                        }
                        return ResponseUtil.sendRawJson(response, jsonContent);
                    });
        } else if (finalInvokeResult instanceof CharSequence charSequence) {
            return ResponseUtil.send(response, HttpResponseStatus.OK, HttpHeaderValues.TEXT_PLAIN.toString(), charSequence.toString());
        } else {
            log.error(
                    "Controller method {}.{} returned unsupported type {}",
                    controller.getClass().getName(), method.getName(), finalInvokeResult.getClass().getName()
            );
            return ResponseUtil.sendStatus(response, HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Mono<Void> handleException(Throwable throwable, Exchange exchange) {
        if (throwable instanceof BadRequestException) {
            return ResponseUtil.sendStatus(exchange.getResponse(), HttpResponseStatus.BAD_REQUEST, throwable.getMessage());
        } else if (throwable instanceof BizException bizException) {
            ResponseStatus annotation = bizException.getClass().getAnnotation(ResponseStatus.class);
            HttpResponseStatus httpResponseStatus = Objects.nonNull(annotation) ? HttpResponseStatus.valueOf(annotation.code()) : HttpResponseStatus.OK;
            return ResponseUtil.send(exchange.getResponse(), httpResponseStatus, bizException.getCode(), bizException.getMessage());
        }
        return ResponseUtil.sendStatus(exchange.getResponse(), HttpResponseStatus.INTERNAL_SERVER_ERROR);
    }

    public String name() {
        return controller.getClass().getName() + "." + method.getName();
    }

    interface ArgValueSupplier {

        Object get(HttpServerRequest request, Map<String, String> pathVariables, Map<String, List<String>> requestParameters) throws BadRequestException;

    }

    static class PathVariableValueSupplier implements ArgValueSupplier {

        private final String name;

        private final Class<?> type;

        PathVariableValueSupplier(String name, Class<?> type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public Object get(HttpServerRequest request, Map<String, String> pathVariables, Map<String, List<String>> requestParameters)
                throws BadRequestException {
            try {
                return StringValueParseUtil.parse(type, pathVariables.get(name));
            } catch (Exception exception) {
                throw new BadRequestException("请求参数 " + name + " 数据类型错误");
            }
        }

    }

    static class RequestParameterValueSupplier implements ArgValueSupplier {

        private final String name;

        private final Class<?> type;

        private final boolean required;

        private final String defaultValue;

        RequestParameterValueSupplier(String name, Class<?> type, boolean required, String defaultValue) {
            this.name = name;
            this.type = type;
            this.required = required;
            this.defaultValue = defaultValue;
        }

        @Override
        public Object get(HttpServerRequest request, Map<String, String> pathVariables, Map<String, List<String>> requestParameters)
                throws BadRequestException {
            List<String> values = requestParameters.get(name);
            if (Objects.isNull(values)) {
                if (required) {
                    throw new BadRequestException("缺少必要的参数 " + name);
                } else if (defaultValue.equals(RequestParam.NULL)) {
                    values = new ArrayList<>();
                } else {
                    values = List.of(defaultValue);
                }
            }

            try {
                if (type.isArray()) {
                    Class<?> itemType = type.componentType();
                    Object array = Array.newInstance(itemType, values.size());
                    for (int i = 0; i < values.size(); ++i) {
                        Array.set(array, i, StringValueParseUtil.parse(itemType, values.get(i)));
                    }
                    return array;
                }
                return StringValueParseUtil.parse(type, values.isEmpty() ? null : values.get(0));
            } catch (Exception exception) {
                throw new BadRequestException("请求参数 " + name + " 数据类型错误");
            }
        }
    }

    static class RequestBodyValueSupplier implements ArgValueSupplier {

        private final Class<?> type;

        public RequestBodyValueSupplier(Class<?> type) {
            this.type = type;
        }

        @Override
        public Mono<?> get(HttpServerRequest request, Map<String, String> pathVariables, Map<String, List<String>> requestParameters) {
            return request.receive().aggregate().asString(StandardCharsets.UTF_8).flatMap(jsonContent -> {
                if (type.isAssignableFrom(String.class)) {
                    return Mono.just(jsonContent);
                }
                try {
                    return Mono.just(JsonUtil.readValue(jsonContent, type));
                } catch (JsonProcessingException jsonProcessingException) {
                    return Mono.error(new BadRequestException("请求体JSON反序列化异常"));
                }
            }).switchIfEmpty(Mono.error(new BadRequestException("需要请求体")));
        }

    }

}
