package com.io.wallet.crypto;

import com.io.wallet.bean.Crypto;
import com.io.wallet.bean.Keys;
import com.io.wallet.bean.ScryptKdfParams;
import com.io.wallet.bean.WalletFile;
import com.io.wallet.main.Storage;
import com.io.wallet.utils.StringUtils;
import com.lambdaworks.crypto.SCrypt;

import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by hwj on 2018/8/24.
 */

public class Wallet {

    private static final int N_LIGHT = 1 << 12;
    private static final int P_LIGHT = 6;

    private static final int N_STANDARD = 1 << 18;
    private static final int P_STANDARD = 1;
    private static final int R = 8;
    private static final int DKLEN = 32;

    public static final String CIPHER = "aes-128-ctr";
    public static final String AES_128_CTR = "pbkdf2";
    public static final String SCRYPT = "scrypt";

    private static final int CURRENT_VERSION = 1;
    public static final String KEY_TYPE = "bytom_kd";

    public static String create(String password, Keys keyPair, int n, int p) throws Exception {
        byte[] salt = generateRandomBytes(32);
        byte[] derivedKey = generateDerivedScryptKey(
                password.getBytes(Charset.forName("UTF-8")), salt, n, R, p, DKLEN);
        byte[] encryptKey = Arrays.copyOfRange(derivedKey, 0, 16);
        byte[] iv = generateRandomBytes(16);
        byte[] privateKeyBytes = keyPair.getXPrv().getBytes();
        byte[] cipherText = performCipherOperation(
                Cipher.ENCRYPT_MODE, iv, encryptKey, privateKeyBytes);
        byte[] mac = generateMac(derivedKey, cipherText);

        WalletFile wallet = getWallet(keyPair, cipherText, iv, salt, mac, n, p);
        return Storage.getInstance().saveKey(wallet, StringUtils.keyFileName(keyPair.getID()));
    }

    private static WalletFile getWallet(
            Keys keys, byte[] cipherText, byte[] iv, byte[] salt, byte[] mac,
            int n, int p) {

        WalletFile walletFile = new WalletFile();

        Crypto crypto = new Crypto();
        crypto.setCipher(CIPHER);
        crypto.setCiphertext(StringUtils.toHexStringNoPrefix(cipherText));

        Crypto.CipherParams cipherParams = new Crypto.CipherParams();
        cipherParams.setIv(StringUtils.toHexStringNoPrefix(iv));
        crypto.setCipherparams(cipherParams);
        crypto.setKdf(SCRYPT);

        ScryptKdfParams kdfParams = new ScryptKdfParams();
        kdfParams.setN(n);
        kdfParams.setP(p);
        kdfParams.setR(R);
        kdfParams.setDklen(DKLEN);
        kdfParams.setSalt(StringUtils.toHexStringNoPrefix(salt));

        crypto.setKdfparams(kdfParams);
        crypto.setMac(StringUtils.toHexStringNoPrefix(mac));

        walletFile.setCrypto(crypto);
        walletFile.setId(UUID.randomUUID().toString());
        walletFile.setVersion(CURRENT_VERSION);
        walletFile.setAlias(keys.getAlias());
        walletFile.setXpub(keys.getXPub());
        walletFile.setType(keys.getKeyType());

        return walletFile;
    }

    public static String createStandard(String password, Keys ecKeyPair)
            throws Exception {
        return create(password, ecKeyPair, N_STANDARD, P_STANDARD);
    }

    public static String createLight(String password, Keys ecKeyPair) throws Exception {
        return create(password, ecKeyPair, N_LIGHT, P_LIGHT);
    }

    private static byte[] generateDerivedScryptKey(
            byte[] password, byte[] salt, int n, int r, int p, int dkLen) throws Exception {
        try {
            return SCrypt.scrypt(password, salt, n, r, p, dkLen);
        } catch (GeneralSecurityException e) {
            throw new Exception(e);
        }
    }

    private static byte[] performCipherOperation(
            int mode, byte[] iv, byte[] encryptKey, byte[] text) throws Exception {
        try {
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");

            SecretKeySpec secretKeySpec = new SecretKeySpec(encryptKey, "AES");
            cipher.init(mode, secretKeySpec, ivParameterSpec);
            return cipher.doFinal(text);
        } catch (Exception e) {
            return throwCipherException(e);
        }
    }

    private static byte[] throwCipherException(Exception e) throws Exception {
        throw new Exception("Error performing cipher operation", e);
    }

    private static byte[] generateMac(byte[] derivedKey, byte[] cipherText) {
        byte[] result = new byte[16 + cipherText.length];

        System.arraycopy(derivedKey, 16, result, 0, 16);
        System.arraycopy(cipherText, 0, result, 16, cipherText.length);

        return Hash.sha3(result);
    }

    static byte[] generateRandomBytes(int size) {
        byte[] bytes = new byte[size];
        SecureRandom random = new SecureRandom();
        random.nextBytes(bytes);
        return bytes;
    }
}
