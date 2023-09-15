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
package zk.rgw.gateway.sdk.util;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 对二进制数据进行hash签名
 */
public class Signer {

    public static final String SIGN_ALGORITHM = "HmacSHA256";

    public static final String HASH_ALGORITHM = "SHA-256";

    private final Mac mac;

    private final MessageDigest md;

    private boolean finished = false;

    public Signer(String secret) throws NoSuchAlgorithmException, InvalidKeyException {
        this.mac = Mac.getInstance(SIGN_ALGORITHM);
        this.mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), SIGN_ALGORITHM));
        this.md = MessageDigest.getInstance(HASH_ALGORITHM);
    }

    public Signer update(String text) {
        return update(text.getBytes(StandardCharsets.UTF_8));
    }

    public Signer update(byte[] bytes) {
        if (finished) {
            throw new IllegalStateException("Already finished update.");
        }
        this.md.update(bytes);
        return this;
    }

    public String finishUpdate() {
        this.finished = true;
        String hexStr = String.valueOf(HexUtil.encode(md.digest()));
        byte[] signedBytes = mac.doFinal(hexStr.getBytes(StandardCharsets.UTF_8));
        return String.valueOf(HexUtil.encode(signedBytes));
    }

    public void reset() {
        this.md.reset();
        this.finished = false;
    }

}
