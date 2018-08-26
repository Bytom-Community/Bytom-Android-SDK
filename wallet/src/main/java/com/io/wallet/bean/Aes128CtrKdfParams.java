package com.io.wallet.bean;

/**
 * Created by hwj on 2018/8/24.
 */

public class Aes128CtrKdfParams implements KdfParams {
    private int dklen;
    private int c;
    private String prf;
    private String salt;

    public Aes128CtrKdfParams() {
    }

    public int getDklen() {
        return dklen;
    }

    public void setDklen(int dklen) {
        this.dklen = dklen;
    }

    public int getC() {
        return c;
    }

    public void setC(int c) {
        this.c = c;
    }

    public String getPrf() {
        return prf;
    }

    public void setPrf(String prf) {
        this.prf = prf;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }
}
