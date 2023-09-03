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

package zk.rgw.alc.common;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.TimeSeriesGranularity;
import com.mongodb.client.model.TimeSeriesOptions;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import zk.rgw.common.access.AccessLog;

@Slf4j
public class AccessLogDocumentUtil {

    private static final int EXPIRE_DAYS = 32;

    private static final String COLLECTION_NAME_PREFIX = "AccessLog_";

    private static final Map<String, Mono<MongoCollection<AccessLogDocument>>> COLLECTION_MAP = new HashMap<>();

    private AccessLogDocumentUtil() {
    }

    public static String collectionNameForEnv(String envId) {
        Objects.requireNonNull(envId);
        return COLLECTION_NAME_PREFIX + envId;
    }

    public static Mono<MongoCollection<AccessLogDocument>> getAccessLogCollectionForEnv(MongoDatabase mongoDatabase, String envId) {
        if (COLLECTION_MAP.containsKey(envId)) {
            return COLLECTION_MAP.get(envId);
        }
        return createIfNotExist(mongoDatabase, envId).doOnNext(collection -> COLLECTION_MAP.put(envId, Mono.just(collection)));
    }

    private static Mono<MongoCollection<AccessLogDocument>> createIfNotExist(MongoDatabase mongoDatabase, String envId) {
        String collectionName = collectionNameForEnv(envId);
        Mono<Void> makeSureCollectionCreatedMono = Flux.from(mongoDatabase.listCollectionNames())
                .hasElement(collectionName)
                .flatMap(alreadyExist -> {
                    if (Boolean.FALSE.equals(alreadyExist)) {
                        TimeSeriesOptions timeSeriesOptions = new TimeSeriesOptions(AccessLogDocument.TIME_FIELD).metaField(AccessLogDocument.META_FIELD)
                                .granularity(TimeSeriesGranularity.SECONDS);
                        CreateCollectionOptions createCollectionOptions = new CreateCollectionOptions()
                                .timeSeriesOptions(timeSeriesOptions)
                                .expireAfter(EXPIRE_DAYS, TimeUnit.DAYS);
                        Mono<Void> createCollectionMono = Mono.from(mongoDatabase.createCollection(collectionName, createCollectionOptions));

                        String requiredIndexName = "access-log-index";
                        MongoCollection<Document> collection = mongoDatabase.getCollection(collectionName);

                        Mono<Void> createIndexMono = Flux.from(collection.listIndexes())
                                .filter(document -> document.get("name").equals(requiredIndexName))
                                .count()
                                .flatMap(count -> {
                                    if (count == 0) {
                                        IndexOptions indexOptions = new IndexOptions().name(requiredIndexName);
                                        String indexDefinition = "{\"requestId\": 1, \"clientInfo.appId\": 1}";
                                        return Mono.from(collection.createIndex(Document.parse(indexDefinition), indexOptions))
                                                .doOnSuccess(ignore -> log.info("create index for mongodb time series collection {}", collectionName));
                                    } else {
                                        return Mono.empty();
                                    }
                                }).then();

                        return createCollectionMono.then(createIndexMono);
                    } else {
                        return Mono.empty();
                    }
                });

        CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build())
        );
        MongoCollection<AccessLogDocument> collection = mongoDatabase.getCollection(collectionName, AccessLogDocument.class).withCodecRegistry(codecRegistry);
        return makeSureCollectionCreatedMono.then(Mono.just(collection));
    }

    public static AccessLogDocument convertFromAccessLog(AccessLog accessLog) {
        AccessLogDocument accessLogDocument = new AccessLogDocument();

        accessLogDocument.setRequestId(accessLog.getRequestId());
        accessLogDocument.setReqTimestamp(accessLog.getReqTimestamp());
        accessLogDocument.setRespTimestamp(accessLog.getRespTimestamp());
        accessLogDocument.setMillisCost(accessLog.getMillisCost());
        accessLogDocument.setApiId(accessLog.getApiId());
        accessLogDocument.setClientInfo(accessLog.getClientInfo());
        accessLogDocument.setRequestInfo(accessLog.getRequestInfo());
        accessLogDocument.setResponseInfo(accessLog.getResponseInfo());
        accessLogDocument.setExtraInfo(accessLog.getExtraInfo());

        accessLogDocument.setTimestampMillis(Instant.ofEpochMilli(accessLogDocument.getReqTimestamp()));
        return accessLogDocument;
    }

}
