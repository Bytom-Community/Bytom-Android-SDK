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
        BytomWallet.initWallet(getApplication(),this.getFilesDir().getAbsolutePath());
        String key = BytomWallet.createKey("hwj", "123");
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

        String account = BytomWallet.createAccount("marshall", 1, xpub);
        Log.d(Tag+"-account", account);
    }
}
