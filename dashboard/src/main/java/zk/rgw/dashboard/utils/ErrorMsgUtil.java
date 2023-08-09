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

public class ErrorMsgUtil {

    private ErrorMsgUtil() {
    }

    public static String apiNotExist(String apiId) {
        return "不存在id为" + apiId + "的API";
    }

    public static String noApiRights(String apiId) {
        return "没有权限操作id为" + apiId + "的API";
    }

    public static String noAppRights(String appId) {
        return "没有权限操作id为" + appId + "的APP";
    }

    public static String envNotExist(String apiId) {
        return "不存在id为" + apiId + "的环境";
    }

    public static String appNotExist(String appId) {
        return "不存在id为" + appId + "的应用";
    }

    public static String subscribeNotExist(String subId) {
        return "不存在id为" + subId + "订阅申请记录";
    }

}
