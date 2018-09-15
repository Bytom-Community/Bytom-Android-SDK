package com.io.wallet.bean;

/**
 * Created by hwj on 2018/9/13.
 */

public class ImageSlice {
    public Account account;
    public long contract_index;

    public ImageSlice(Account account, long contract_index) {
        this.account = account;
        this.contract_index = contract_index;
    }
}
