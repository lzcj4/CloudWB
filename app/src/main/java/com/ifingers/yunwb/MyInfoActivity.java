package com.ifingers.yunwb;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Toast;

import com.ifingers.yunwb.utility.GuiHelper;
import com.ifingers.yunwb.utility.ServerAPI;
import com.ifingers.yunwb.utility.ServerError;
import com.ifingers.yunwb.utility.WhiteboardTaskContext;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;

public class MyInfoActivity extends AppCompatActivity {

    private final String TAG = "MyInfoActivity";
    private final int IMAGE_REQUEST_CODE = 0;
    private Uri newImageFile;
    private ImageView imageView;
    private AutoCompleteTextView username;
    private AutoCompleteTextView usercompany;
    private AutoCompleteTextView usertitle;
    private AutoCompleteTextView usergender;
    private WhiteboardTaskContext config = WhiteboardTaskContext.getInstance();

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.wizard, menu);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_info);
        GuiHelper.setActionBarTitle(this, "我的信息");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        imageView = (ImageView) findViewById(R.id.user_image);
        imageView.setImageBitmap(ServerAPI.getInstance().getImageBitmap(config.getUserInfo().getAvatarUrl()));
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentFromGallery = new Intent();
                intentFromGallery.setType("image/*");
                intentFromGallery
                        .setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intentFromGallery,
                        IMAGE_REQUEST_CODE);
            }
        });

        username = (AutoCompleteTextView) findViewById(R.id.user_name);
        username.setText(config.getUserInfo().getName());

        usercompany = (AutoCompleteTextView) findViewById(R.id.user_company);
        usercompany.setText(config.getUserInfo().getCompany());

        usertitle = (AutoCompleteTextView) findViewById(R.id.user_title);
        usertitle.setText(config.getUserInfo().getJobTitle());

        usergender = (AutoCompleteTextView) findViewById(R.id.user_gender);
        usergender.setText(config.getUserInfo().getGender());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_CANCELED) {

            switch (requestCode) {
                case IMAGE_REQUEST_CODE:
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    try {
                        newImageFile = data.getData();
                        imageView.setImageBitmap(BitmapFactory.decodeStream(getContentResolver().openInputStream(newImageFile), null, options));
                    } catch (FileNotFoundException e) {
                        Log.e(TAG, Log.getStackTraceString(e));
                    }
                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * get real path from url
     * @param contentUri
     * @return
     */
    private String getRealPathFromURI(Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_save:
                boolean needUpdate = false;
                if ("".equals(username.getText().toString())){
                    Toast.makeText(this, "请输入姓名", Toast.LENGTH_LONG).show();
                    return true;
                } else if (!config.getUserInfo().getName().equals(username.getText().toString())) {
                    config.getUserInfo().setName(username.getText().toString());
                    needUpdate = true;
                }

                if ("".equals(usergender.getText().toString())){
                    Toast.makeText(this, "请输入性别", Toast.LENGTH_LONG).show();
                    return true;
                } else if (config.getUserInfo().getGender() == null || !config.getUserInfo().getGender().equals(usergender.getText().toString())) {
                    config.getUserInfo().setGender(usergender.getText().toString());
                    needUpdate = true;
                }

                if ("".equals(usercompany.getText().toString())){
                    Toast.makeText(this, "请输入公司", Toast.LENGTH_LONG).show();
                    return true;
                } else if (!config.getUserInfo().getCompany().equals(usercompany.getText().toString())) {
                    config.getUserInfo().setCompany(usercompany.getText().toString());
                    needUpdate = true;
                }

                if ("".equals(usertitle.getText().toString())){
                    Toast.makeText(this, "请输入职位", Toast.LENGTH_LONG).show();
                    return true;
                } else if (!config.getUserInfo().getJobTitle().equals(usertitle.getText().toString())) {
                    config.getUserInfo().setJobTitle(usertitle.getText().toString());
                    needUpdate = true;
                }

                if (needUpdate || newImageFile != null){
                    if (needUpdate) {
                        ServerAPI.UserData data = ServerAPI.getInstance().update(config.getUserInfo());
                        if (data.getCode() != ServerError.OK){
                            Toast.makeText(this, "更新用户信息失败", Toast.LENGTH_LONG).show();
                            Log.e(TAG, "code is " + data.getCode());
                            return true;
                        } else {
                            config.setUserInfo(data.getUser());
                            config.saveUserInfoToLocal(data.getUser());
                        }
                    }

                    if (newImageFile != null){
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        Bitmap bmp = null;
                        try {
                            bmp = BitmapFactory.decodeStream(getContentResolver().openInputStream(newImageFile));
                            bmp = Bitmap.createScaledBitmap(bmp, 200, 200, false);
                            bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);

                            byte[] byteArray = stream.toByteArray();
                            ServerAPI.AvatarData avatarData = ServerAPI.getInstance().uploadAvatar(config.getUserInfo().get_id(), byteArray);
                            if (avatarData.getCode() != ServerError.OK){
                                Toast.makeText(this, "更新头像失败", Toast.LENGTH_LONG).show();
                                Log.e(TAG, "code is " + avatarData.getCode());
                                return true;
                            } else {
                                config.getUserInfo().setAvatarUrl(avatarData.getUrl());
                                config.saveUserInfoToLocal(config.getUserInfo());
                            }

                        } catch (FileNotFoundException e) {
                            Log.e(TAG, Log.getStackTraceString(e));
                        }
                    }
                }
                setResult(RESULT_OK);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
