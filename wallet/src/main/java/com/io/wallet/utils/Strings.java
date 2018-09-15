package com.io.wallet.utils;


import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import static com.io.wallet.utils.Constant.extendedPublicKeySize;

public class Strings {
    final protected static char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    public static String rfc3339DateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    public static final Gson serializer = new GsonBuilder().setDateFormat(rfc3339DateFormat).create();

    /**
     * Returns the given byte array hex encoded.
     *
     * @param b
     * @return
     */
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
     * Return the given hex bytes decoded
     *
     * @param input
     * @return
     */
    public static byte[] hex2Bytes(String input) {
        int len = input.length();
        if (len == 0) {
            return new byte[]{};
        }
        byte[] data;
        int startIdx;
        if (len % 2 != 0) {
            data = new byte[(len / 2) + 1];
            data[0] = (byte) Character.digit(input.charAt(0), 16);
            startIdx = 1;
        } else {
            data = new byte[len / 2];
            startIdx = 0;
        }

        for (int i = startIdx; i < len; i += 2) {
            data[(i + 1) / 2] = (byte) ((Character.digit(input.charAt(i), 16) << 4)
                    + Character.digit(input.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * get UUID
     *
     * @return
     */
    public static String getUUID32() {
        return UUID.randomUUID().toString().toLowerCase();
    }


    /**
     * get format timestamp
     *
     * @param date
     * @return
     */
    public static String getISO8601Timestamp(Date date) {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        df.setTimeZone(tz);
        String nowAsISO = df.format(date);
        return nowAsISO;

    }

    /**
     * generate key file name
     *
     * @param id
     * @return
     */
    public static String keyFileName(String id) {
        return "UTC--" + getISO8601Timestamp(new Date()) + "--" + id;
    }


    /**
     * get Unmarshal key
     *
     * @param key
     * @return
     * @throws Exception
     */
    public static String getUnmarshalText(String key) throws Exception {
        if (key.length() != 2 * extendedPublicKeySize)
            throw new Exception("bad key length");
        return key;
    }

    /**
     * cover json to object
     *
     * @param str
     * @return
     */
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

    /**
     * cover object to json string
     *
     * @param object
     * @return
     */
    public static String objectToJson(Object object) {
        return new Gson().toJson(object);
    }

    /**
     * return curernt time seconds
     *
     * @return
     */
    public static long currentTimeSeconds() {
        return System.currentTimeMillis() / 1000;
    }

    /**
     *
     * @param val
     * @param stream
     * @throws IOException
     */
    public static void uint32ToByteStreamLE(long val, OutputStream stream) throws IOException {
        stream.write((int) (0xFF & (val)));
        stream.write((int) (0xFF & (val >> 8)));
        stream.write((int) (0xFF & (val >> 16)));
        stream.write((int) (0xFF & (val >> 24)));
    }

    /**
     * generate random bytes
     *
     * @param size
     * @return
     */
    public static byte[] generateRandomBytes(int size) {
        byte[] bytes = new byte[size];
        SecureRandom random = new SecureRandom();
        random.nextBytes(bytes);
        return bytes;
    }

}
