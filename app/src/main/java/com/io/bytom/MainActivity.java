package com.io.bytom;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.io.wallet.main.BytomWallet;

public class MainActivity extends AppCompatActivity {
    private String Tag = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BytomWallet.initWallet(this.getFilesDir().getAbsolutePath());
        String key = BytomWallet.createKey("hwj", "123");
        Log.d(Tag+"-key", key);
        String list = BytomWallet.listKeys();
        Log.d(Tag+"-listKey", list);
    }
}
