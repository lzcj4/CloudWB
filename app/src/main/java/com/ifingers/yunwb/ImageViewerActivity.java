package com.ifingers.yunwb;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.ifingers.yunwb.services.WXService;
import com.ifingers.yunwb.utility.GuiHelper;

import java.io.File;

public class ImageViewerActivity extends AppCompatActivity {

    private final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 100;
    private PopupWindow popupWindow;
    private boolean isAlive;
    private boolean isPrivateFolder;

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        switch (requestCode) {
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE:
                String paths[] = getIntent().getStringExtra("file").split("/");
                String url = MediaStore.Images.Media.insertImage(getContentResolver(), BitmapFactory.decodeFile(getIntent().getStringExtra("file")), paths[paths.length - 1], "");
                Uri uri = Uri.parse(url);
                String[] proj = { MediaStore.Images.Media.DATA };
                Cursor actualimagecursor = ImageViewerActivity.this.getContentResolver().query(uri, proj, null, null, null);
                int actual_image_column_index = actualimagecursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                actualimagecursor.moveToFirst();
                String img_path = actualimagecursor.getString(actual_image_column_index);
                Toast.makeText(ImageViewerActivity.this, "文件已保存至" + img_path, Toast.LENGTH_LONG).show();
                return;
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);
        GuiHelper.setActionBarTitle(this, "");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        isAlive = getIntent().getBooleanExtra("alive", false);
        isPrivateFolder = getIntent().getBooleanExtra("private", false);
        final View popupView = getLayoutInflater().inflate(R.layout.save_thumbnail, null);
        final Animation movein = AnimationUtils.loadAnimation(this, R.anim.popup_movein);
        ImageView view = (ImageView)findViewById(R.id.image);
        view.setImageBitmap(BitmapFactory.decodeFile(getIntent().getStringExtra("file")));
        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                popupView.startAnimation(movein);
                popupWindow.showAtLocation(v, Gravity.BOTTOM, 0, 0);
                return true;
            }
        });

        popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setTouchable(true);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(00000));
        View contentView = popupWindow.getContentView();
        Button save = (Button)contentView.findViewById(R.id.action_save);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(ImageViewerActivity.this,Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(ImageViewerActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                } else {
                    String paths[] = getIntent().getStringExtra("file").split("/");
                    String url = MediaStore.Images.Media.insertImage(getContentResolver(), BitmapFactory.decodeFile(getIntent().getStringExtra("file")), paths[paths.length - 1], "");
                    Uri uri = Uri.parse(url);
                    String[] proj = { MediaStore.Images.Media.DATA };
                    Cursor actualimagecursor = ImageViewerActivity.this.getContentResolver().query(uri,  proj, null, null, null);
                    int actual_image_column_index = actualimagecursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    actualimagecursor.moveToFirst();
                    String img_path = actualimagecursor.getString(actual_image_column_index);
                    Toast.makeText(ImageViewerActivity.this, "文件已保存至" + img_path, Toast.LENGTH_LONG).show();
                }

                popupWindow.dismiss();
            }
        });
        Button cancel = (Button)contentView.findViewById(R.id.action_cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow.dismiss();
            }
        });

        Button delete = (Button)contentView.findViewById(R.id.action_delete);
        if (isAlive && !isPrivateFolder)
            delete.setVisibility(View.GONE);
        else
            delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    File file = new File(getIntent().getStringExtra("file"));
                    boolean result = file.delete();
                    if(result) {
                        Toast.makeText(ImageViewerActivity.this, "文件已删除", Toast.LENGTH_SHORT).show();
                        popupWindow.dismiss();
                        finish();
                        return;
                    }
                    else
                        Toast.makeText(ImageViewerActivity.this, "文件删除失败", Toast.LENGTH_SHORT).show();
                    popupWindow.dismiss();
                }
            });

        Button share = (Button) contentView.findViewById(R.id.action_share);
        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap image = BitmapFactory.decodeFile(getIntent().getStringExtra("file"));
                WXService.getInstance().shareImage(image);
                Toast.makeText(ImageViewerActivity.this, "文件已分享", Toast.LENGTH_SHORT).show();
                popupWindow.dismiss();
            }
        });
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
