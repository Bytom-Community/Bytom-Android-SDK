package com.io.bytom;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.io.wallet.main.BytomWallet;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
  private String Tag = "MainActivity";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    BytomWallet.initWallet(getApplication());
    String alias = "hwj2";
    String key = BytomWallet.createKey(alias, "123");
    Log.d(Tag + "-key", key);
    String lists = BytomWallet.listKeys();
    Log.d(Tag + "-listKey", lists);
    String xpub = "";
    try {
      JSONObject keyObject = new JSONObject(key);
      String result = keyObject.getString("status");
      if (result.equals("success")) {
        xpub = keyObject.getJSONObject("data").getString("xpub");
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }

    String account = BytomWallet.createAccount(alias, 1, xpub);
    Log.d(Tag + "-account", account);

    String allCounts = BytomWallet.listAccounts();
    Log.d(Tag + "-allCounts", allCounts);

    String accountId = "", accountAlias = "";
    try {
      JSONObject keyObject = new JSONObject(account);
      String result = keyObject.getString("status");
      if (result.equals("success")) {
        accountId = keyObject.getJSONObject("data").getString("id");
        accountAlias = keyObject.getJSONObject("data").getString("alias");
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }

    String address = BytomWallet.createAccountReceiver(accountId, accountAlias);
    Log.d(Tag + "-address", address);

    String addressList = BytomWallet.listAddress(accountId, accountAlias);
    Log.d(Tag + "-addressList", addressList);

    String backupWallet = BytomWallet.backupWallet();
    Log.d(Tag + "-backupWallet", backupWallet);

    BytomWallet.restoreWallet(
        "{\"status\":\"success\",\"data\":{\"account_image\":{\"slices\":[{\"account\":{\"type\":\"account\",\"xpubs\":[\"c3659724fa1ef34e3e60d426a91f380642242c9703cb9d2f8f6765097fe8ce9c272c9d9930f0a2e0335e9ef284f5541032f9c3eb5ce861e98504a96ca58aea71\"],\"quorum\":1,\"key_index\":1,\"id\":\"0IPNDP0500A02\",\"alias\":\"marshall\"},\"contract_index\":2}]},\"asset_image\":{\"assets\":[]},\"key_images\":{\"xkeys\":[{\"crypto\":{\"cipher\":\"aes-128-ctr\",\"ciphertext\":\"f0de764ceb528404a889c646ca4f489d1f9c364b17e4245d8a94f8cb9ab7a0895e099b1624d9013233d82a2ca2f6780d99d0002a2aba73c10bd1f189ad28b8ce\",\"cipherparams\":{\"iv\":\"94234ae5ef2ee34c0e89012220d93547\"},\"kdf\":\"scrypt\",\"kdfparams\":{\"dklen\":32,\"n\":4096,\"p\":6,\"r\":8,\"salt\":\"a4ea269598b866c1596cf5930a145589e2a2c8472bf9933e3e35f860606aa7e7\"},\"mac\":\"67c453c8240cc41c570a4e3757186bf399bd566743c88d78c252c663115e4dc7\"},\"id\":\"cabb0a8b-1f91-4c8a-a1a8-0e6dfad0d68b\",\"type\":\"bytom_kd\",\"version\":1,\"alias\":\"marshall\",\"xpub\":\"c3659724fa1ef34e3e60d426a91f380642242c9703cb9d2f8f6765097fe8ce9c272c9d9930f0a2e0335e9ef284f5541032f9c3eb5ce861e98504a96ca58aea71\"}]}}}");
  }
}
