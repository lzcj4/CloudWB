package com.ifingers.yunwb;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ifingers.yunwb.customizeview.DatePickerFragment;
import com.ifingers.yunwb.customizeview.TimePickerFragment;
import com.ifingers.yunwb.dao.ConferenceDao;
import com.ifingers.yunwb.services.WXService;
import com.ifingers.yunwb.utility.ActivityCode;
import com.ifingers.yunwb.utility.Constants;
import com.ifingers.yunwb.utility.GuiHelper;
import com.ifingers.yunwb.utility.ServerAPI;
import com.ifingers.yunwb.utility.ServerError;
import com.ifingers.yunwb.utility.TBConfManager;
import com.ifingers.yunwb.utility.WbMessager;
import com.ifingers.yunwb.utility.WhiteboardTaskContext;
import com.tb.conf.api.struct.CTBUserEx;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import tb.confui.module.ITBConfKitListener;
import tb.confui.module.TBConfKit;

public class MeetingWizardActivity extends AppCompatActivity implements ITBConfKitListener {
    private String TAG = "MeetingWizardActivity";
    private String meetingUrl = null;
    private ConferenceDao conferenceDao;
    private boolean gotoInvite = false;
    private WhiteboardTaskContext globalConfig = WhiteboardTaskContext.getInstance();
    private String deviceName;
    private Gson gson = new GsonBuilder().create();

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.wizard, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        String title = getResources().getString(R.string.prompt_title_info);
        String content = "确认终止创建会议?";
        String no = getResources().getString(R.string.prompt_no);
        String yes = getResources().getString(R.string.prompt_yes);
        WbMessager.show(this, title, content, no, yes,
                //no click
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                },
                //yes click
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        setResult(ActivityCode.RESULT_CANCEL_CONFERENCE);
                        finish();
                    }
                }
        );
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                setResult(ActivityCode.RESULT_CANCEL_CONFERENCE);
                finish();
                return true;
            case R.id.action_save:
                conferenceDao = new ConferenceDao();
                //for quick testing....
                boolean success = fillConferenceInfo(conferenceDao);
                if (!success)
                    return true;

                //start launch meeting by create tech bridge meeting first. next steps are called in tech bridge's callback
                createTechBridgeMeeting(conferenceDao);

                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meeting_wizard);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        GuiHelper.setActionBarTitle(this, "会议设置");

        deviceName = getIntent().getStringExtra(Constants.DeviceNameKey);

        final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        final AutoCompleteTextView meeting_time = (AutoCompleteTextView)findViewById(R.id.meeting_time);
        final AutoCompleteTextView meeting_date = (AutoCompleteTextView)findViewById(R.id.meeting_date);
        meeting_time.setInputType(InputType.TYPE_NULL);
        meeting_date.setInputType(InputType.TYPE_NULL);
        meeting_time.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    TimePickerFragment newFragment = new TimePickerFragment();
                    newFragment.setOnTimeSetListener(new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                            meeting_time.setText(String.format("%02d:%02d", hourOfDay, minute));
                        }
                    });
                    imm.hideSoftInputFromWindow(meeting_time.getWindowToken(), 0);
                    newFragment.show(getSupportFragmentManager(), "timePicker");
                }
                return false;
            }
        });

        meeting_date.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    DatePickerFragment newFragment = new DatePickerFragment();
                    newFragment.setOnDateSetListener(new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                            meeting_date.setText(String.format("%d-%02d-%02d", year, monthOfYear + 1, dayOfMonth));
                        }
                    });
                    imm.hideSoftInputFromWindow(meeting_date.getWindowToken(), 0);
                    newFragment.show(getSupportFragmentManager(), "datePicker");
                }
                return false;
            }
        });

        if (globalConfig.DEBUG) {
            fillDebugConferenceOnGui();
        } else {
            // date
            AutoCompleteTextView item = (AutoCompleteTextView)MeetingWizardActivity.this.findViewById(R.id.meeting_date);
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            String datetime[] = format.format(new Date()).split(" ");
            item.setText(datetime[0]);

            // time
            item = (AutoCompleteTextView)MeetingWizardActivity.this.findViewById(R.id.meeting_time);
            item.setText(datetime[1]);
        }

        AutoCompleteTextView password = (AutoCompleteTextView)findViewById(R.id.meeting_password);
        password.setTypeface(Typeface.DEFAULT);
        password.setTransformationMethod(new PasswordTransformationMethod());

        AutoCompleteTextView meeting_name = (AutoCompleteTextView)findViewById(R.id.meeting_name);
        meeting_name.requestFocus();
    }

    private boolean fillConferenceInfo(ConferenceDao dao) {
        AutoCompleteTextView item = (AutoCompleteTextView)MeetingWizardActivity.this.findViewById(R.id.meeting_name);
        // verify name
        String name = item.getText().toString();
        if (name == null || "".equals(name)){
            item.setError(getString(R.string.error_field_required));
            item.requestFocus();
            return false;
        }
        dao.setName(name);

        // password
        item = (AutoCompleteTextView)MeetingWizardActivity.this.findViewById(R.id.meeting_password);
        String password = item.getText().toString();
        if (password == null || "".equals(password)){
            item.setError(getString(R.string.error_field_required));
            item.requestFocus();
            return false;
        }
        dao.setPassword(password);

        // date
        item = (AutoCompleteTextView)MeetingWizardActivity.this.findViewById(R.id.meeting_date);
        String date = item.getText().toString();
        if (date == null || "".equals(date)){
            item.setError(getString(R.string.error_field_required));
            item.requestFocus();
            return false;
        }

        // time
        item = (AutoCompleteTextView)MeetingWizardActivity.this.findViewById(R.id.meeting_time);
        String time = item.getText().toString();
        if (time == null || "".equals(date)){
            item.setError(getString(R.string.error_field_required));
            item.requestFocus();
            return false;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Calendar calendar = Calendar.getInstance();
        try {
            calendar.setTime(format.parse(date + " " + time));
        } catch (ParseException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        dao.setDate(calendar);

        // location
        item = (AutoCompleteTextView)MeetingWizardActivity.this.findViewById(R.id.meeting_location);
        dao.setLocation(item.getText().toString());

        // company
        item = (AutoCompleteTextView)MeetingWizardActivity.this.findViewById(R.id.meeting_company);
        dao.setCompany(item.getText().toString());

        // abstract
        item = (AutoCompleteTextView)MeetingWizardActivity.this.findViewById(R.id.meeting_abstract);
        dao.setConferenceAbstract(item.getText().toString());

        //hoster
        ArrayList<String> users = new ArrayList<>();
        ArrayList<String> nicknames = new ArrayList<>();
        users.add(globalConfig.getUserId());
        nicknames.add(globalConfig.getUserInfo().getName());
        dao.setUsers(users, nicknames);

        return true;
    }

    private final long SDEMP_ERRORCODE_BASE_CODE = 0x300000000L;
    private final String NODE_MEETINGPWD = "meetingPassword";
    private final String NODE_DISPLAYNAME = "displayname";
    private final String NODE_USERNAME = "username";
    private final String NODE_UILAYOUT = "uilayout";
    private final String NODE_MEETINID = "meetingId";

    private void createTechBridgeMeeting(ConferenceDao conf) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        String initJson = TBConfManager.toJsonForConfCmdLineInit(globalConfig.getTechBridgeSite());
        String createJson = TBConfManager.toJsonForCreateConfCmdLine(conf.getPassword(), globalConfig.getUserInfo().getName(),
                globalConfig.getUserId(), "1", null, conf.getName(), true);

        TBConfManager.createConf(getApplicationContext(), this, displayMetrics, globalConfig.getTechBridgeAppKey(), createJson, initJson);
    }

    private void promptAddMembers(){
        WbMessager.show(MeetingWizardActivity.this, getResources().getString(R.string.prompt_title_info), getResources().getString(R.string.prompt_members),
                getResources().getString(R.string.prompt_no), getResources().getString(R.string.prompt_yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //cancel add member
                        dialog.dismiss();
                        //go to white board activity
                        launchWhiteboardActivity();
                    }
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //go to weixin
                        dialog.dismiss();
                        gotoInvite = true;
                        String title = String.format(conferenceDao.getCompany() + "发起会议:" + conferenceDao.getName());
                        String desc = String.format("会议密码:" + conferenceDao.getPassword() + "\r\n会议地址:" + conferenceDao.getLocation() + "\n会议概要:" + conferenceDao.getConferenceAbstract());
                        String url = ServerAPI.getInstance().getInviteUrl(conferenceDao.getConferenceId(), conferenceDao.getPassword(), conferenceDao.getName());
                        WXService.getInstance().shareWeb(url, title, desc);
                    }
                }
        );
    }

    @Override
    protected void onResume() {
        if (gotoInvite) {
            gotoInvite = false;//reset immediately
            launchWhiteboardActivity();
        }
        super.onResume();
    }

    private void launchWhiteboardActivity() {
        if (conferenceDao != null) {
            Intent it = new Intent(MeetingWizardActivity.this, WhiteBoardActivity.class);
            it.putExtra(Constants.UrlKey, meetingUrl);
            it.putExtra(Constants.NameKey, conferenceDao.getName());
            it.putExtra(Constants.ConferenceIdKey, conferenceDao.getConferenceId());
            it.putExtra(Constants.PasswordKey, conferenceDao.getPassword());
            it.putExtra(Constants.HostKey, true);
            it.putExtra(Constants.DeviceNameKey, deviceName);
            it.putExtra(Constants.TechBridgeIdKey, conferenceDao.getTechBridgeId());
            startActivity(it);
            MeetingWizardActivity.this.finish();
        }
    }

    private void fillDebugConference(ConferenceDao dao) {
        dao.setName("DEBUG");
        dao.setCompany("ROLLS ROYCE");
        dao.setConferenceAbstract("Testing");
        Date now = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        dao.setDate(calendar);
        dao.setLocation("636");
        ArrayList<String> users = new ArrayList<>();
        ArrayList<String> nicknames = new ArrayList<>();

        users.add(globalConfig.getUserId());//session id is user id
        nicknames.add(globalConfig.getUserInfo().getName());
        dao.setUsers(users, nicknames);
        dao.setPassword("123");
    }

    private void fillDebugConferenceOnGui() {
        //name
        AutoCompleteTextView item = (AutoCompleteTextView)MeetingWizardActivity.this.findViewById(R.id.meeting_name);
        item.setText("DEBUG");

        // password
        item = (AutoCompleteTextView)MeetingWizardActivity.this.findViewById(R.id.meeting_password);
        item.setText("123");

        // date
        item = (AutoCompleteTextView)MeetingWizardActivity.this.findViewById(R.id.meeting_date);
        item.setText("2016-04-1");

        // time
        item = (AutoCompleteTextView)MeetingWizardActivity.this.findViewById(R.id.meeting_time);
        item.setText("18:18");

        // location
        item = (AutoCompleteTextView)MeetingWizardActivity.this.findViewById(R.id.meeting_location);
        item.setText("636");

        // company
        item = (AutoCompleteTextView)MeetingWizardActivity.this.findViewById(R.id.meeting_company);
        item.setText("ROLLS ROYCE");

        // abstract
        item = (AutoCompleteTextView) MeetingWizardActivity.this.findViewById(R.id.meeting_abstract);
        item.setText("Testing");
    }

    @Override
    public boolean TbConfNotification_On3rdCreateUI(int i, View view) {
        return true;
    }

    @Override
    public boolean TbConfNotification_OnMessageRegisted() {
        return true;
    }

    @Override
    public boolean TbConfNotification_OnMeetingCreated(long errorcode, String s, String s1) {
        //success, here use 0 as success, OnMeetingJoined use SDEMP_ERRORCODE_BASE_CODE, very weird design
        if (errorcode == 0) {
            HashMap map = gson.fromJson(s1, HashMap.class);
            String techBridgeMeetingId = (String) map.get(NODE_MEETINID);
            conferenceDao.setTechBridgeId(techBridgeMeetingId);
        } else {
            conferenceDao.setTechBridgeId(null);
            Toast.makeText(this, "语音功能未成功发起，请更换设备或继续进行会议\r\n错误码:" + errorcode + " " + s, Toast.LENGTH_LONG).show();
        }

        //ask server to create meeting
        ServerAPI.ConferenceData data = ServerAPI.getInstance().launchConference(conferenceDao);
        if (data.getCode() == ServerError.OK) {
            conferenceDao = new ConferenceDao();
            conferenceDao.fill(data.getConference());
            meetingUrl = data.getMeetingUrl();
            Log.i(TAG, "new meeting id = " + conferenceDao.getConferenceId());
            promptAddMembers();
        } else if (data.getCode() == ServerError.MEETING_ALREADY_EXIST) {
            //close tech bridge meeting
            if (conferenceDao.getTechBridgeId() != null) {
                TBConfManager.leaveConf(true);
            }
            Toast.makeText(MeetingWizardActivity.this, "您创建的会议名称已存在，请更换会议名", Toast.LENGTH_SHORT).show();
        }
        else {
            //close tech bridge meeting
            if (conferenceDao.getTechBridgeId() != null) {
                TBConfManager.leaveConf(true);
            }
            Toast.makeText(MeetingWizardActivity.this, "创建会议失败", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    @Override
    public boolean TbConfNotification_OnMeetingJoined(long l, String s, boolean b) {
        return true;
    }

    @Override
    public boolean TbConfNotification_OnMeetingLeft(long l, String s, boolean b) {
        return true;
    }

    @Override
    public boolean TbConfNotification_OnUserJoin(CTBUserEx ctbUserEx) {
        return true;
    }

    @Override
    public boolean TbConfNotification_OnUserUpdate(CTBUserEx ctbUserEx) {
        return true;
    }

    @Override
    public boolean TbConfNotification_OnUserLeft(CTBUserEx ctbUserEx) {
        return true;
    }

    @Override
    public boolean TbConfNotification_On3rdUIResponse(int i, String s) {
        return true;
    }

    @Override
    public boolean TbConfNotification_On3rdDestroyUI(int i) {
        return true;
    }

    @Override
    public boolean TbConfNotification_OnConfUITerminate(long l, String s) {
        return true;
    }
}
