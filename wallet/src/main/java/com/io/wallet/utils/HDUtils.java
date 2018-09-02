package com.io.wallet.utils;

import org.spongycastle.crypto.digests.SHA512Digest;
import org.spongycastle.crypto.macs.HMac;
import org.spongycastle.crypto.params.KeyParameter;

public class HDUtils {

    public static HMac createHmacSha512Digest(byte[] key) {
        SHA512Digest digest = new SHA512Digest();
        HMac hMac = new HMac(digest);
        hMac.init(new KeyParameter(key));
        return hMac;
    }

    public static byte[] hmacSha512(HMac hmacSha512, byte[] input) {
        hmacSha512.reset();
        hmacSha512.update(input, 0, input.length);
        byte[] out = new byte[64];
        hmacSha512.doFinal(out, 0);
        return out;
    }

    public static byte[] hmacSha512(byte[] key, byte[] data) {
        return hmacSha512(createHmacSha512Digest(key), data);
    }

}
