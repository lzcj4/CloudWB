package com.ifingers.yunwb;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ifingers.yunwb.dao.ConferenceDao;
import com.ifingers.yunwb.utility.GuiHelper;
import com.ifingers.yunwb.utility.LocalConferenceRecords;
import com.ifingers.yunwb.utility.ServerAPI;
import com.ifingers.yunwb.utility.ServerError;
import com.ifingers.yunwb.utility.WbMessager;
import com.ifingers.yunwb.utility.WhiteboardTaskContext;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;

public class ConferenceRecord extends AppCompatActivity {

    private String TAG = "ConferenceRecord";
    private ArrayList<String> selectedFolders = new ArrayList<>();

    private MenuItem deleteMenuItem;
    private MenuItem cancelMenuItem;
    private MenuItem saveMenuItem;
    private YearAdapter yearAdapter;
    private boolean deleteClicked = false;
    private ArrayList<ConferenceDao> aliveConfList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conference_record);
        GuiHelper.setActionBarTitle(this, "历史会议");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        RetrieveConferenceRecordList task = new RetrieveConferenceRecordList();
        task.execute();
    }

    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (deleteClicked){
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
                selectedFolders.clear();
                deleteClicked = true;
                yearAdapter.notifyDataSetChanged();
                invalidateOptionsMenu();
                return true;
            case R.id.action_cancel:
                deleteClicked = false;
                yearAdapter.notifyDataSetChanged();
                invalidateOptionsMenu();
                return true;
            case R.id.action_save:
                if (selectedFolders.size() == 0){
                    deleteClicked = false;
                    invalidateOptionsMenu();
                    return true;
                }
                WbMessager.show(ConferenceRecord.this, "警告", "确定删除所选择文件吗?", "取消", "确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        aliveConfList.clear();
                        ServerAPI.ConferenceListData serverData = ServerAPI.getInstance().requestConferenceList();
                        final int code = serverData.getCode();
                        String userId = WhiteboardTaskContext.getInstance().getUserId();
                        if (code == ServerError.OK) {
                            ArrayList<HashMap<String, Object>> conferences = serverData.getConferences();

                            if (conferences != null) {
                                for (HashMap<String, Object> map : conferences) {
                                    ConferenceDao dao = new ConferenceDao();
                                    dao.fill(map);
                                    if (!dao.getHostId().equals(userId))//don't display conference which is created by current user
                                        aliveConfList.add(dao);
                                }
                            }
                        }

                        for (String fileName : selectedFolders) {
                            try {
                                boolean isAlive = false;
                                if(aliveConfList.size() > 0){
                                    for(ConferenceDao dao : aliveConfList){
                                        if (fileName.contains(dao.getConferenceId())){
                                            isAlive = true;
                                            break;
                                        }
                                    }
                                }
                                if (isAlive) {
                                    Toast.makeText(ConferenceRecord.this, "无法删除正在进行的会议", Toast.LENGTH_SHORT).show();
                                    continue;
                                }
                                String pathStr[] = fileName.split("_");
                                String folder = LocalConferenceRecords.getRootPath(ConferenceRecord.this) + pathStr[0] + "/" + pathStr[1] + "_" + pathStr[2];
                                FileUtils.deleteDirectory(new File(folder));
                            } catch (IOException e) {
                                Log.e(TAG, Log.getStackTraceString(e));
                            }
                        }

                        deleteClicked = false;
                        RetrieveConferenceRecordList task = new RetrieveConferenceRecordList();
                        task.execute();
                        invalidateOptionsMenu();
                    }
                });
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void onGridItemSelected(String date, String id, String name) {
        if (deleteClicked)
            return;

        Intent it = new Intent(ConferenceRecord.this, ConferenceRecordFolderActivity.class);
        it.putExtra("date", date);
        it.putExtra("id", id);
        it.putExtra("name", name);
        it.putExtra("alive", false);
        startActivity(it);
    }

    class RetrieveConferenceRecordList extends AsyncTask {

        @Override
        protected Object doInBackground(Object[] params) {
            ArrayList<Integer> years = new ArrayList<>();

            try {
                years = LocalConferenceRecords.retrieveConferenceYears(ConferenceRecord.this);
            } catch (ParseException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
            return years;
        }

        @Override
        protected void onPostExecute(Object obj) {
            ListView listView = (ListView) findViewById(R.id.conference_list);
            yearAdapter = new YearAdapter((ArrayList<Integer>) obj, ConferenceRecord.this);
            listView.setAdapter(yearAdapter);
        }
    }

    class ViewHolder {
        TextView txtName;
        GridView grdView;
    }

    class GridViewHolder {
        TextView txtName;
        ImageView imageView;
        CheckBox selector;
        String conferenceId;
        String conferenceDate;
        String name;
    }

    class YearAdapter extends BaseAdapter {

        private ArrayList<Integer> years;
        private Context context;

        public YearAdapter(ArrayList<Integer> years, Context context) {
            this.years = years;
            this.context = context;
        }

        @Override
        public int getCount() {
            return years.size();
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
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder row;
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.conference_list_view, parent, false);

                row = new ViewHolder();
                row.txtName = (TextView) convertView.findViewById(R.id.txt_conf_date);
                row.grdView = (GridView) convertView.findViewById(R.id.grid_conf_detail);
                row.grdView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                        GridViewHolder holder = (GridViewHolder) view.getTag();
                        ConferenceRecord.this.onGridItemSelected(holder.conferenceDate, holder.conferenceId, holder.name);
                    }
                });
                convertView.setTag(row);
            } else {
                row = (ViewHolder) convertView.getTag();
            }

            row.txtName.setText(years.get(position).toString() + "年");
            row.grdView.setAdapter(new GridAdapter(LocalConferenceRecords.retrieveRecords(years.get(position).toString(), ConferenceRecord.this),
                    ConferenceRecord.this));
            return convertView;
        }
    }

    class GridAdapter extends BaseAdapter {

        private ArrayList<String> names;
        private Context context;

        public GridAdapter(ArrayList<String> names, Context context) {
            this.context = context;
            this.names = names;
        }

        @Override
        public int getCount() {
            return names == null? 0 : names.size();
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

            GridViewHolder item;
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.conference_list_item, parent, false);

                item = new GridViewHolder();
                item.imageView = (ImageView) convertView.findViewById(R.id.img_confitem);
                item.txtName = (TextView) convertView.findViewById(R.id.txt_confitem_name);
                item.selector = (CheckBox) convertView.findViewById(R.id.item_selector);
                convertView.setTag(item);
            } else {
                item = (GridViewHolder) convertView.getTag();
            }

            item.selector.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked){
                        if (!selectedFolders.contains(names.get(position))){
                            selectedFolders.add(names.get(position));
                        }
                    } else {
                        if (selectedFolders.contains(names.get(position))){
                            selectedFolders.remove(names.get(position));
                        }
                    }
                }
            });

            String values[] = names.get(position).split("_");
            String name = values[0].substring(5);
            String text = name;
            if (values.length > 2)
                text = name + " " + (values[2] == null ? "" : values[2]);
            item.txtName.setText(text);
            item.conferenceDate = values[0];
            item.conferenceId = values[1];
            item.name = values[2] == null ? "" : values[2];

            if (deleteClicked){
                item.selector.setVisibility(View.VISIBLE);
            } else {
                item.selector.setVisibility(View.INVISIBLE);
            }
            return convertView;
        }
    }
}
