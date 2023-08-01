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
package zk.rgw.dashboard.utils;

public final class Patters {

    private Patters() {
    }

    public static final String IDENTIFIER = "^[a-zA-Z0-9_]+$";

    public static final String IDENTIFIER_ZH = "^[a-zA-Z0-9_\u4E00-\u9FA5]+$";
    public static final String DOMAIN_PART = "([a-zA-Z0-9]|[a-zA-Z0-9][-a-zA-Z0-9]{0,61}[a-zA-Z0-9])";

    public static final String DOMAIN = "^(?=^.{3,255}$)" + DOMAIN_PART + "(\\." + DOMAIN_PART + ")+$";

    public static final String VERSION_NUM = "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$";

}
