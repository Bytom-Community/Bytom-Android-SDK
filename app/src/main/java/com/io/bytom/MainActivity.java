package com.io.bytom;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.io.wallet.bean.Account;
import com.io.wallet.main.BytomWallet;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private String Tag = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String alias = "hwj15348";
        BytomWallet.initWallet(getApplication());
        String key = BytomWallet.createKey(alias, "123");
        Log.d(Tag+"-key", key);
        String list = BytomWallet.listKeys();
        Log.d(Tag+"-listKey", list);
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
        Log.d(Tag+"-account", account);

        String allCounts = BytomWallet.listAccounts();
        Log.d(Tag+"-allCounts", account);


        String accountId = "",accountAlias = "";
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

        String address = BytomWallet.createAccountReceiver(accountId,accountAlias);
        Log.d(Tag+"-address", address);

        String addressList = BytomWallet.listAddress(accountId,accountAlias);
        Log.d(Tag+"-address", address);

    }
}
