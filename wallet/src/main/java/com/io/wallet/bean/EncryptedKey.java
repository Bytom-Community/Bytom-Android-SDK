package com.io.wallet.bean;

import com.io.wallet.utils.Strings;

/**
 * Created by hwj on 2018/8/24.
 */

public class EncryptedKey {
    private Crypto crypto;
    private String id;
    private int version;
    private String alias;
    private String xpub;
    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getXpub() {
        return xpub;
    }

    public void setXpub(String xpub) {
        this.xpub = xpub;
    }

    public EncryptedKey() {
    }

    public Crypto getCrypto() {
        return crypto;
    }

    public void setCrypto(Crypto crypto) {
        this.crypto = crypto;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public EncryptedKey getWalletFile(String wallet) {
        return Strings.serializer.fromJson(wallet, EncryptedKey.class);
    }

    public String toJson() {
        return Strings.serializer.toJson(this);
    }
}
