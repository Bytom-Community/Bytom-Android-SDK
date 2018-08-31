package com.io.wallet.crypto;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.io.wallet.bean.Account;
import com.io.wallet.bean.Crypto;
import com.io.wallet.bean.CtrlProgram;
import com.io.wallet.bean.Keys;
import com.io.wallet.bean.ScryptKdfParams;
import com.io.wallet.bean.WalletFile;
import com.io.wallet.main.Storage;
import com.io.wallet.utils.Constant;
import com.io.wallet.utils.SpUtil;
import com.io.wallet.utils.StringUtils;
import com.lambdaworks.crypto.SCrypt;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static com.io.wallet.utils.Constant.ACCOUNTS_JSON;
import static com.io.wallet.utils.StringUtils.jsonToJsonObject;

/**
 * Created by hwj on 2018/8/24.
 */

public class Wallet {

    public static String create(String password, Keys keyPair, int n, int p) throws Exception {
        byte[] salt = generateRandomBytes(32);
        byte[] derivedKey = generateDerivedScryptKey(
                password.getBytes(Charset.forName("UTF-8")), salt, n, Constant.R, p, Constant.DKLEN);
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
        crypto.setCipher(Constant.CIPHER);
        crypto.setCiphertext(StringUtils.toHexStringNoPrefix(cipherText));

        Crypto.CipherParams cipherParams = new Crypto.CipherParams();
        cipherParams.setIv(StringUtils.toHexStringNoPrefix(iv));
        crypto.setCipherparams(cipherParams);
        crypto.setKdf(Constant.SCRYPT);

        ScryptKdfParams kdfParams = new ScryptKdfParams();
        kdfParams.setN(n);
        kdfParams.setP(p);
        kdfParams.setR(Constant.R);
        kdfParams.setDklen(Constant.DKLEN);
        kdfParams.setSalt(StringUtils.toHexStringNoPrefix(salt));

        crypto.setKdfparams(kdfParams);
        crypto.setMac(StringUtils.toHexStringNoPrefix(mac));

        walletFile.setCrypto(crypto);
        walletFile.setId(UUID.randomUUID().toString());
        walletFile.setVersion(Constant.CURRENT_VERSION);
        walletFile.setAlias(keys.getAlias());
        walletFile.setXpub(keys.getXPub());
        walletFile.setType(keys.getKeyType());

        return walletFile;
    }

    public static String createStandard(String password, Keys ecKeyPair)
            throws Exception {
        return create(password, ecKeyPair, Constant.N_STANDARD, Constant.P_STANDARD);
    }

    public static Account creatAcount(List rootXPub, int quorum, String alias) throws Exception {
        String normalizedAlias = alias.trim().toLowerCase();
        if (!TextUtils.isEmpty(SpUtil.getString("AccountAlias:" + normalizedAlias)))
            throw new Exception("alias is exist");
        int index = getNextAccountIndex();
        Account account = createSigners("account", rootXPub, quorum, index);
        String id = idGenerate(index);
        account.setId(id);
        account.setAlias(normalizedAlias);
        String accountStr = SpUtil.getString(ACCOUNTS_JSON, "");
        JsonObject accountObj = jsonToJsonObject(accountStr);
        accountObj.add(id, new Gson().toJsonTree(account).getAsJsonObject());
        SpUtil.putString(ACCOUNTS_JSON, StringUtils.objectToJson(accountObj));
        SpUtil.putString("AccountAlias:" + normalizedAlias, id);
        return account;
    }

    public static CtrlProgram createAddress(String accountId, String accountAlias) throws Exception {
        String id = SpUtil.getString("AccountAlias:" + accountAlias.trim().toLowerCase(), "");
        if (TextUtils.isEmpty(id)) throw new Exception("alias is not exist");
        String accountStr = SpUtil.getString(ACCOUNTS_JSON, "");
        JsonObject accountObj = jsonToJsonObject(accountStr);
        Account curAccount = new Gson().fromJson(StringUtils.objectToJson(accountObj.get(id)), Account.class);
        if (null == curAccount) throw new Exception("alias is not exist");
        CtrlProgram cp;
        if (1 == curAccount.getXpubs().size()) {
            cp = createP2PKH(curAccount, false);
        } else {
            cp = createP2SH(curAccount, false);
        }
        return cp;
    }

    private static CtrlProgram createP2PKH(Account account, boolean change) {
        return new CtrlProgram();
    }

    private static CtrlProgram createP2SH(Account account, boolean change) {
        return new CtrlProgram();
    }

    public static List listAccounts() {
        List accounts = new ArrayList();
        String accountStr = SpUtil.getString(ACCOUNTS_JSON, "");
        JsonObject accountObj = jsonToJsonObject(accountStr);
        Iterator it = accountObj.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            accounts.add(entry.getValue());
        }
        return accounts;
    }

    private static Account createSigners(String signerType, List xpubs, int quorum, int accountIndex) throws Exception {
        if (null == xpubs || xpubs.size() == 0) {
            throw new Exception("xpubs is empty");
        }

        Collections.sort(xpubs);
        for (int i = 1; i < xpubs.size(); i++) {
            if (xpubs.get(i).equals(xpubs.get(i - 1))) {
                throw new Exception(String.format("duplicated key = %s", xpubs.get(i)));
            }
        }

        if (0 == quorum || quorum > xpubs.size()) {
            throw new Exception("quorum is error");
        }
        Account account = new Account();
        account.setType(signerType);
        account.setXpubs(xpubs);
        account.setQuorum(quorum);
        account.setKey_index(accountIndex);
        return account;
    }

    private static String idGenerate(int index) {
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

    private static int getNextAccountIndex() {
        int nextIndex = SpUtil.getInt("accountIndexKey", 1);
        SpUtil.putInt("accountIndexKey", nextIndex + 1);
        return nextIndex;
    }

    public static String createLight(String password, Keys ecKeyPair) throws Exception {
        return create(password, ecKeyPair, Constant.N_LIGHT, Constant.P_LIGHT);
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
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");

        SecretKeySpec secretKeySpec = new SecretKeySpec(encryptKey, "AES");
        cipher.init(mode, secretKeySpec, ivParameterSpec);
        return cipher.doFinal(text);
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
