package com.ifingers.yunwb;

import android.content.Intent;
import android.media.ThumbnailUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.ifingers.yunwb.utility.GuiHelper;
import com.ifingers.yunwb.utility.ServerAPI;
import com.ifingers.yunwb.utility.WhiteboardTaskContext;

public class HelperMainActivity extends AppCompatActivity {

    private final int REQUEST_CODE_MY_INFO = 1;

    private void renderInfo(){
        ImageView imageView = (ImageView)findViewById(R.id.user_image);
        imageView.setImageBitmap(ThumbnailUtils.extractThumbnail(ServerAPI.getInstance().getImageBitmap(WhiteboardTaskContext.getInstance().getUserInfo().getAvatarUrl()), 200, 200));

        TextView textView = (TextView)findViewById(R.id.user_name);
        textView.setText(WhiteboardTaskContext.getInstance().getUserInfo().getName());
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_helper_main);
        GuiHelper.setActionBarTitle(this, "关于");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        renderInfo();

        Button btnInfo = (Button)findViewById(R.id.action_my_info);
        btnInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent it = new Intent(HelperMainActivity.this, MyInfoActivity.class);
                startActivityForResult(it, REQUEST_CODE_MY_INFO);
            }
        });

        Button btnAbout = (Button)findViewById(R.id.action_about);
        btnAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent it = new Intent(HelperMainActivity.this, AboutActivity.class);
                startActivity(it);
            }
        });

        Button btnVersion = (Button) findViewById(R.id.action_version);
        btnVersion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent it = new Intent(HelperMainActivity.this, VersionInfoActivity.class);
                startActivity(it);
            }
        });

        Button btnLogout = (Button) findViewById(R.id.action_logout);
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WhiteboardTaskContext.getInstance().clearUserInfoAtLocal();
                WhiteboardTaskContext.getInstance().setUserInfo(null);
                finish();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_MY_INFO){
            if(resultCode == RESULT_OK){
                renderInfo();
            }
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
