package com.io.wallet.main;

import android.content.Context;
import android.text.TextUtils;

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
  public static Respon createKey(String alias, String password) {
    String normalizedAlias = alias.trim().toLowerCase();
    if (TextUtils.isEmpty(normalizedAlias) || TextUtils.isEmpty(password)) {
      return new Respon<>(Constant.FAIL, "invalid alias or password");
    }

    if (KeyCache.hasAlias(normalizedAlias)) {
      return new Respon<>(Constant.FAIL, "duplicate key alias");
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
      return new Respon<>(Constant.FAIL, e.getMessage());
    }
    return new Respon<>(Constant.SUCCESS, xpub);
  }

  /**
   * list all keyPair
   *
   * @return
   */
  public static Respon listKeys() {
    return new Respon<>(Constant.SUCCESS, KeyCache.getAllXpub());
  }

  /**
   * create account
   *
   * @param alias
   * @param quorum
   * @param rootXPub
   * @return
   */
  public static Respon createAccount(String alias, int quorum, String rootXPub) {
    Account account;
    try {
      account =
          Wallet.createAccount(
              new ArrayList<>(Arrays.asList(Strings.getUnmarshalText(rootXPub))), quorum, alias);
    } catch (Exception e) {
      return new Respon<>(Constant.FAIL, e.getMessage());
    }
    return new Respon<>(Constant.SUCCESS, account);
  }

  /**
   * list all account
   *
   * @return
   */
  public static Respon listAccounts() {
    return new Respon<>(Constant.SUCCESS, AccountCache.getAllAccount());
  }

  /**
   * create address
   *
   * @param accountId
   * @param accountAlias
   * @return
   */
  public static Respon createAccountReceiver(String accountId, String accountAlias) {
    CtrlProgram ctrlProgram;
    try {
      ctrlProgram = Wallet.createAddress(accountId, accountAlias);
    } catch (Exception e) {
      return new Respon<>(Constant.FAIL, e.getMessage());
    }

    return new Respon<>(
        Constant.SUCCESS,
        new Receiver(Strings.byte2hex(ctrlProgram.getControlProgram()), ctrlProgram.getAddress()));
  }

  /**
   * list all address
   *
   * @param accountID
   * @param accountAlias
   * @return
   */
  public static Respon listAddress(String accountID, String accountAlias) {
    try {
      return new Respon<>(Constant.SUCCESS, Wallet.listAddress(accountID, accountAlias));
    } catch (Exception e) {
      return new Respon<>(Constant.FAIL, e.getMessage());
    }
  }

  /**
   * backup Wallet
   *
   * @return
   */
  public static Respon backupWallet() {
    return new Respon<>(Constant.SUCCESS, Wallet.backup());
  }

  /**
   * reset root key password
   *
   * @param rootXPub
   * @param oldPassword
   * @param newPassword
   * @return
   */
  public static Respon resetKeyPassword(String rootXPub, String oldPassword, String newPassword) {
    try {
      Wallet.resetPassword(rootXPub, oldPassword, newPassword);
      return new Respon<>(Constant.SUCCESS, new ResetPasswordResp(true));
    } catch (Exception e) {
      return new Respon<>(Constant.FAIL, new ResetPasswordResp(false));
    }
  }

  /**
   * restore wallet image
   *
   * @param walletImage
   * @return
   */
  public static Respon restoreWallet(String walletImage) {
    try {
      Wallet.restoreWalletImage(Strings.serializer.fromJson(walletImage, WalletImage.class));
    } catch (Exception e) {
      return new Respon<>(Constant.FAIL, "Invalid image string");
    }
    return new Respon<>(Constant.SUCCESS, "");
  }

  /**
   * sign transaction
   *
   * @param privateKeys
   * @param template
   * @param decodedTx
   * @return
   */
  public Respon signTransaction(String[] privateKeys, Template template, RawTransaction decodedTx) {
    return new Respon<>(
        Constant.SUCCESS, Signatures.generateSignatures(privateKeys, template, decodedTx));
  }
}
