package com.io.wallet.crypto;

import com.io.wallet.utils.Strings;

import org.spongycastle.jcajce.provider.digest.Keccak;

/**
 * Crypto related functions.
 */
public class Hash {
    private Hash() {
    }

    /**
     * Keccak-256 hash function.
     *
     * @param hexInput hex encoded input data with optional 0x prefix
     * @return hash value as hex encoded string
     */
    public static String sha3(String hexInput) {
        byte[] bytes = Strings.hex2Bytes(hexInput);
        byte[] result = sha3(bytes);
        return Strings.byte2hex(result);
    }

    /**
     * Keccak-256 hash function.
     *
     * @param input  binary encoded input data
     * @param offset of start of data
     * @param length of data
     * @return hash value
     */
    public static byte[] sha3(byte[] input, int offset, int length) {
        Keccak.DigestKeccak kecc = new Keccak.Digest256();
        kecc.update(input, offset, length);
        return kecc.digest();
    }

    public static byte[] sha3(byte[] input) {
        return sha3(input, 0, input.length);
    }
}


