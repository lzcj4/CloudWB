package com.ifingers.yunwb;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import com.ifingers.yunwb.utility.GuiHelper;

import java.text.SimpleDateFormat;
import java.util.Date;

public class VersionInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_version_info);
        GuiHelper.setActionBarTitle(this, "版本信息");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            TextView date = (TextView) findViewById(R.id.version_date);
            SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd");
            date.setText(sf.format(new Date(pInfo.lastUpdateTime)));

            TextView sn = (TextView) findViewById(R.id.version_sn);
            sn.setText(pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("VersionInfoActivity", Log.getStackTraceString(e));
        }
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
