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
package zk.rgw.plugin.filter.grpc;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.ManagedChannel;
import io.grpc.MethodDescriptor;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.stub.ClientCalls;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import lombok.Getter;
import lombok.Setter;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerResponse;

import zk.rgw.common.exception.RgwException;
import zk.rgw.plugin.api.Exchange;
import zk.rgw.plugin.api.filter.FilterChain;
import zk.rgw.plugin.api.filter.JsonConfFilterPlugin;
import zk.rgw.plugin.exception.PluginConfException;
import zk.rgw.plugin.util.ResponseUtil;

public class GrpcFilter implements JsonConfFilterPlugin {

    private static final String ARG_SERVICE_NAME = "service";

    private static final String ARG_METHOD_NAME = "method";

    private final Conf conf = new Conf();

    private DescriptorProtos.FileDescriptorSet fds;

    @Override
    public Conf getConf() {
        return conf;
    }

    @Override
    public void afterConfigured() throws PluginConfException {
        try {
            byte[] bytes = Base64.getDecoder().decode(conf.fileDescriptorSetBase64);
            this.fds = DescriptorProtos.FileDescriptorSet.parseFrom(bytes);
        } catch (Exception exception) {
            throw new PluginConfException(exception.getMessage(), exception);
        }
        // help gc，把不在使用的大字符串置为null
        conf.fileDescriptorSetBase64 = null;
    }

    @Override
    public Mono<Void> filter(Exchange exchange, FilterChain chain) {
        Map<String, List<String>> queryParams = new QueryStringDecoder(exchange.getRequest().uri()).parameters();

        if (!queryParams.containsKey(ARG_SERVICE_NAME)) {
            return ResponseUtil.sendStatus(exchange.getResponse(), HttpResponseStatus.BAD_REQUEST, "Require uri parameter " + ARG_SERVICE_NAME);
        }

        if (!queryParams.containsKey(ARG_METHOD_NAME)) {
            return ResponseUtil.sendStatus(exchange.getResponse(), HttpResponseStatus.BAD_REQUEST, "Require uri parameter " + ARG_METHOD_NAME);
        }

        String serviceName = queryParams.get(ARG_SERVICE_NAME).get(0);
        String methodName = queryParams.get(ARG_METHOD_NAME).get(0);

        Mono<String> respJsonMono = doGrpcCall(exchange, serviceName, methodName);

        return respJsonMono.flatMap(respJson -> {
            final byte[] bytes = respJson.getBytes();
            HttpServerResponse response = exchange.getResponse();
            response.status(HttpResponseStatus.OK);
            response.header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
            response.header(HttpHeaderNames.CONTENT_LENGTH, String.valueOf(bytes.length));
            return response.sendByteArray(Mono.just(bytes)).then();
        }).onErrorResume(throwable -> {
            if (throwable instanceof GrpcCallException grpcCallException) {
                return ResponseUtil.sendStatus(exchange.getResponse(), grpcCallException.getStatus(), grpcCallException.getMessage());
            } else {
                return Mono.error(throwable);
            }
        });
    }

    private Mono<String> doGrpcCall(Exchange exchange, String serviceName, String methodName) {
        Objects.requireNonNull(fds);
        List<DescriptorProtos.FileDescriptorProto> fdpList = fds.getFileList();
        for (DescriptorProtos.FileDescriptorProto fdp : fdpList) {
            Descriptors.FileDescriptor fd;
            try {
                fd = Descriptors.FileDescriptor.buildFrom(fdp, new Descriptors.FileDescriptor[0]);
            } catch (Descriptors.DescriptorValidationException dve) {
                GrpcCallException exception = new GrpcCallException(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Grpc Descriptor build error.");
                return Mono.error(exception);
            }
            final Descriptors.ServiceDescriptor sd = fd.findServiceByName(serviceName);
            if (Objects.isNull(sd)) {
                continue;
            }
            Descriptors.MethodDescriptor md = sd.findMethodByName(methodName);
            if (Objects.isNull(md)) {
                continue;
            }

            MethodDescriptor.Marshaller<DynamicMessage> marshaller = ProtoUtils.marshaller(DynamicMessage.newBuilder(md.getOutputType()).build());
            MethodDescriptor<DynamicMessage, DynamicMessage> methodDescriptor = MethodDescriptor
                    .<DynamicMessage, DynamicMessage>newBuilder().setType(MethodDescriptor.MethodType.UNKNOWN)
                    .setFullMethodName(MethodDescriptor.generateFullMethodName(serviceName, methodName))
                    .setRequestMarshaller(marshaller).setResponseMarshaller(marshaller).build();

            ManagedChannel channel = NettyChannelBuilder.forAddress(conf.getHost(), conf.getPort()).build();

            DynamicMessage.Builder reqMsgBuilder = DynamicMessage.newBuilder(md.getInputType());

            exchange.getRequest().receive().aggregate().flatMap((Function<ByteBuf, Mono<?>>) byteBuf -> {
                String reqJson = new String(ByteBufUtil.getBytes(byteBuf));
                try {
                    JsonFormat.parser().merge(reqJson, reqMsgBuilder);
                    DynamicMessage reqMsg = reqMsgBuilder.build();
                    ClientCall<DynamicMessage, DynamicMessage> clientCall = channel.newCall(methodDescriptor, CallOptions.DEFAULT);
                    DynamicMessage respMsg = ClientCalls.blockingUnaryCall(clientCall, reqMsg);
                    return Mono.just(JsonFormat.printer().print(respMsg));
                } catch (InvalidProtocolBufferException e) {
                    GrpcCallException grpcCallException = new GrpcCallException(
                            HttpResponseStatus.INTERNAL_SERVER_ERROR, "Failed to convert json to protobuf data."
                    );
                    return Mono.error(grpcCallException);
                }
            });
        }
        String msg = String.format("No grpc service named %s exists, or no method named %s in this service.", serviceName, methodName);
        GrpcCallException grpcCallException = new GrpcCallException(HttpResponseStatus.BAD_REQUEST, msg);
        return Mono.error(grpcCallException);
    }

    @Getter
    @Setter
    public static class Conf {

        private boolean tlsEnabled;

        private String host;

        private int port;

        private String fileDescriptorSetBase64;

    }

    private static class GrpcCallException extends RgwException {

        @Getter
        private final HttpResponseStatus status;

        public GrpcCallException(HttpResponseStatus status, String message) {
            super(message);
            this.status = status;
        }

    }

}
