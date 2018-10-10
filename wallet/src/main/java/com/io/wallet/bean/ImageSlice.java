package com.io.wallet.bean;

import com.google.gson.annotations.SerializedName;

/** Created by hwj on 2018/9/13. */
public class ImageSlice {
  public Account account;

  @SerializedName("contractIndex")
  public long contract_index;

  public ImageSlice(Account account, long contract_index) {
    this.account = account;
    this.contract_index = contract_index;
  }
}
