package com.ifingers.yunwb;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ifingers.yunwb.utility.GuiHelper;

public class ConferenceRecordFolderActivity extends AppCompatActivity {

    private String date;
    private String id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conference_record_folder);
        GuiHelper.setActionBarTitle(this,  getIntent().getStringExtra("name"));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        LinearLayout publicView = (LinearLayout)findViewById(R.id.folder_public);
        LinearLayout privateView = (LinearLayout)findViewById(R.id.folder_private);
        date = getIntent().getStringExtra("date");
        id = getIntent().getStringExtra("id");

        publicView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent it = new Intent(ConferenceRecordFolderActivity.this, RecordThumbnailActivity.class);
                it.putExtra("date", date);
                it.putExtra("id", id);
                it.putExtra("folder", "public");
                it.putExtra("name", getIntent().getStringExtra("name"));
                it.putExtra("alive", getIntent().getBooleanExtra("alive", false));
                startActivity(it);
            }
        });

        privateView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent it = new Intent(ConferenceRecordFolderActivity.this, RecordThumbnailActivity.class);
                it.putExtra("date", date);
                it.putExtra("id", id);
                it.putExtra("name", getIntent().getStringExtra("name"));
                it.putExtra("folder", "private");
                startActivity(it);
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
