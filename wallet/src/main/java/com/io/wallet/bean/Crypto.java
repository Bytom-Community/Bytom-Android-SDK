package com.io.wallet.bean;

/**
 * Created by hwj on 2018/8/24.
 */

public class Crypto {

    private String cipher;
    private String ciphertext;
    private CipherParams cipherparams;

    private String kdf;
    private KdfParams kdfparams;

    private String mac;

    public Crypto() {
    }

    public String getCipher() {
        return cipher;
    }

    public void setCipher(String cipher) {
        this.cipher = cipher;
    }

    public String getCiphertext() {
        return ciphertext;
    }

    public void setCiphertext(String ciphertext) {
        this.ciphertext = ciphertext;
    }

    public CipherParams getCipherparams() {
        return cipherparams;
    }

    public void setCipherparams(CipherParams cipherparams) {
        this.cipherparams = cipherparams;
    }

    public String getKdf() {
        return kdf;
    }

    public void setKdf(String kdf) {
        this.kdf = kdf;
    }

    public KdfParams getKdfparams() {
        return kdfparams;
    }

    public void setKdfparams(KdfParams kdfparams) {
        this.kdfparams = kdfparams;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public static class CipherParams {
        private String iv;

        public CipherParams() {
        }

        public String getIv() {
            return iv;
        }

        public void setIv(String iv) {
            this.iv = iv;
        }
    }
}
