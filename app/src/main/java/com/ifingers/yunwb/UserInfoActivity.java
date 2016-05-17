package com.ifingers.yunwb;

import android.content.Intent;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.ifingers.yunwb.utility.GuiHelper;
import com.ifingers.yunwb.utility.ServerAPI;

public class UserInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);
        GuiHelper.setActionBarTitle(this, "个人信息");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Intent it = getIntent();

        ImageView img = (ImageView) findViewById(R.id.user_image);
        img.setImageBitmap(ServerAPI.getInstance().getImageBitmap(it.getStringExtra("url")));

        TextView view = (TextView)findViewById(R.id.meeting_nick);
        view.setText(it.getStringExtra("name"));

        view = (TextView)findViewById(R.id.user_company);
        view.setText(it.getStringExtra("company"));

        view = (TextView)findViewById(R.id.user_title);
        view.setText(it.getStringExtra("title"));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
