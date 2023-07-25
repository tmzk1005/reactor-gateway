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


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import zk.rgw.dashboard.framework.xo.Po;

public class CollectionOfEntityReferenceCodec<T extends Po<?>> implements Codec<Collection<T>> {

    private final Codec<T> codec;

    private final Class<Collection<T>> collectionClass;

    public CollectionOfEntityReferenceCodec(EntityReferenceCodec<T> codec, Class<Collection<T>> collectionClass) {
        this.codec = codec;
        this.collectionClass = collectionClass;
    }

    @Override
    public Collection<T> decode(BsonReader reader, DecoderContext decoderContext) {
        Collection<T> collection = createCollectionInstance(getEncoderClass());
        reader.readStartArray();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            if (reader.getCurrentBsonType() == BsonType.NULL) {
                collection.add(null);
                reader.readNull();
            } else {
                collection.add(codec.decode(reader, decoderContext));
            }
        }
        reader.readEndArray();
        return collection;
    }

    @Override
    public void encode(BsonWriter writer, Collection<T> collection, EncoderContext encoderContext) {
        writer.writeStartArray();
        for (final T value : collection) {
            if (value == null) {
                writer.writeNull();
            } else {
                codec.encode(writer, value, encoderContext);
            }
        }
        writer.writeEndArray();
    }

    @Override
    public Class<Collection<T>> getEncoderClass() {
        return collectionClass;
    }

    private static <T> Collection<T> createCollectionInstance(Class<Collection<T>> clazz) {
        if (clazz.equals(Set.class)) {
            return new HashSet<>(1);
        } else if (clazz.equals(List.class)) {
            return new ArrayList<>(1);
        }
        throw new IllegalArgumentException("Only Set and List is supported now!");
    }

}