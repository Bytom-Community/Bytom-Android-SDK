package com.io.wallet.crypto;

import com.io.wallet.utils.HDUtils;

import java.util.Arrays;
import java.util.Random;

/**
 * Created by hwj on 2018/8/21.
 */

public class ChainKd {
    /**
     * random private key by HMACSHA256
     *
     * @return
     */
    public static byte[] rootXPrv() {
        Random randomno = new Random();
        byte[] nbyte = new byte[32];
        randomno.nextBytes(nbyte);
        byte[] keys = HDUtils.hmacSha512(new byte[]{'R', 'o', 'o', 't'}, nbyte);
        byte[] pri = pruneRootScalar(Arrays.copyOfRange(keys, 0, 32));
        System.arraycopy(pri, 0, keys, 0, 32);
        return keys;
    }

    /**
     * derive public key by private key
     *
     * @param xprv
     * @return
     */
    public static byte[] deriveXpub(byte[] xprv) {
        byte[] xpub = new byte[xprv.length];
        byte[] scalar = new byte[xprv.length / 2];
        System.arraycopy(xprv, 0, scalar, 0, xprv.length / 2);
        byte[] buf = Ed25519.scalarMultWithBaseToBytes(scalar);
        System.arraycopy(buf, 0, xpub, 0, buf.length);
        System.arraycopy(xprv, xprv.length / 2, xpub, xprv.length / 2, xprv.length / 2);
        return xpub;
    }

    /**
     * key must be >= 32 bytes long and gets rewritten in place.
     * This is NOT the same pruning as in Ed25519: it additionally clears the third
     * highest bit to ensure subkeys do not overflow the second highest bit.
     *
     * @param key
     * @return
     */
    private static byte[] pruneRootScalar(byte[] key) {
        key[0] &= 248;
        key[31] &= 31;
        key[31] |= 64;
        return key;
    }

}
