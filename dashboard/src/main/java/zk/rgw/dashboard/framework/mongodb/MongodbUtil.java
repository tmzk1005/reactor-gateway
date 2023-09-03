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

import java.util.Objects;

import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import zk.rgw.common.util.ObjectUtil;
import zk.rgw.dashboard.framework.exception.NotObjectIdException;

public class MongodbUtil {

    private MongodbUtil() {
    }

    public static String getCollectionName(Class<?> clazz) {
        Objects.requireNonNull(clazz);
        Document documentAnno = clazz.getDeclaredAnnotation(Document.class);
        if (Objects.isNull(documentAnno)) {
            throw new IllegalArgumentException(clazz.getName() + " is not an mongodb Document class.");
        }
        String collectionName = documentAnno.collection();
        if (ObjectUtil.isEmpty(collectionName)) {
            collectionName = clazz.getSimpleName();
        }
        return collectionName;
    }

    public static Bson createFilterById(String id) throws NotObjectIdException {
        try {
            ObjectId objectId = new ObjectId(id);
            return Filters.eq("_id", objectId);
        } catch (IllegalArgumentException exception) {
            throw new NotObjectIdException(id);
        }
    }

}
