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

package zk.rgw.gateway.sdk.app;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Getter;

public class AppAuthInfo {

    public static final String APP_AUTH_HEADER_NAME = "X-Rgw-Authorization";

    private static final Pattern AUTHORIZATION_PATTERN = Pattern.compile("Access=([^;]+);\\s?SignTime=(\\d+);\\s?SignedHeaders=([^;]*);\\s?Signature=(\\w+)");

    private static final String SIGN_TIME_PATTERN = "yyyyMMddHHmmss";

    @Getter
    private final String accessKey;

    @Getter
    private final String signTimeStr;

    @Getter
    private final List<String> headerNames;

    @Getter
    private final String signature;

    @Getter
    private final Date signTime;

    public AppAuthInfo(String accessKey, Date signTime, List<String> headerNames, String signature) {
        this.accessKey = accessKey;
        this.signTime = signTime;
        this.signTimeStr = formatSignTime(signTime);
        this.headerNames = headerNames;
        this.signature = signature;
    }

    public static AppAuthInfo parseAuthorization(String authorization) {
        final Matcher matcher = AUTHORIZATION_PATTERN.matcher(authorization);
        if (!matcher.find()) {
            return null;
        }
        String accessKey = matcher.group(1);
        String signTimeStr = matcher.group(2);
        String headerNamesStr = matcher.group(3);
        String signature = matcher.group(4);
        final String[] headerNamesArr = headerNamesStr.split(",");
        try {
            return new AppAuthInfo(
                    accessKey,
                    parseSignTime(signTimeStr),
                    Arrays.asList(headerNamesArr),
                    signature
            );
        } catch (ParseException parseException) {
            return null;
        }
    }

    public static String formatSignTime(Date signTime) {
        return new SimpleDateFormat(SIGN_TIME_PATTERN).format(signTime);
    }

    public static Date parseSignTime(String signTimeStr) throws ParseException {
        return new SimpleDateFormat(SIGN_TIME_PATTERN).parse(signTimeStr);
    }

    public String asString() {
        return "Access=" + accessKey +
                ";SignTime=" + signTimeStr +
                ";SignedHeaders=" + String.join(",", headerNames) +
                ";Signature=" + signature;
    }

    @Override
    public String toString() {
        return asString();
    }

}
