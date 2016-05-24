package com.ifingers.yunwb;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.ifingers.yunwb.bluetooth.BLCommService;
import com.ifingers.yunwb.dao.ConferenceDao;
import com.ifingers.yunwb.dao.UserDao;
import com.ifingers.yunwb.services.IWBDevice;
import com.ifingers.yunwb.services.WXService;
import com.ifingers.yunwb.utility.ActivityCode;
import com.ifingers.yunwb.utility.Constants;
import com.ifingers.yunwb.utility.CrashHandler;
import com.ifingers.yunwb.utility.DataLog;
import com.ifingers.yunwb.utility.GuiHelper;
import com.ifingers.yunwb.utility.ServerAPI;
import com.ifingers.yunwb.utility.ServerError;
import com.ifingers.yunwb.utility.TBConfManager;
import com.ifingers.yunwb.utility.WakeLock;
import com.ifingers.yunwb.utility.WhiteboardTaskContext;
import com.tb.conf.api.struct.CTBUserEx;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import tb.confui.module.ITBConfKitListener;

public class ActionListActivity extends AppCompatActivity implements IWBDevice.WBDeviceStatusHandler, ITBConfKitListener {
    private String TAG = "ActionListActivity";
    private ActionListAdapter listAdapter;
    private PopupWindow popupWindow;
    private WhiteboardTaskContext globalConfig = WhiteboardTaskContext.getInstance();
    private IWBDevice device = globalConfig.getWbDevice();
    private Uri inviteUri = null;
    private boolean pullingList = false;
    private String deviceName = null;
    private WXService service = WXService.getInstance();

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.refresh, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                pullList();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mWakeLock.unlockAll();
        TBConfManager.dispose();
        DataLog.getInstance().close();
    }

    WakeLock mWakeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            // Activity was brought to front and not created,
            // Thus finishing this will get us to the last viewed activity
            Log.i(TAG, "bring to front");
            finish();
            return;
        }
        setContentView(R.layout.activity_action_list);

        globalConfig.init(this);
        service.init(this);
        CrashHandler.getInstance().register(this);

        GuiHelper.setActionBarTitle(this, "会议列表");
        pullList();

        View createButton = findViewById(R.id.action_launch_conf);
        createButton.setOnClickListener((view) -> startQRCodeScan());

        View recordButton = findViewById(R.id.action_record);
        recordButton.setOnClickListener((view) -> {
            Intent it = new Intent(ActionListActivity.this, ConferenceRecord.class);
            startActivity(it);
        });

        View create = findViewById(R.id.txt_create);
        create.setOnClickListener((view) -> startQRCodeScan());

        View more = findViewById(R.id.action_more);
        more.setOnClickListener((view) -> {
            Intent it = new Intent(ActionListActivity.this, HelperMainActivity.class);
            startActivity(it);
        });
        View popupView = getLayoutInflater().inflate(R.layout.action_password_input, null);
        popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setTouchable(true);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(00000));

        dispatch(getIntent());
        mWakeLock = new WakeLock(this);
        mWakeLock.lockAll();
    }

    private void startQRCodeScan() {
        IntentIntegrator integrator = new IntentIntegrator(ActionListActivity.this);
        integrator.setCaptureActivity(QRScannerActivity.class);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
        integrator.setOrientationLocked(false);
        integrator.initiateScan();
        // CrashReport.testJavaCrash();
//                deviceName = "test";
//                device.init(ActionListActivity.this, ActionListActivity.this);
//                device.connect(deviceName);
    }

    private void dispatch(Intent it) {
        inviteUri = it.getData();
        boolean isLogin = false;

        //check login or not
        if (globalConfig.getUserInfo() == null) {
            //try load user info
            UserDao userInfo = globalConfig.loadUserInfoFromLocal();
            if (userInfo == null) {
                //never login before, go to weixin login
                Intent newIntent = new Intent(this, WXLoginActivity.class);
                startActivityForResult(newIntent, ActivityCode.REQUEST_WEIXIN_LOGIN);
            } else {
                //find the user
                globalConfig.setUserInfo(userInfo);
                isLogin = true;
            }
        } else {
            isLogin = true;
        }

        if (isLogin) {
            if (inviteUri != null) {
                joinConference();
            } else {
                pullList();
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        dispatch(intent);
    }

    @Override
    public void onConnected() {
        //nothing to do ,allow user to fill conference info
        Log.i(TAG, "bt connected");
        Intent it = new Intent(ActionListActivity.this, MeetingWizardActivity.class);
        it.putExtra(Constants.DeviceNameKey, deviceName);
        startActivityForResult(it, ActivityCode.REQUEST_CREATE_CONFERENCE);
    }

    @Override
    public void onDisconnected() {
        Log.i(TAG, "bt disconnected");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ActionListActivity.this, getResources().getString(R.string.bluetooth_connect_failure), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDeviceNotFound() {
        Log.i(TAG, "bt device not found");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ActionListActivity.this, getResources().getString(R.string.bluetooth_device_not_found), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == IntentIntegrator.REQUEST_CODE) {
            IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if (scanningResult != null && scanningResult.getContents() != null) {
                //we have a result
                deviceName = scanningResult.getContents();
                //connect to device, if success, create meeting
                device.init(this, this);
                device.connect(deviceName);
            } else {
                Toast.makeText(getApplicationContext(),
                        R.string.qr_not_found, Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == BLCommService.REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                //// TODO: 2016/5/11  Why is camera not bluetooth?
                if (ContextCompat.checkSelfPermission(ActionListActivity.this,
                        Manifest.permission.BLUETOOTH)
                        == PackageManager.PERMISSION_GRANTED)

                    device.connect(deviceName);
                else
                    Toast.makeText(getApplicationContext(),
                            R.string.prompt_bluetooth, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(),
                        R.string.prompt_bluetooth, Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == ActivityCode.REQUEST_WEIXIN_LOGIN) {
            //intent from outside uri, want to join the conference
            if (resultCode == ActivityCode.RESULT_WEIXIN_LOGIN_OK && inviteUri != null) {
                joinConference();
            }
        } else if (requestCode == ActivityCode.REQUEST_CREATE_CONFERENCE) {
            if (resultCode == ActivityCode.RESULT_CANCEL_CONFERENCE) {
                device.disconnect();
                Toast.makeText(this, "会议未成功创建", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void joinConference() {
        List<String> segments = inviteUri.getPathSegments();
        inviteUri = null;//reset immediately indicates we consume the event
        if (segments.size() == 3) {
            String conferenceId = segments.get(0);
            String password = segments.get(1);
            String meetingName = segments.get(2);

            ServerAPI.ConferenceData cdata = ServerAPI.getInstance().requestConferenceInfo(conferenceId, password);
            if (cdata.getCode() == ServerError.OK) {
                ConferenceDao dao = new ConferenceDao();
                dao.fill(cdata.getConference());

                if (dao.getTechBridgeId() != null)
                    joinTechBridgeMeeting(dao);

                Intent intent = new Intent();
                intent.putExtra(Constants.ConferenceIdKey, conferenceId);
                intent.putExtra(Constants.PasswordKey, password);
                intent.putExtra(Constants.NameKey, meetingName);
                intent.putExtra(Constants.HostKey, false);
                intent.putExtra(Constants.TechBridgeIdKey, dao.getTechBridgeId());
                intent.setClass(ActionListActivity.this, WhiteBoardActivity.class);
                ActionListActivity.this.startActivity(intent);
            } else {
                Toast.makeText(this, "加入会议失败\r\n错误码:" + cdata.getCode(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private final String NODE_MEETINGPWD = "meetingPassword";
    private final String NODE_DISPLAYNAME = "displayname";
    private final String NODE_USERNAME = "username";
    private final String NODE_UILAYOUT = "uilayout";
    private final String NODE_MEETINID = "meetingId";
    private Gson gson = new GsonBuilder().create();

    private void joinTechBridgeMeeting(ConferenceDao conf) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        String initJson = TBConfManager.toJsonForConfCmdLineInit(globalConfig.getTechBridgeSite());
        String joinJson = TBConfManager.toJsonForJoinConfCmdLine(conf.getTechBridgeId(), conf.getPassword(), globalConfig.getUserInfo().getName(),
                globalConfig.getUserId(), "1", null, null, null);

        TBConfManager.joinConf(this.getApplicationContext(), this, displayMetrics, globalConfig.getTechBridgeAppKey(), joinJson, initJson);
    }

    @Override
    protected void onResume() {
        super.onResume();
        dispatch(new Intent());
    }

    private void pullList() {
        if (!pullingList) {
            pullingList = true;
            PullConferenceList task = new PullConferenceList();
            task.execute();
        }
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
    public boolean TbConfNotification_OnMeetingCreated(long l, String s, String s1) {
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

    class PullConferenceList extends AsyncTask {

        @Override
        protected Object doInBackground(Object[] params) {
            listAdapter = new ActionListAdapter(ActionListActivity.this);
            return listAdapter;
        }

        @Override
        protected void onPostExecute(Object obj) {
            View view = findViewById(R.id.view_create);
            ListView listView = (ListView) findViewById(R.id.conference_list);

            if (listAdapter.getCount() == 0) {
                view.setVisibility(View.VISIBLE);
                listView.setVisibility(View.GONE);
            } else {
                view.setVisibility(View.GONE);

                listView.setVisibility(View.VISIBLE);
                listView.setAdapter(listAdapter);
                listAdapter.notifyDataSetChanged();
            }
            pullingList = false;
        }
    }

    private class ActionListAdapter extends BaseAdapter {

        private String TAG = "ActionListAdapter";
        private LinkedList<ConferenceDao> data = new LinkedList<>();
        private Context context;

        public ActionListAdapter(Context context) {
            this.context = context;
            try {
                ServerAPI.ConferenceListData serverData = ServerAPI.getInstance().requestConferenceList();
                final int code = serverData.getCode();
                String userId = globalConfig.getUserId();
                if (code == ServerError.OK) {
                    ArrayList<HashMap<String, Object>> conferences = serverData.getConferences();

                    if (conferences != null) {
                        for (HashMap<String, Object> map : conferences) {
                            ConferenceDao dao = new ConferenceDao();
                            dao.fill(map);
                            if (!dao.getHostId().equals(userId))//don't display conference which is created by current user
                                data.add(dao);
                        }
                    }
                } else {
                    ActionListActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Resources res = ActionListActivity.this.getResources();
                            Toast.makeText(ActionListActivity.this, String.format(res.getString(R.string.network_slow_code), code), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }

        @Override
        public int getCount() {
            return data == null ? 0 : data.size();
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

            final ViewHolder row;
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.conference_list, parent, false);

                row = new ViewHolder();
                row.txtName = (TextView) convertView.findViewById(R.id.txt_conf_name);
                row.txtDate = (TextView) convertView.findViewById(R.id.txt_conf_date);
                row.btnEnter = (Button) convertView.findViewById(R.id.btn_enter);
                row.image = (ImageView) convertView.findViewById(R.id.conf_image);
                convertView.setTag(row);
            } else {
                row = (ViewHolder) convertView.getTag();
            }

            row.txtName.setText(data.get(position).getName());
            SimpleDateFormat format = new SimpleDateFormat("MM-dd HH:mm");

            row.txtDate.setText(format.format(data.get(position).getDate().getTime()));

            if ((position + 1) % 3 == 1)
                row.image.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.conferenceicon1));
            else if ((position + 1) % 3 == 2)
                row.image.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.conferenceicon2));
            else
                row.image.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.conferenceicon3));

            row.btnEnter.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    View contentView = popupWindow.getContentView();
                    contentView.findViewById(R.id.txt_conf_pw);
                    final AutoCompleteTextView text = (AutoCompleteTextView) contentView.findViewById(R.id.txt_conf_pw);
                    text.setText("");

                    TextView title = (TextView) contentView.findViewById(R.id.title);
                    title.setText(row.txtName.getText().toString());

                    popupWindow.showAtLocation(v, Gravity.CENTER, 0, 0);
                    Button btn_ok = (Button) contentView.findViewById(R.id.btn_ok);
                    btn_ok.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ConferenceDao conf = data.get(position);
                            String inputPwd = text.getText().toString();
                            //check password
                            if (conf.getPassword().equals(inputPwd)) {
                                // navigate to wizard view
                                if (conf.getTechBridgeId() != null)
                                    joinTechBridgeMeeting(conf);

                                Intent intent = new Intent();
                                intent.putExtra(Constants.ConferenceIdKey, conf.getConferenceId());
                                intent.putExtra(Constants.PasswordKey, inputPwd);
                                intent.putExtra(Constants.NameKey, conf.getName());
                                intent.putExtra(Constants.HostKey, false);
                                intent.putExtra(Constants.TechBridgeIdKey, conf.getTechBridgeId());
                                intent.setClass(ActionListActivity.this, WhiteBoardActivity.class);
                                popupWindow.dismiss();
                                ActionListActivity.this.startActivity(intent);
                            } else {
                                Toast.makeText(ActionListActivity.this, "密码不正确", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                    Button btn_cancel = (Button) contentView.findViewById(R.id.btn_cancel);
                    btn_cancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            popupWindow.dismiss();
                        }
                    });
                }
            });
            return convertView;
        }
    }

    class ViewHolder {
        TextView txtName;
        TextView txtDate;
        Button btnEnter;
        ImageView image;
    }
}

