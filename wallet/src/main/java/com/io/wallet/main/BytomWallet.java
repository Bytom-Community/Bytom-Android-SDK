package com.io.wallet.main;

import android.content.Context;

import com.io.wallet.bean.Account;
import com.io.wallet.bean.CtrlProgram;
import com.io.wallet.bean.Keys;
import com.io.wallet.bean.RawTransaction;
import com.io.wallet.bean.Respon;
import com.io.wallet.bean.Template;
import com.io.wallet.bean.Xpub;
import com.io.wallet.crypto.ChainKd;
import com.io.wallet.crypto.Wallet;
import com.io.wallet.utils.Constant;
import com.io.wallet.utils.Signatures;
import com.io.wallet.utils.StringUtils;
import com.orhanobut.hawk.Hawk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.io.wallet.utils.Constant.KEY_TYPE;


/**
 * Created by hwj on 2018/8/20.
 */

public class BytomWallet {

    public static void initWallet(Context context, String storagePath) {
        Storage.getInstance().init(context, storagePath);
        Hawk.init(context).build();
    }

    /**
     * creat keyPair by password and alias
     *
     * @param alias
     * @param password
     * @return
     */
    public static String createKey(String alias, String password) {
        if (Storage.hasXpubAlias(alias)) {
            return new Respon(Constant.FAIL, "alias exist").toJson();
        }
        byte[] priKey = ChainKd.rootXPrv();
        byte[] pubKey = ChainKd.deriveXpub(priKey);
        Keys keys = new Keys(StringUtils.getUUID32(), alias, pubKey, KEY_TYPE, priKey);
        Xpub xpub;
        try {
            xpub = new Xpub(StringUtils.byte2hex(pubKey), alias, Wallet.createLight(password, keys));
            Storage.addXpubCache(xpub);
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
        } catch (Exception e) {
            e.printStackTrace();
            return new Respon(Constant.FAIL, e.getMessage()).toJson();
        }
        return new Respon(Constant.SUCCESS, keys).toJson();
    }

    /**
     * create acount
     *
     * @param alias
     * @param quorum
     * @param rootXPub
     * @return
     */
    public static String createAccount(String alias, int quorum, String rootXPub) {
        Account account;
        try {
            account = Wallet.creatAcount(new ArrayList(Arrays.asList(StringUtils.getUnmarshalText(rootXPub))), quorum, alias);
        } catch (Exception e) {
            e.printStackTrace();
            return new Respon(Constant.FAIL, e.getMessage()).toJson();
        }
        return new Respon(Constant.SUCCESS, account).toJson();
    }

    /**
     * list all account
     *
     * @return
     */
    public static String listAccounts() {
        return new Respon(Constant.SUCCESS, Wallet.listAccounts()).toJson();
    }

    /**
     * creat address
     *
     * @param accountId
     * @param accountAlias
     * @return
     */
    public static String createAccountReceiver(String accountId, String accountAlias) {
        CtrlProgram ctrlProgram;
        try {
            ctrlProgram = Wallet.createAddress(accountId, accountAlias);
        } catch (Exception e) {
            e.printStackTrace();
            return new Respon(Constant.FAIL, e.getMessage()).toJson();
        }

        return new Respon(Constant.SUCCESS, ctrlProgram.getAddress()).toJson();
    }

    /**
     * list all address
     *
     * @param accountID
     * @param accountAlias
     * @return
     */
    public static String listAddress(String accountID, String accountAlias) {
        try {
            return new Respon(Constant.SUCCESS, Wallet.listAddress(accountID, accountAlias)).toJson();
        } catch (Exception e) {
            e.printStackTrace();
            return new Respon(Constant.FAIL, e.getMessage()).toJson();
        }
    }

    /**
     * backup Wallet
     *
     * @return
     */
    public static String backupWalletImage() {
        return new Respon(Constant.SUCCESS, Wallet.backup()).toJson();
    }


    /**
     * sign transaction
     *
     * @param privateKeys
     * @param template
     * @param decodedTx
     * @return
     */
    public String signTransaction(String[] privateKeys, Template template, RawTransaction decodedTx) {
        return new Respon(Constant.SUCCESS, Signatures.generateSignatures(privateKeys, template, decodedTx)).toJson();
    }
}
