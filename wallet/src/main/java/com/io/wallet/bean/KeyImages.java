package com.io.wallet.bean;

import java.util.ArrayList;

/**
 * Created by hwj on 2018/9/13.
 */

public class KeyImages {
    public ArrayList<EncryptedKey> xkeys;

    public KeyImages(ArrayList<EncryptedKey> xkeys) {
        this.xkeys = xkeys;
    }
}
