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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;

import lombok.Setter;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.ClassModelBuilder;
import org.bson.codecs.pojo.Convention;
import org.bson.codecs.pojo.PropertyModelBuilder;
import org.bson.codecs.pojo.TypeWithTypeParameters;

import zk.rgw.common.exception.RgwRuntimeException;
import zk.rgw.dashboard.framework.xo.Po;

public class CustomConvention implements Convention {

    @Setter
    private CodecRegistry codecRegistry;

    @Override
    public void apply(ClassModelBuilder<?> classModelBuilder) {
        for (final PropertyModelBuilder<?> propertyModel : classModelBuilder.getPropertyModelBuilders()) {
            if (!"id".equals(propertyModel.getName())) {
                propertyModel.propertySerialization(new CustomPropertySerialization<>());
            }
            for (PropertyModelBuilder<?> propertyModelBuilder : classModelBuilder.getPropertyModelBuilders()) {
                processPropertyAnnotations(propertyModelBuilder);
            }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void processPropertyAnnotations(final PropertyModelBuilder<?> propertyModelBuilder) {
        for (Annotation annotation : propertyModelBuilder.getReadAnnotations()) {
            if (annotation instanceof DocumentReference) {
                TypeWithTypeParameters<?> typeInfo = getTypeInfoOfPropertyModelBuilder(propertyModelBuilder);
                Class<?> type = typeInfo.getType();
                if (Collection.class.isAssignableFrom(type)) {
                    propertyModelBuilder.codec((Codec) getCollectionOfEntityReferenceCodec(typeInfo));
                } else if (Po.class.isAssignableFrom(type)) {
                    propertyModelBuilder.codec((Codec) getEntityReferenceCodec(type));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> TypeWithTypeParameters<T> getTypeInfoOfPropertyModelBuilder(PropertyModelBuilder<T> propertyModelBuilder) {
        try {
            Field typeDataField = propertyModelBuilder.getClass().getDeclaredField("typeData");
            typeDataField.setAccessible(true);
            return (TypeWithTypeParameters<T>) typeDataField.get(propertyModelBuilder);
        } catch (ReflectiveOperationException exception) {
            throw new RgwRuntimeException(exception);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Po<?>> EntityReferenceCodec<T> getEntityReferenceCodec(Class<?> entityClass) {
        return new EntityReferenceCodec<>(codecRegistry, (Class<T>) entityClass);
    }

    @SuppressWarnings("unchecked")
    private <T extends Po<?>> CollectionOfEntityReferenceCodec<T> getCollectionOfEntityReferenceCodec(TypeWithTypeParameters<?> typeInfo) {
        Class<Collection<T>> collectionClass = (Class<Collection<T>>) typeInfo.getType();
        Class<T> entityClass = (Class<T>) typeInfo.getTypeParameters().get(0).getType();
        return new CollectionOfEntityReferenceCodec<>(getEntityReferenceCodec(entityClass), collectionClass);
    }

}
