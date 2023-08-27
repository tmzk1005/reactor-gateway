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
package zk.rgw.gateway.accesslog;

import java.util.LinkedList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

public class BytesCollector {

    private final List<byte[]> byteArrayList = new LinkedList<>();

    private int size = 0;

    public void append(ByteBuf byteBuf) {
        byte[] bytes = ByteBufUtil.getBytes(byteBuf);
        byteArrayList.add(bytes);
        size += bytes.length;
    }

    public byte[] allBytes() {
        byte[] finalByteArray = new byte[size];
        int start = 0;
        for (byte[] byteArray : byteArrayList) {
            int length = byteArray.length;
            System.arraycopy(byteArray, 0, finalByteArray, start, length);
            start += length;
        }
        return finalByteArray;
    }

}
