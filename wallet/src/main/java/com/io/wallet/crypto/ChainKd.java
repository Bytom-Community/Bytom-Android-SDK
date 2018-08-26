package com.io.wallet.crypto;

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
    public static String rootXPrv() {
        Random randomno = new Random();
        byte[] nbyte = new byte[32];
        randomno.nextBytes(nbyte);
        return HMACSHA256.HMACSHA256(nbyte, new byte[]{'R', 'o', 'o', 't'});
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

}
