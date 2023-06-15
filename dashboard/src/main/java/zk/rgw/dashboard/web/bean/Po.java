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

package zk.rgw.dashboard.web.bean;

import java.io.Serializable;
import java.lang.reflect.Constructor;

public interface Po<D extends Dto> extends Serializable {

    String getId();

    void setId(String id);

    <P extends Po<D>> P initFromDto(D dto);

    static <P extends Po<D>, D extends Dto> P fromDto(Class<P> poClass, D dtoInstance) {
        Constructor<P> constructor;
        try {
            constructor = poClass.getConstructor();
            P poInstance = constructor.newInstance();
            return poInstance.initFromDto(dtoInstance);
        } catch (ReflectiveOperationException reflectiveOperationException) {
            throw new IllegalArgumentException("Class " + poClass.getName() + " does not has an default constructor.", reflectiveOperationException);
        }
    }

}
