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

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import lombok.Getter;

public class BytesCollector {

    private final List<byte[]> byteArrayList;

    private final long limit;

    @Getter
    private int size = 0;

    private boolean exceed = false;

    public BytesCollector(long limit) {
        this.limit = limit;
        if (limit == 0) {
            exceed = true;
            byteArrayList = null;
        } else {
            byteArrayList = new ArrayList<>();
        }
    }

    public void append(ByteBuf byteBuf) {
        if (exceed) {
            size += byteBuf.readableBytes();
            return;
        }
        byte[] bytes = ByteBufUtil.getBytes(byteBuf);
        size += bytes.length;
        assert byteArrayList != null;
        if (size > limit) {
            // 尽快删除byteArrayList，以便JVM更好的GC
            byteArrayList.clear();
            exceed = true;
        } else {
            byteArrayList.add(bytes);
        }
    }

    public byte[] allBytes() {
        if (exceed) {
            return new byte[0];
        }
        byte[] finalByteArray = new byte[size];
        int start = 0;
        assert byteArrayList != null;
        for (byte[] byteArray : byteArrayList) {
            int length = byteArray.length;
            System.arraycopy(byteArray, 0, finalByteArray, start, length);
            start += length;
        }
        return finalByteArray;
    }

}
