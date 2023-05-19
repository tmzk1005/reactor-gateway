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

package zk.rgw.plugin.util;

import lombok.Getter;
import lombok.Setter;

public class Shuck<T> {

    public static final int CODE_OK = 0;

    public static final int CODE_ERROR = -1;

    public static final String MESSAGE_OK = "Succeed";

    public static final String MESSAGE_ERROR = "Internal Server Error";

    @Setter
    @Getter
    private int code;

    @Setter
    @Getter
    private String message;

    @Getter
    @Setter
    private T data;

    public Shuck() {
    }

    public Shuck(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Shuck<T> success(T data) {
        return success(MESSAGE_OK, data);
    }

    public static <T> Shuck<T> success(String message, T data) {
        return new Shuck<>(CODE_OK, message, data);
    }

    public String toJsonNoData() {
        return jsonOf(this.code, this.message);
    }

    public static String jsonOf(int code, String message) {
        return "{\"code\": " + code + ", \"message\": \"" + message + "\"}";
    }

}
