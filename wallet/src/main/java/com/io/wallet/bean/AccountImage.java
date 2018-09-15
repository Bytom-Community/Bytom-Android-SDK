package com.io.wallet.bean;

import java.util.List;

/**
 * Created by hwj on 2018/9/13.
 */

public class AccountImage {
    public List<ImageSlice> slices;

    public AccountImage(List<ImageSlice> slices) {
        this.slices = slices;
    }
}
