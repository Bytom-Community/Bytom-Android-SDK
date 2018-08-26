package com.io.wallet.bean;

import com.io.wallet.utils.StringUtils;

public class Keys {
    public static final int PRIVATE_KEY_SIZE = 32;
    public static final int PUBLIC_KEY_SIZE = 64;
    public static final String PATH = "/key/";

    private String ID;
    private String Alias;
    private String XPub;
    private String KeyType;
    private String XPrv;

    public Keys() {
    }

    public Keys(String ID, String alias, String XPub, String keyType, String XPrv) {
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

    public String getXPub() {
        return XPub;
    }

    public void setXPub(String XPub) {
        this.XPub = XPub;
    }

    public String getKeyType() {
        return KeyType;
    }

    public void setKeyType(String keyType) {
        KeyType = keyType;
    }

    public String getXPrv() {
        return XPrv;
    }

    public void setXPrv(String XPrv) {
        this.XPrv = XPrv;
    }

    public String toJson() {
        return StringUtils.serializer.toJson(this);
    }
}
