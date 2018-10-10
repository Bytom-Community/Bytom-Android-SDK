package com.io.wallet.crypto;

import android.text.TextUtils;

import com.google.crypto.tink.subtle.Base64;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.io.wallet.bean.Account;
import com.io.wallet.bean.AccountImage;
import com.io.wallet.bean.AddressResp;
import com.io.wallet.bean.AssetImage;
import com.io.wallet.bean.Crypto;
import com.io.wallet.bean.CtrlProgram;
import com.io.wallet.bean.EncryptedKey;
import com.io.wallet.bean.KeyImages;
import com.io.wallet.bean.ScryptKdfParams;
import com.io.wallet.bean.WalletImage;
import com.io.wallet.blockchain.account.AccountCache;
import com.io.wallet.blockchain.keys.KeyCache;
import com.io.wallet.blockchain.keys.Keys;
import com.io.wallet.blockchain.keys.Xpub;
import com.io.wallet.script.ScriptBuilder;
import com.io.wallet.utils.Constant;
import com.io.wallet.utils.HDUtils;
import com.io.wallet.utils.SegwitAddress;
import com.io.wallet.utils.Strings;
import com.orhanobut.hawk.Hawk;

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

import static com.google.common.base.Preconditions.checkArgument;
import static com.io.wallet.crypto.ChainKd.generateDerivedScryptKey;
import static com.io.wallet.utils.Constant.ACCOUNTKEYSPACE;
import static com.io.wallet.utils.Constant.ADDRESS_JSON;
import static com.io.wallet.utils.Constant.CIPHER;
import static com.io.wallet.utils.Constant.CONTRACTPREFIX;
import static com.io.wallet.utils.Constant.CURRENT_VERSION;
import static com.io.wallet.utils.Constant.KEY_TYPE;
import static com.io.wallet.utils.Strings.generateRandomBytes;
import static com.io.wallet.utils.Strings.jsonToJsonObject;

/** Created by hwj on 2018/8/24. */
public class Wallet {

  /**
   * creat key and save to file
   *
   * @param password
   * @param keyPair
   * @param n
   * @param p
   * @return
   * @throws Exception
   */
  public static String create(String password, Keys keyPair, int n, int p, String keyPath)
      throws Exception {
    byte[] salt = generateRandomBytes(32);
    byte[] derivedKey =
        generateDerivedScryptKey(password.getBytes(), salt, n, Constant.R, p, Constant.DKLEN);
    byte[] encryptKey = Arrays.copyOfRange(derivedKey, 0, 16);
    byte[] iv = generateRandomBytes(16);
    byte[] privateKeyBytes = keyPair.getXPrv();
    byte[] cipherText =
        performCipherOperation(Cipher.ENCRYPT_MODE, iv, encryptKey, privateKeyBytes);
    byte[] mac = ChainKd.generateMac(derivedKey, cipherText);

    EncryptedKey key = getEncryptedKey(keyPair, cipherText, iv, salt, mac, n, p);
    return KeyCache.saveEncryptedKey(key, keyPath);
  }

  /**
   * encrypted key
   *
   * @param keys
   * @param cipherText
   * @param iv
   * @param salt
   * @param mac
   * @param n
   * @param p
   * @return
   */
  private static EncryptedKey getEncryptedKey(
      Keys keys, byte[] cipherText, byte[] iv, byte[] salt, byte[] mac, int n, int p) {
    EncryptedKey encryptedKey = new EncryptedKey();

    Crypto crypto = new Crypto();
    crypto.setCipher(CIPHER);
    crypto.setCiphertext(Strings.byte2hex(cipherText));
    Crypto.CipherParams cipherParams = new Crypto.CipherParams();
    cipherParams.setIv(Strings.byte2hex(iv));
    crypto.setCipherparams(cipherParams);
    crypto.setKdf(Constant.SCRYPT);

    ScryptKdfParams kdfParams = new ScryptKdfParams();
    kdfParams.setN(n);
    kdfParams.setP(p);
    kdfParams.setR(Constant.R);
    kdfParams.setDklen(Constant.DKLEN);
    kdfParams.setSalt(Strings.byte2hex(salt));

    crypto.setKdfparams(kdfParams);
    crypto.setMac(Strings.byte2hex(mac));

    encryptedKey.setCrypto(crypto);
    encryptedKey.setId(UUID.randomUUID().toString());
    encryptedKey.setType(keys.getKeyType());
    encryptedKey.setVersion(CURRENT_VERSION);
    encryptedKey.setAlias(keys.getAlias());
    encryptedKey.setXpub(Strings.byte2hex(keys.getXPub()));

    return encryptedKey;
  }

  public static String createLight(String password, Keys ecKeyPair, String keyPath)
      throws Exception {
    return create(password, ecKeyPair, Constant.N_LIGHT, Constant.P_LIGHT, keyPath);
  }

  public static String createStandard(String password, Keys ecKeyPair) throws Exception {
    return create(
        password,
        ecKeyPair,
        Constant.N_STANDARD,
        Constant.P_STANDARD,
        Strings.keyFileName(ecKeyPair.getID()));
  }

  public static Account createAccount(List<String> rootXPubs, int quorum, String alias)
      throws Exception {
    String normalizedAlias = alias.trim().toLowerCase();
    if (AccountCache.hasAlias(normalizedAlias)) {
      throw new Exception("duplicate account alias");
    }
    int index = AccountCache.getNextAccountIndex();
    Account account = createSigners("account", rootXPubs, quorum, index);
    String id = ChainKd.idGenerate(index);
    account.setId(id);
    account.setAlias(normalizedAlias);
    AccountCache.addAccount(account);
    return account;
  }

  public static CtrlProgram createAddress(String accountId, String accountAlias) throws Exception {
    if (TextUtils.isEmpty(accountAlias)) {
      throw new Exception("alias is error");
    }
    String id = AccountCache.getAccoutnId(accountAlias);
    if (!TextUtils.isEmpty(id)) {
      accountId = id;
    }
    Account curAccount = AccountCache.getAccountById(accountId);
    if (null == curAccount) throw new Exception("alias is not exist");
    CtrlProgram cp;
    if (1 == curAccount.getXpubs().size()) {
      cp = createP2PKH(curAccount, false);
    } else {
      cp = createP2SH(curAccount, false);
    }
    insertControlPrograms(id, cp);
    return cp;
  }

  private static void insertControlPrograms(String id, CtrlProgram cp) {
    String addressStr = Hawk.get(ADDRESS_JSON + id, "");
    JsonObject addressObj = jsonToJsonObject(addressStr);
    String hash = Strings.byte2hex(Hash.sha3(cp.getControlProgram()));
    addressObj.add(CONTRACTPREFIX + hash, new Gson().toJsonTree(cp).getAsJsonObject());
    Hawk.put(ADDRESS_JSON + id, Strings.objectToJson(addressObj));
  }

  private static CtrlProgram createP2PKH(Account account, boolean change) throws Exception {
    int idx = AccountCache.getNextContractIndex(account.getId());
    ArrayList<byte[]> derivedXPubs = getPath(account, ACCOUNTKEYSPACE, idx);
    byte[] pubHash = HDUtils.sha256hash160(derivedXPubs.get(0));
    checkArgument(pubHash.length == 20, "witness program must be 20 bytes for p2wpkh");
    String address =
        SegwitAddress.encode(Constant.BECH32HRPSEGWI.getBytes(), Constant.WITNESSVERSION, pubHash);
    byte[] control = new ScriptBuilder().data(pubHash).smallNum(0).build().getProgram();
    return new CtrlProgram(account.getId(), address, idx, control, change);
  }

  private static CtrlProgram createP2SH(Account account, boolean change) throws Exception {
    int idx = AccountCache.getNextContractIndex(account.getId());
    ArrayList<byte[]> derivedXPubs = getPath(account, ACCOUNTKEYSPACE, idx);
    byte[] signScript =
        ScriptBuilder.createMultiSigOutputScript(account.getQuorum(), derivedXPubs).getProgram();
    byte[] scriptHash = HDUtils.sha256hash160(signScript);
    checkArgument(scriptHash.length == 32, "witness program must be 32 bytes for p2wsh");
    String address =
        SegwitAddress.encode(
            Base64.decode(Constant.BECH32HRPSEGWI), Constant.WITNESSVERSION, scriptHash);
    byte[] control = new ScriptBuilder().data(scriptHash).smallNum(0).build().getProgram();
    return new CtrlProgram(account.getId(), address, idx, control, change);
  }

  private static ArrayList<byte[]> getPath(Account account, byte keySpace, int... itemIndexes) {
    ArrayList<byte[]> paths = new ArrayList<>();
    byte[] signerPath = new byte[9];
    signerPath[0] = keySpace;
    signerPath[1] = (byte) account.getKey_index();
    paths.add(signerPath);
    for (int idx : itemIndexes) {
      byte[] idxBytes = new byte[8];
      idxBytes[0] = (byte) idx;
      paths.add(idxBytes);
    }
    return paths;
  }

  public static List<AddressResp> listAddress(String accountID, String accountAlias)
      throws Exception {
    List<AddressResp> addressList = new ArrayList<>();
    Account target = AccountCache.getAccountById(accountID);
    if (null == target) {
      throw new Exception("accountID is Invalid");
    }
    String addressStr = Hawk.get(ADDRESS_JSON + accountID, "");
    JsonObject addressObj = jsonToJsonObject(addressStr);
    Iterator it = addressObj.entrySet().iterator();
    while (it.hasNext()) {
      AddressResp resp = new AddressResp();
      Map.Entry entry = (Map.Entry) it.next();
      JsonObject object = (JsonObject) entry.getValue();
      CtrlProgram cp = new Gson().fromJson(Strings.objectToJson(object), CtrlProgram.class);
      resp.account_alias = accountAlias;
      resp.account_id = accountID;
      resp.address = cp.getAddress();
      resp.control_program = Strings.byte2hex(cp.getControlProgram());
      resp.change = cp.isChange();
      resp.keyIndex = cp.getKeyIndex();
      addressList.add(resp);
    }
    return addressList;
  }

  private static Account createSigners(
      String signerType, List<String> xpubs, int quorum, int accountIndex) throws Exception {
    if (null == xpubs || xpubs.size() == 0) {
      throw new Exception("at least one xpub is required");
    }
    Collections.sort(xpubs);
    for (int i = 1; i < xpubs.size(); i++) {
      if (xpubs.get(i).equals(xpubs.get(i - 1))) {
        throw new Exception(String.format("duplicated key = %s", xpubs.get(i)));
      }
    }

    if (0 == quorum || quorum > xpubs.size()) {
      throw new Exception(
          "quorum must be greater than 1 and less than or equal to the length of xpubs");
    }
    Account account = new Account();
    account.setType(signerType);
    account.setXpubs(xpubs);
    account.setQuorum(quorum);
    account.setKey_index(accountIndex);
    return account;
  }

  public static WalletImage backup() {
    KeyImages keyImages = new KeyImages(KeyCache.getAllEncryptedKey());
    AccountImage accountImage = new AccountImage(AccountCache.backup());
    return new WalletImage(accountImage, new AssetImage(new ArrayList()), keyImages);
  }

  private static byte[] performCipherOperation(int mode, byte[] iv, byte[] encryptKey, byte[] text)
      throws Exception {
    IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
    Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");

    SecretKeySpec secretKeySpec = new SecretKeySpec(encryptKey, "AES");
    cipher.init(mode, secretKeySpec, ivParameterSpec);
    return cipher.doFinal(text);
  }

  public byte[] xSign(byte[] xpub, byte[][] path, byte[] msg, String auth) throws Exception {
    List<EncryptedKey> walletFiles = KeyCache.getAllEncryptedKey();
    EncryptedKey wallet = null;
    for (int i = 0; i < walletFiles.size(); i++) {
      if (walletFiles.get(i).getXpub().equals(xpub)) {
        wallet = walletFiles.get(i);
      }
    }
    if (null == wallet) throw new Exception("xpub is not exis");
    Keys keys = decryptKey(wallet, auth);
    byte[] xprv = keys.getXPrv();
    if (path.length > 0) {
      //            xprv =  HDUtils.expandedPrivateKey(NonHardenedChild.child(xprv, path));
    }
    return Signer.Ed25519InnerSign(xprv, msg);
  }

  public static Keys decryptKey(EncryptedKey keyProtected, String auth) throws Exception {
    if (CURRENT_VERSION != keyProtected.getVersion()) {
      throw new Exception("Version not supported:" + keyProtected.getVersion());
    }
    if (!KEY_TYPE.equals(keyProtected.getType())) {
      throw new Exception("Key type not supported:" + keyProtected.getType());
    }
    if (!CIPHER.equals(keyProtected.getCrypto().getCipher())) {
      throw new Exception("Cipher not supported:" + keyProtected.getCrypto().getCipher());
    }
    byte[] derivedKey = getKDFKey(keyProtected.getCrypto(), auth);
    byte[] calculatedMAC =
        ChainKd.generateMac(derivedKey, keyProtected.getCrypto().getCiphertext().getBytes());
    if (!keyProtected.getCrypto().getMac().equals(calculatedMAC)) {
      throw new Exception("MAC is error");
    }
    byte[] encryptKey = Arrays.copyOfRange(derivedKey, 0, 16);
    byte[] cipherText = keyProtected.getCrypto().getCiphertext().getBytes();
    byte[] iv = keyProtected.getCrypto().getCipherparams().getIv().getBytes();
    byte[] xprv = performCipherOperation(Cipher.ENCRYPT_MODE, iv, encryptKey, cipherText);
    byte[] xpub = ChainKd.deriveXpub(xprv);
    return new Keys(
        keyProtected.getId(), keyProtected.getAlias(), xpub, keyProtected.getType(), xprv);
  }

  public static byte[] getKDFKey(Crypto cryptoJSON, String auth) throws Exception {
    ScryptKdfParams params = cryptoJSON.getKdfparams();
    byte[] authArray = auth.getBytes();
    byte[] salt = params.getSalt().getBytes();
    int dkLen = params.getDklen();
    if ("scrypt".equals(cryptoJSON.getKdf())) {
      return generateDerivedScryptKey(
          authArray, salt, params.getN(), params.getR(), params.getP(), dkLen);
    } else if ("pbkdf2".equals(cryptoJSON.getKdf())) {
      // TODO
    }
    throw new Exception("Unsupported KDF");
  }

  public static Keys loadDecryptKeyByXpub(String xpub, String auth) throws Exception {
    EncryptedKey key = KeyCache.getEncryptedKey(xpub);
    if (null != key) {
      return decryptKey(key, auth);
    }
    throw new Exception("load decrypt key failure");
  }

  public static void resetPassword(String xpub, String oldAuth, String newAuth) throws Exception {
    Xpub xpubObj = KeyCache.getXpub(xpub);
    if (null == xpubObj || TextUtils.isEmpty(oldAuth) || TextUtils.isEmpty(newAuth))
      throw new Exception("illegality input");
    Keys keys = loadDecryptKeyByXpub(xpub, oldAuth);
    Wallet.createLight(newAuth, keys, xpubObj.getFile());
  }

  public static void restoreWalletImage(WalletImage image) throws Exception {
    KeyCache.restore(image.key_images.xkeys);
    AccountCache.restore(image.account_image.slices);
  }
}
