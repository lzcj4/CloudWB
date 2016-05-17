package com.ifingers.yunwb.wxapi;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.ifingers.yunwb.services.WXService;

public class WXEntryActivity extends AppCompatActivity {
    private String tag = "WXEntryActivity";
    private WXService service = WXService.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState)  {
        super.onCreate(savedInstanceState);
        Intent it = getIntent();
        Log.i(tag, "WXEntryActivity onCreate "+ this.toString());
        Log.i(tag, "task id = " + this.getTaskId());
        Log.i(tag, String.format("action = %s, type = %s, flag = %x", it.getAction(), it.getType(), it.getFlags()));

        service.handleIntent(it);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(tag, "WXEntryActivity onResume "+ this.toString());
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(tag, "WXEntryActivity onDestory " + this.toString());
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(tag, "WXEntryActivity onStop " + this.toString());
    }

    @Override
    protected void onNewIntent(Intent it) {
        super.onNewIntent(it);
        Log.i(tag, "WXEntryActivity onNewIntent " + this.toString());
        Log.i(tag, String.format("action = %s, type = %s, flag = %x", it.getAction(), it.getType(), it.getFlags()));
    }
}
