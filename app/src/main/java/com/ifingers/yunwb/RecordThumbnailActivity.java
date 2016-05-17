package com.ifingers.yunwb;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.ImageView;

import com.ifingers.yunwb.utility.GuiHelper;
import com.ifingers.yunwb.utility.LocalConferenceRecords;
import com.ifingers.yunwb.utility.WbMessager;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

public class RecordThumbnailActivity extends AppCompatActivity {

    private String date;
    private String id;
    private boolean isLoadPrivate;
    private String rootThumbnailPath;
    private String name;
    private boolean isAlive;

    private boolean deleteClicked = false;
    private MenuItem deleteMenuItem;
    private MenuItem cancelMenuItem;
    private MenuItem saveMenuItem;
    private ThumbnailAdapter adapter;

    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
        super.onPrepareOptionsMenu(menu);

        if (!isLoadPrivate && isAlive){
            deleteMenuItem.setVisible(false);
            cancelMenuItem.setVisible(false);
            saveMenuItem.setVisible(false);
        } else if (deleteClicked){
            deleteMenuItem.setVisible(false);
            cancelMenuItem.setVisible(true);
            saveMenuItem.setVisible(true);
        } else {
            deleteMenuItem.setVisible(true);
            cancelMenuItem.setVisible(false);
            saveMenuItem.setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit, menu);

        deleteMenuItem = menu.findItem(R.id.action_edit);
        cancelMenuItem = menu.findItem(R.id.action_cancel);
        saveMenuItem = menu.findItem(R.id.action_save);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_edit:
                deleteClicked = true;
                adapter.notifyDataSetChanged();
                invalidateOptionsMenu();
                return true;
            case R.id.action_cancel:
                deleteClicked = false;
                adapter.notifyDataSetChanged();
                invalidateOptionsMenu();
                return true;
            case R.id.action_save:
                if (adapter.getSelectedFiles().size() == 0) {
                    deleteClicked = false;
                    invalidateOptionsMenu();
                    return true;
                }
                WbMessager.show(RecordThumbnailActivity.this, "警告", "确定删除所选择文件吗?", "取消", "确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        for (String fileName : adapter.getSelectedFiles()) {
                            File file = new File(rootThumbnailPath + "/" + fileName);
                            boolean result = file.delete();
                        }

                        deleteClicked = false;
                        adapter.updateData();
                        invalidateOptionsMenu();
                    }
                });

                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume(){
        super.onResume();
        LoadThumbnailsTask task = new LoadThumbnailsTask();
        task.execute();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_thumbnail);
        GuiHelper.setActionBarTitle(this, getIntent().getStringExtra("name"));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        date = getIntent().getStringExtra("date");
        id = getIntent().getStringExtra("id");
        isLoadPrivate = "private".equals(getIntent().getStringExtra("folder"));
        name = getIntent().getStringExtra("name");
        isAlive = getIntent().getBooleanExtra("alive", true);

        LoadThumbnailsTask task = new LoadThumbnailsTask();
        task.execute();
    }

    class ThumbnailAdapter extends BaseAdapter{

        private String files[];
        private ArrayList<String> selectedFiles = new ArrayList<>();

        public ArrayList<String> getSelectedFiles(){
            return selectedFiles;
        }

        public void updateData(){
            int length = files.length - 1;
            ArrayList<String> remainedFiles = new ArrayList<>();
            for(int i = length; i >= 0; i--){
                if (!selectedFiles.contains(files[i])){
                    remainedFiles.add(files[i]);
                }
            }

            selectedFiles.clear();
            files = new String[remainedFiles.size()];
            files = remainedFiles.toArray(files);
            notifyDataSetChanged();
        }

        ThumbnailAdapter(String[] files){
            this.files = files;
        }

        @Override
        public int getCount() {
            return files == null? 0 : files.length;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ImageView view;
            DisplayMetrics displaymetrics = new DisplayMetrics();
            RecordThumbnailActivity.this.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
            int size = displaymetrics.widthPixels / 4;
            if (convertView == null) {
                convertView = LayoutInflater.from(RecordThumbnailActivity.this).inflate(R.layout.thunbnail_item, parent, false);
                convertView.setLayoutParams(new GridView.LayoutParams(size, size));
                view = (ImageView)convertView.findViewById(R.id.thumbnail_image);
                convertView.setTag(view);
            } else {
                view = (ImageView) convertView.getTag();
            }

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(RecordThumbnailActivity.this.rootThumbnailPath + "/" + files[position], options),
                    size,size);
            view.setImageBitmap(bitmap);

            CheckBox chk = (CheckBox)convertView.findViewById(R.id.thumbnail_select);
            chk.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked){
                        if (!selectedFiles.contains(files[position])){
                            selectedFiles.add(files[position]);
                        }
                    } else {
                        if (selectedFiles.contains(files[position])){
                            selectedFiles.remove(files[position]);
                        }
                    }

                }
            });

            if (deleteClicked)
                chk.setVisibility(View.VISIBLE);
            else
                chk.setVisibility(View.INVISIBLE);

            return convertView;
        }
    }

    class LoadThumbnailsTask extends AsyncTask{

        @Override
        protected Object doInBackground(Object[] params) {
            String rootStr = LocalConferenceRecords.getRootPath(RecordThumbnailActivity.this);
            File root = new File(rootStr + date);
            String folders[] = root.list(new FilenameFilter() {
                @Override
                public boolean accept(File current, String name) {
                    return new File(current, name).isDirectory() && name.contains(id) && name.contains(RecordThumbnailActivity.this.name);
                }
            });

            if (folders.length > 0){
                String folder = folders[0];

                if (isLoadPrivate)
                    RecordThumbnailActivity.this.rootThumbnailPath = rootStr + date + "/" + folder + "/private";
                else
                    RecordThumbnailActivity.this.rootThumbnailPath = rootStr + date + "/" + folder + "/public";


                File file = new File(RecordThumbnailActivity.this.rootThumbnailPath);
                return file.list();
            }

            return null;
        }

        @Override
        protected void onPostExecute(final Object obj) {
            final String files[] = (String[])obj;
            adapter = new ThumbnailAdapter(files);
            GridView view = (GridView)RecordThumbnailActivity.this.findViewById(R.id.record_list);
            view.setAdapter(adapter);
            view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Intent it = new Intent(RecordThumbnailActivity.this, ImageViewerActivity.class);
                    it.putExtra("file", RecordThumbnailActivity.this.rootThumbnailPath + "/" + files[position]);
                    it.putExtra("alive", isAlive);
                    it.putExtra("private", isLoadPrivate);
                    startActivity(it);
                }
            });
        }
    }
}
