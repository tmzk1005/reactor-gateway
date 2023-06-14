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

package zk.rgw.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class JsonUtil {

    private static final ZoneId TZ_ID = ZoneId.of("CTT", ZoneId.SHORT_IDS);

    private static final TimeZone TZ = TimeZone.getTimeZone(TZ_ID);

    public static final String DEFAULT_DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    public static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_PATTERN).withZone(TZ_ID);

    private static final ObjectMapper OM = new JsonMapper()
            .registerModule(new JavaTimeModule())
            .setTimeZone(TZ)
            .setDateFormat(new SimpleDateFormat(DEFAULT_DATE_TIME_PATTERN));

    private static final ObjectWriter OW = OM.writerWithDefaultPrettyPrinter();

    private JsonUtil() {
    }

    public static String toJson(Object object, boolean pretty) throws JsonProcessingException {
        return pretty ? OW.writeValueAsString(object) : OM.writeValueAsString(object);
    }

    public static String toJsonPretty(Object object) throws JsonProcessingException {
        return toJson(object, true);
    }

    public static String toJson(Object object) throws JsonProcessingException {
        return toJson(object, false);
    }

    public static <T> T readValue(String content, Class<T> clazz) throws JsonProcessingException {
        return OM.readValue(content, clazz);
    }

    public static <T> T readValue(InputStream inputStream, Class<T> clazz) throws IOException {
        return OM.readValue(inputStream, clazz);
    }

    @SuppressWarnings("unchecked")
    public static Map<Object, Object> readValue(String content) throws JsonProcessingException {
        return OM.readValue(content, HashMap.class);
    }

    public static String formatInstant(Instant instant) {
        return FMT.format(instant);
    }

}
