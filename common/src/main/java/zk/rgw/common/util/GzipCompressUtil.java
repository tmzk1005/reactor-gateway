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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import lombok.extern.slf4j.Slf4j;

import zk.rgw.common.exception.RgwRuntimeException;

@Slf4j
public final class GzipCompressUtil {

    private GzipCompressUtil() {
    }

    public static byte[] decompress(byte[] bytes) {
        if (ObjectUtil.isEmpty(bytes)) {
            return new byte[0];
        }
        try (
                ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
                ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytes);
                InputStream compressedIn = new GZIPInputStream(bytesIn);
        ) {
            copy(compressedIn, bytesOut);
            return bytesOut.toByteArray();
        } catch (IOException ioException) {
            String message = "Failed to decompress compressed content";
            log.error("{}", message, ioException);
            throw new RgwRuntimeException(message);
        }
    }

    public static int copy(InputStream in, OutputStream out) throws IOException {
        try (in; out) {
            int count = (int) in.transferTo(out);
            out.flush();
            return count;
        }
    }

    public static byte[] compress(byte[] bytes) {
        try (
                ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
                ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytes);
                OutputStream outputStream = new GZIPOutputStream(bytesOut);
        ) {
            copy(bytesIn, outputStream);
            return bytesOut.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("couldn't encode body to gzip", e);
        }
    }

}