package com.io.wallet.utils;


import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.spongycastle.crypto.digests.RIPEMD160Digest;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import static com.io.wallet.bean.Keys.extendedPublicKeySize;

public class StringUtils {
    private static final MessageDigest digest;
    public static String rfc3339DateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    public static final Gson serializer = new GsonBuilder().setDateFormat(rfc3339DateFormat).create();

    static {
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);  // Can't happen.
        }
    }

    public static String byte2hex(byte[] b) {
        StringBuilder hs = new StringBuilder();
        String stmp;
        for (int n = 0; b != null && n < b.length; n++) {
            stmp = Integer.toHexString(b[n] & 0XFF);
            if (stmp.length() == 1)
                hs.append('0');
            hs.append(stmp);
        }
        return hs.toString();
    }

    /**
     * Calculates RIPEMD160(SHA256(input)). This is used in Address calculations.
     */
    public static byte[] sha256hash160(byte[] input) {
        try {
            byte[] sha256 = MessageDigest.getInstance("SHA-256").digest(input);
            RIPEMD160Digest digest = new RIPEMD160Digest();
            digest.update(sha256, 0, sha256.length);
            byte[] out = new byte[20];
            digest.doFinal(out, 0);
            return out;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);  // Cannot happen.
        }
    }

    public static byte[] sha256(byte[]... datas) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        for (byte[] data : datas) {
            digest.update(data, 0, data.length);
        }

        return digest.digest();

    }

    /**
     * Calculates the SHA-256 hash of the given byte range, and then hashes the resulting hash
     * again. This is
     * standard procedure in Bitcoin. The resulting hash is in big endian form.
     */
    public static byte[] doubleDigest(byte[] input, int offset, int length) {
        synchronized (digest) {
            digest.reset();
            digest.update(input, offset, length);
            byte[] first = digest.digest();
            return digest.digest(first);
        }
    }


    public static String toHexStringNoPrefix(BigInteger value) {
        return value.toString(16);
    }

    public static String toHexStringNoPrefix(byte[] input) {
        return toHexString(input, 0, input.length, false);
    }

    public static String getUUID32() {
        return UUID.randomUUID().toString().toLowerCase();
    }

    public static byte[] hexStringToByteArray(String input) {
        String cleanInput = cleanHexPrefix(input);
        int len = cleanInput.length();
        if (len == 0) {
            return new byte[]{};
        }
        byte[] data;
        int startIdx;
        if (len % 2 != 0) {
            data = new byte[(len / 2) + 1];
            data[0] = (byte) Character.digit(cleanInput.charAt(0), 16);
            startIdx = 1;
        } else {
            data = new byte[len / 2];
            startIdx = 0;
        }

        for (int i = startIdx; i < len; i += 2) {
            data[(i + 1) / 2] = (byte) ((Character.digit(cleanInput.charAt(i), 16) << 4)
                    + Character.digit(cleanInput.charAt(i + 1), 16));
        }
        return data;
    }

    public static String cleanHexPrefix(String input) {
        if (containsHexPrefix(input)) {
            return input.substring(2);
        } else {
            return input;
        }
    }

    public static boolean containsHexPrefix(String input) {
        return input.length() > 1 && input.charAt(0) == '0' && input.charAt(1) == 'x';
    }

    public static String toHexString(byte[] input, int offset, int length, boolean withPrefix) {
        StringBuilder stringBuilder = new StringBuilder();
        if (withPrefix) {
            stringBuilder.append("0x");
        }
        for (int i = offset; i < offset + length; i++) {
            stringBuilder.append(String.format("%02x", input[i] & 0xFF));
        }

        return stringBuilder.toString();
    }

    public static String toHexString(byte[] input) {
        return toHexString(input, 0, input.length, true);
    }

    public static String getISO8601Timestamp(Date date) {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        df.setTimeZone(tz);
        String nowAsISO = df.format(date);
        return nowAsISO;

    }

    public static String keyFileName(String id) {
        return "UTC--" + getISO8601Timestamp(new Date()) + "--" + id;
    }

    public static String getUnmarshalText(String key) throws Exception {
        if (key.length() != 2 * extendedPublicKeySize)
            throw new Exception("bad key length");
        return key;
    }

    public static JsonObject jsonToJsonObject(String str) {
        JsonObject accountObj;
        if (TextUtils.isEmpty(str)) {
            accountObj = new JsonObject();
        } else {
            try {
                accountObj = new JsonParser().parse(str).getAsJsonObject();
            } catch (Exception e) {
                accountObj = new JsonObject();
            }
        }
        return accountObj;
    }

    public static String objectToJson(Object object) {
        return new Gson().toJson(object);
    }

    public static long currentTimeSeconds() {
        return System.currentTimeMillis() / 1000;
    }

    public static void uint32ToByteStreamLE(long val, OutputStream stream) throws IOException {
        stream.write((int) (0xFF & (val)));
        stream.write((int) (0xFF & (val >> 8)));
        stream.write((int) (0xFF & (val >> 16)));
        stream.write((int) (0xFF & (val >> 24)));
    }

    final protected static char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    /**
     * Returns the given byte array hex encoded.
     */
    public static String bytesToHexString(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars).toUpperCase(Locale.US);
    }


    /**
     * Returns a copy of the given byte array in reverse order.
     */
    public static byte[] reverseBytes(byte[] bytes) {
        // We could use the XOR trick here but it's easier to understand if we don't. If we find
        // this is really a
        // performance issue the matter can be revisited.
        byte[] buf = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++)
            buf[i] = bytes[bytes.length - 1 - i];
        return buf;
    }

    public static String hashToString(byte[] bytes) {
        return bytesToHexString(reverseBytes(bytes));
    }

}
