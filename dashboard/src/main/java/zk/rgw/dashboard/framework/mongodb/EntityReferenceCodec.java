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
package zk.rgw.dashboard.framework.mongodb;

import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.types.ObjectId;

import zk.rgw.common.exception.RgwRuntimeException;
import zk.rgw.dashboard.framework.xo.Po;

public class EntityReferenceCodec<T extends Po<?>> implements Codec<T> {

    private final Class<T> entityClass;

    private final CodecRegistry codecRegistry;

    public EntityReferenceCodec(CodecRegistry codecRegistry, Class<T> entityClass) {
        this.codecRegistry = codecRegistry;
        this.entityClass = entityClass;
    }

    @Override
    public T decode(BsonReader reader, DecoderContext decoderContext) {
        BsonType currentBsonType = reader.getCurrentBsonType();
        if (BsonType.OBJECT_ID == currentBsonType) {
            return decodeOnlyId(reader);
        }
        return codecRegistry.get(entityClass).decode(reader, decoderContext);
    }

    @Override
    public void encode(BsonWriter writer, T value, EncoderContext encoderContext) {
        writer.writeObjectId(new ObjectId(value.getId()));
    }

    @Override
    public Class<T> getEncoderClass() {
        return entityClass;
    }

    private T decodeOnlyId(BsonReader reader) {
        String refId = reader.readObjectId().toString();
        T instance;
        try {
            instance = getEncoderClass().getConstructor().newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new RgwRuntimeException(exception);
        }
        instance.setId(refId);
        return instance;
    }

}