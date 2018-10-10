package com.io.wallet.bean;

import com.google.gson.annotations.SerializedName;

/** Created by hwj on 2018/9/13. */
public class WalletImage {
  @SerializedName("accountImage")
  public AccountImage account_image;

  @SerializedName("assetImage")
  public AssetImage asset_image;

  @SerializedName("keyImages")
  public KeyImages key_images;

  public WalletImage(AccountImage account_image, AssetImage asset_image, KeyImages key_images) {
    this.account_image = account_image;
    this.asset_image = asset_image;
    this.key_images = key_images;
  }
}
