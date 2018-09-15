package com.io.wallet.bean;

/**
 * Created by hwj on 2018/9/13.
 */

public class WalletImage {
    public AccountImage account_image;
    public AssetImage asset_image;
    public KeyImages key_images;

    public WalletImage(AccountImage account_image, AssetImage asset_image, KeyImages key_images) {
        this.account_image = account_image;
        this.asset_image = asset_image;
        this.key_images = key_images;
    }
}
