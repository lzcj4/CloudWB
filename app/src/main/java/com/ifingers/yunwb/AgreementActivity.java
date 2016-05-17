package com.ifingers.yunwb;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.EditText;

import com.ifingers.yunwb.utility.GuiHelper;

public class AgreementActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agreement);

        GuiHelper.setActionBarTitle(this, "用户协议");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        EditText textView = (EditText) findViewById(R.id.agreement_content);
//        StringBuilder sb = new StringBuilder();
//        for (int i = 0; i < 50; i++) {
//            sb.append(i + ". 这里是用户协议\r\n");
//        }
        textView.setText(R.string.agreement);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
