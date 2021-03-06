package com.io.wallet.blockchain.keys;

import com.io.wallet.utils.Strings;

public class Keys {
    private String ID;
    private String Alias;
    private byte[] XPub;
    private String KeyType;
    private byte[] XPrv;

    public Keys() {
    }

    public Keys(String ID, String alias, byte[] XPub, String keyType, byte[] XPrv) {
        this.ID = ID;
        Alias = alias;
        this.XPub = XPub;
        KeyType = keyType;
        this.XPrv = XPrv;
    }

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public String getAlias() {
        return Alias;
    }

    public void setAlias(String alias) {
        Alias = alias;
    }

    public byte[] getXPub() {
        return XPub;
    }

    public void setXPub(byte[] XPub) {
        this.XPub = XPub;
    }

    public String getKeyType() {
        return KeyType;
    }

    public void setKeyType(String keyType) {
        KeyType = keyType;
    }

    public byte[] getXPrv() {
        return XPrv;
    }

    public void setXPrv(byte[] XPrv) {
        this.XPrv = XPrv;
    }

    public String toJson() {
        return Strings.serializer.toJson(this);
    }
}
