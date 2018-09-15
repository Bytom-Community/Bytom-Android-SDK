package com.io.wallet.crypto;

import android.util.Log;

import com.io.wallet.utils.HDUtils;
import com.io.wallet.utils.Strings;
import com.lambdaworks.crypto.SCrypt;

import org.bouncycastle.util.encoders.Hex;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.Arrays;

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
        byte[] keys = HDUtils.hmacSha512(new byte[]{'R', 'o', 'o', 't'}, Strings.generateRandomBytes(32));
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

    /**
     * generate derived ScryptKey
     *
     * @param password
     * @param salt
     * @param n
     * @param r
     * @param p
     * @param dkLen
     * @return
     * @throws Exception
     */
    public static byte[] generateDerivedScryptKey(
            byte[] password, byte[] salt, int n, int r, int p, int dkLen) throws Exception {
        try {
            return SCrypt.scrypt(password, salt, n, r, p, dkLen);
        } catch (GeneralSecurityException e) {
            throw new Exception(e);
        }
    }

    /**
     * generate id by index
     *
     * @param index
     * @return
     */
    public static String idGenerate(int index) {
        long ourEpochMS = 1496635208000L;
        Long n;
        long nowMs = (long) (System.nanoTime() / 1e6);
        long seqId = index % 1024;
        long shareId = 5;
        n = (nowMs - ourEpochMS) << 23;
        n = n | (shareId << 10);
        n = n | seqId;
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(0, n);
        return Base32.encode(buffer.array());
    }

    /**
     * generate mac
     *
     * @param derivedKey
     * @param cipherText
     * @return
     */
    public static byte[] generateMac(byte[] derivedKey, byte[] cipherText) {
        byte[] result = new byte[16 + cipherText.length];
        System.arraycopy(derivedKey, 16, result, 0, 16);
        System.arraycopy(cipherText, 0, result, 16, cipherText.length);
        return Hash.sha3(result);
    }


    /**
     * find private key dst
     *
     * @param privateKeys
     * @param xpub
     * @return
     * @throws Exception
     */
    public static int find(String[] privateKeys, String xpub) throws Exception {
        // 多签情况下，找到xpub对应的private key的下标 dst
        int dst = -1;
        for (int k = 0; k < privateKeys.length; k++) {
            byte[] tempXpub = ChainKd.deriveXpub(Hex.decode(privateKeys[k]));
            if (xpub.equals(Hex.toHexString(tempXpub))) {
                dst = k;
                Log.d("private[dst]: ", privateKeys[dst]);
                break;
            }
        }
        if (dst == -1) {
            throw new Exception("Not a proper private key to sign transaction.");
        }
        return dst;
    }

}
