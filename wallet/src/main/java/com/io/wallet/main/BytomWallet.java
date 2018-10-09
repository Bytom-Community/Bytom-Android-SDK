package com.io.wallet.main;

import android.content.Context;
import android.text.TextUtils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.io.wallet.bean.Account;
import com.io.wallet.bean.CtrlProgram;
import com.io.wallet.bean.RawTransaction;
import com.io.wallet.bean.Receiver;
import com.io.wallet.bean.ResetPasswordResp;
import com.io.wallet.bean.Respon;
import com.io.wallet.bean.Template;
import com.io.wallet.bean.WalletImage;
import com.io.wallet.blockchain.account.AccountCache;
import com.io.wallet.blockchain.keys.KeyCache;
import com.io.wallet.blockchain.keys.Keys;
import com.io.wallet.blockchain.keys.Xpub;
import com.io.wallet.crypto.ChainKd;
import com.io.wallet.crypto.Wallet;
import com.io.wallet.utils.Constant;
import com.io.wallet.utils.Signatures;
import com.io.wallet.utils.Strings;
import com.orhanobut.hawk.Hawk;

import java.util.ArrayList;
import java.util.Arrays;

import static com.io.wallet.utils.Constant.KEY_TYPE;

/** Created by hwj on 2018/8/20. */
public class BytomWallet {
  public static String PATH;

  public static void initWallet(Context context) {
    PATH = context.getFilesDir().getAbsolutePath();
    Hawk.init(context).build();
  }

  /**
   * create keyPair by password and alias
   *
   * @param alias
   * @param password
   * @return
   */
  public static String createKey(String alias, String password) {
    String normalizedAlias = alias.trim().toLowerCase();
    if (TextUtils.isEmpty(normalizedAlias) || TextUtils.isEmpty(password)) {
      return new Respon<>(Constant.FAIL, "invalid alias or password").toJson();
    }

    if (KeyCache.hasAlias(normalizedAlias)) {
      return new Respon<>(Constant.FAIL, "duplicate key alias").toJson();
    }
    byte[] priKey = ChainKd.rootXPrv();
    byte[] pubKey = ChainKd.deriveXpub(priKey);
    Keys keys = new Keys(Strings.getUUID32(), normalizedAlias, pubKey, KEY_TYPE, priKey);
    Xpub xpub;
    try {
      xpub =
          new Xpub(
              Strings.byte2hex(pubKey),
              normalizedAlias,
              Wallet.createLight(password, keys, Strings.keyFileName(keys.getID())));
      KeyCache.addXpu(xpub);
    } catch (Exception e) {
      return new Respon<>(Constant.FAIL, e.getMessage()).toJson();
    }
    return new Respon<>(Constant.SUCCESS, xpub).toJson();
  }

  /**
   * list all keyPair
   *
   * @return
   */
  public static String listKeys() {
    return new Respon<>(Constant.SUCCESS, KeyCache.getAllXpub()).toJson();
  }

  /**
   * create account
   *
   * @param alias
   * @param quorum
   * @param rootXPub
   * @return
   */
  public static String createAccount(String alias, int quorum, String rootXPub) {
    Account account;
    try {
      account =
          Wallet.createAccount(
              new ArrayList<>(Arrays.asList(Strings.getUnmarshalText(rootXPub))), quorum, alias);
    } catch (Exception e) {
      return new Respon<>(Constant.FAIL, e.getMessage()).toJson();
    }
    return new Respon<>(Constant.SUCCESS, account).toJson();
  }

  /**
   * list all account
   *
   * @return
   */
  public static String listAccounts() {
    return new Respon<>(Constant.SUCCESS, AccountCache.getAllAccount()).toJson();
  }

  /**
   * create address
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
      return new Respon<>(Constant.FAIL, e.getMessage()).toJson();
    }

    return new Respon<>(
            Constant.SUCCESS,
            new Receiver(
                Strings.byte2hex(ctrlProgram.getControlProgram()), ctrlProgram.getAddress()))
        .toJson();
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
      return new Respon<>(Constant.SUCCESS, Wallet.listAddress(accountID, accountAlias)).toJson();
    } catch (Exception e) {
      return new Respon<>(Constant.FAIL, e.getMessage()).toJson();
    }
  }

  /**
   * backup Wallet
   *
   * @return
   */
  public static String backupWallet() {
    return new Respon<>(Constant.SUCCESS, Wallet.backup()).toJson();
  }

  /**
   * reset root key password
   *
   * @param rootXPub
   * @param oldPassword
   * @param newPassword
   * @return
   */
  public static String resetKeyPassword(String rootXPub, String oldPassword, String newPassword) {
    try {
      Wallet.resetPassword(rootXPub, oldPassword, newPassword);
      return new Respon<>(Constant.SUCCESS, new ResetPasswordResp(true)).toJson();
    } catch (Exception e) {
      return new Respon<>(Constant.FAIL, new ResetPasswordResp(false)).toJson();
    }
  }

  /**
   * restore wallet image
   *
   * @param walletImage
   * @return
   */
  public static String restoreWallet(String walletImage) {
    try {
      JsonObject object = new JsonParser().parse(walletImage).getAsJsonObject();
      Wallet.restoreWalletImage(Strings.serializer.fromJson(object.get("data"), WalletImage.class));
    } catch (Exception e) {
      return new Respon<>(Constant.FAIL, "Invalid image string").toJson();
    }
    return new Respon<>(Constant.SUCCESS, "").toJson();
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
    return new Respon<>(
            Constant.SUCCESS, Signatures.generateSignatures(privateKeys, template, decodedTx))
        .toJson();
  }
}
