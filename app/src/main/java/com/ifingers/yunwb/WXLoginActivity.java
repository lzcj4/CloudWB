package com.ifingers.yunwb;

import android.content.Intent;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ifingers.yunwb.services.WXService;
import com.ifingers.yunwb.utility.ActivityCode;
import com.ifingers.yunwb.utility.WhiteboardTaskContext;

public class WXLoginActivity extends AppCompatActivity {
    private String tag = "WXLoginActivity";
    private CheckBox mGrantAgreementCheckBox;
    private LinearLayout mWXLoginButton;
    private TextView mTextAgreement;
    private WXService service = WXService.getInstance();
    private WhiteboardTaskContext globalContext = WhiteboardTaskContext.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitNetwork().build();
        StrictMode.setThreadPolicy(policy);

        super.onCreate(savedInstanceState);

        Log.i(tag, "WXLoginActivity onCreate " + this.toString());
        Log.i(tag, "task id = " + this.getTaskId());
        Intent it = getIntent();
        Log.i(tag, String.format("action = %s, type = %s, flag = %x", it.getAction(), it.getType(), it.getFlags()));

        setContentView(R.layout.activity_wxlogin);
        getSupportActionBar().hide();

        mWXLoginButton = (LinearLayout) findViewById(R.id.wxlogin);
        mGrantAgreementCheckBox = (CheckBox) findViewById(R.id.grantAgreement);
        mGrantAgreementCheckBox.requestFocus();

        mTextAgreement = (TextView) findViewById(R.id.textAgreement);
        mTextAgreement.setText(Html.fromHtml(mTextAgreement.getText().toString()));

        mWXLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mGrantAgreementCheckBox.isChecked()) {
                    Toast.makeText(WXLoginActivity.this, "正在跳转到微信授权界面", Toast.LENGTH_SHORT).show();
                    service.startLogin();
                } else {
                    Toast.makeText(WXLoginActivity.this, "请确认同意用户协议", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mTextAgreement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(WXLoginActivity.this, AgreementActivity.class));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(tag, "WXLoginActivity onResume "+ this.toString());
        if (globalContext.getUserInfo() != null) {
            setResult(ActivityCode.RESULT_WEIXIN_LOGIN_OK);
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(tag, "WXLoginActivity onDestory " + this.toString());
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(tag, "WXLoginActivity onStop " + this.toString());
    }

    @Override
    protected void onNewIntent(Intent it) {
        super.onNewIntent(it);
        Log.i(tag, "WXLoginActivity onNewIntent " + this.toString());
        Log.i(tag, String.format("action = %s, type = %s, flag = %x", it.getAction(), it.getType(), it.getFlags()));
    }

    @Override
    public void onBackPressed() {
        if (globalContext.getUserInfo() != null)
            super.onBackPressed();
        else {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            startActivity(intent);
        }
    }
}
