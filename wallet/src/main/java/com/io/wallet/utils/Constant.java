package com.io.wallet.utils;

public class Constant {
    public static String SUCCESS = "success";
    public static String FAIL = "fail";
    public static String ADDRESS_JSON = "address_json";

    public static final int N_LIGHT = 1 << 12;
    public static final int P_LIGHT = 6;

    public static final int N_STANDARD = 1 << 18;
    public static final int P_STANDARD = 1;
    public static final int R = 8;
    public static final int DKLEN = 32;

    public static final String CIPHER = "aes-128-ctr";
    public static final String AES_128_CTR = "pbkdf2";
    public static final String SCRYPT = "scrypt";

    public static final int CURRENT_VERSION = 1;
    public static final String KEY_TYPE = "bytom_kd";
    public static final int PRIVATE_KEY_SIZE = 32;
    public static final int PUBLIC_KEY_SIZE = 64;
    public static final int extendedPublicKeySize = 64;

    public static final String BECH32HRPSEGWI = "bm";
    public static final byte WITNESSVERSION = 0x00;

    public static final byte ACCOUNTKEYSPACE = 1;
    public static final String ACCOUNTCACHE = "accountCache";
    public static String ACCOUNTS_JSON = "account_json";
    public static String ACCOUNTINDEX = "AccountIndex";
    public static String ACCOUNT = "Account:";
    public static String ACCOUNTALIAS = "AccountAlias:";

    public static final String CONTRACTPREFIX = "Contract:";

    public static final String XPUBCACHE = "xpubCache";
    public static final String ENCRYPTEDKEY = "encryptedKey";

}
