package com.io.wallet.main;

import com.io.wallet.bean.Constant;
import com.io.wallet.bean.Keys;
import com.io.wallet.bean.Respon;
import com.io.wallet.bean.Xpub;
import com.io.wallet.crypto.ChainKd;
import com.io.wallet.crypto.Wallet;
import com.io.wallet.utils.StringUtils;

import java.io.IOException;
import java.util.List;

import static com.io.wallet.crypto.Wallet.KEY_TYPE;

/**
 * Created by hwj on 2018/8/20.
 */

public class BytomWallet {

    public static void initWallet(String storagePath) {
        Storage.getInstance().init(storagePath);
    }

    /**
     * creat keyPair by password and alias
     *
     * @param alias
     * @param password
     * @return
     */
    public static String createKey(String alias, String password) {
        if (Storage.getInstance().hasAlias(alias.trim().toLowerCase())) {
            return new Respon(Constant.FAIL, "alias exist").toJson();
        }
        String priKey = ChainKd.rootXPrv();
        String pubKey = StringUtils.byte2hex(ChainKd.deriveXpub(priKey.getBytes()));
        Keys keys = new Keys(StringUtils.getUUID32(), alias, pubKey, KEY_TYPE, priKey);
        Xpub xpub;
        try {
            xpub = new Xpub(pubKey, alias, Wallet.createLight(password, keys));
        } catch (Exception e) {
            e.printStackTrace();
            return new Respon(Constant.FAIL, e.getMessage()).toJson();
        }
        return new Respon(Constant.SUCCESS, xpub).toJson();
    }

    /**
     * list all keyPair
     *
     * @return
     */
    public static String listKeys() {
        List keys;
        try {
            keys = Storage.getInstance().loadKeys();
        } catch (IOException e) {
            e.printStackTrace();
            return new Respon(Constant.FAIL, e.getMessage()).toJson();
        }
        return new Respon(Constant.SUCCESS, keys).toJson();
    }

    public String createAccount(String alias, int quorum, String rootXPub) {
        return "";
    }
}
