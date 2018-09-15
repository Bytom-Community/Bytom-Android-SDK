package com.io.wallet.blockchain.keys;

import android.text.TextUtils;

import com.io.wallet.bean.EncryptedKey;
import com.io.wallet.main.BytomWallet;
import com.io.wallet.utils.Strings;
import com.orhanobut.hawk.Hawk;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static com.io.wallet.utils.Constant.ENCRYPTEDKEY;
import static com.io.wallet.utils.Constant.XPUBCACHE;

/**
 * Created by hwj on 2018/9/15.
 */

public class KeyCache {

    private static ArrayList<Xpub> xpubCache = new ArrayList<>();
    private static ArrayList<EncryptedKey> encryptedKeyCache = new ArrayList<>();

    public KeyCache() {
        encryptedKeyCache = Hawk.get(ENCRYPTEDKEY, new ArrayList<EncryptedKey>());
        xpubCache = Hawk.get(XPUBCACHE, new ArrayList<Xpub>());
    }

    public static void addXpu(Xpub xpub) {
        xpubCache.add(xpub);
        Hawk.put(XPUBCACHE, xpubCache);
    }

    public static boolean hasAlias(String alias) {
        if (TextUtils.isEmpty(alias)) return false;
        for (Xpub item : xpubCache) {
            if (item.getAlias().equals(alias.trim().toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public static String saveEncryptedKey(EncryptedKey key, String name) {
        File file;
        PrintStream ps = null;
        try {
            file = new File(BytomWallet.PATH, name);
            if (!file.exists()) file.createNewFile();
            ps = new PrintStream(new FileOutputStream(file));
            ps.println(key.toJson());
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        } finally {
            if (null != ps) {
                ps.close();
            }
            encryptedKeyCache.add(key);
            Hawk.put(ENCRYPTEDKEY, encryptedKeyCache);
        }
        return file.getAbsolutePath();
    }

    public static ArrayList<EncryptedKey> getAllEncryptedKey() {
        return encryptedKeyCache;
    }

    public static EncryptedKey getEncryptedKey(String xpub) {
        for (EncryptedKey key : encryptedKeyCache) {
            if (key.getXpub().equals(xpub)) {
                return key;
            }
        }
        return null;
    }

    public static Xpub getXpub(String xpub) {
        for (Xpub pub : xpubCache) {
            if (pub.getXpub().equals(xpub)) {
                return pub;
            }
        }
        return null;
    }

    public static ArrayList<Xpub> getAllXpub() {
        return xpubCache;
    }

    public static void restore(List<EncryptedKey> encryptedKeys) throws Exception {
        for(EncryptedKey key :encryptedKeys){
            if(hasAlias(key.getAlias())){
                throw new Exception("duplicate key alias");
            }
            saveEncryptedKey(key, Strings.keyFileName(key.getId()));
        }
    }
}
