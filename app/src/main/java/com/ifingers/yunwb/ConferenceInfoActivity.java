package com.ifingers.yunwb;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.ifingers.yunwb.customizeview.DatePickerFragment;
import com.ifingers.yunwb.customizeview.TimePickerFragment;
import com.ifingers.yunwb.dao.ConferenceDao;
import com.ifingers.yunwb.dao.MeetingDao;
import com.ifingers.yunwb.dao.UserDao;
import com.ifingers.yunwb.services.WXService;
import com.ifingers.yunwb.utility.ActivityCode;
import com.ifingers.yunwb.utility.Constants;
import com.ifingers.yunwb.utility.GuiHelper;
import com.ifingers.yunwb.utility.ServerAPI;
import com.ifingers.yunwb.utility.ServerError;
import com.ifingers.yunwb.utility.WhiteboardTaskContext;

import java.text.SimpleDateFormat;
import java.util.List;

public class ConferenceInfoActivity extends AppCompatActivity {

    private String conferenceId;
    private String password;
    private ConferenceDao conference;
    private List<UserDao> aliveUsers;
    private UserDao hostInfo;
    private boolean isHost;
    private String TAG = "ConferenceInfoActivity";
    private Button btnExit;
    private WhiteboardTaskContext config = WhiteboardTaskContext.getInstance();
    private View progressView;
    private View infoView;
    private GridView view;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.invite, menu);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conference_info);
        GuiHelper.setActionBarTitle(this, "会议信息");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            conferenceId = getIntent().getStringExtra("id");
            password = getIntent().getStringExtra("password");
        } else {
            conferenceId = savedInstanceState.getString("id");
            password = savedInstanceState.getString("password");
        }

        btnExit = (Button) findViewById(R.id.btn_exit_conf);
        btnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(ActivityCode.RESULT_EXIT_CONFERENCE);
                finish();
            }
        });

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

        progressView = findViewById(R.id.progress);
        infoView = findViewById(R.id.info_view);
        showProgress(true);
        PullConferenceInfo task = new PullConferenceInfo();
        task.execute();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState){
        savedInstanceState.putString("password", password);
        savedInstanceState.putString("id", conferenceId);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case R.id.action_invite:
                String title = String.format(conference.getCompany() + "发起会议:" + conference.getName());
                String desc = String.format("会议密码:" + conference.getPassword() + "\r\n会议地址:" + conference.getLocation() + "\n会议概要:" + conference.getConferenceAbstract());
                String url = ServerAPI.getInstance().getInviteUrl(conference.getConferenceId(), conference.getPassword(), conference.getName());
                WXService.getInstance().shareWeb(url, title, desc);
                return true;
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);

                boolean updateConfInfo = false;

                if (conference == null)
                    return true;

                AutoCompleteTextView meetingname = (AutoCompleteTextView) findViewById(R.id.meeting_name);
                if (!conference.getName().equals(meetingname.getText().toString())) {
                    conference.setName(meetingname.getText().toString());
                    updateConfInfo = true;
                }

                AutoCompleteTextView password = (AutoCompleteTextView) findViewById(R.id.meeting_password);
                if (!conference.getPassword().equals(password.getText().toString())) {
                    conference.setPassword(password.getText().toString());
                    updateConfInfo = true;
                }

                AutoCompleteTextView meetinglocation = (AutoCompleteTextView) findViewById(R.id.meeting_location);
                if (!conference.getLocation().equals(meetinglocation.getText().toString())) {
                    conference.setLocation(meetinglocation.getText().toString());
                    updateConfInfo = true;
                }

                AutoCompleteTextView meetingcompany = (AutoCompleteTextView) findViewById(R.id.meeting_company);
                if (!conference.getCompany().equals(meetingcompany.getText().toString())) {
                    conference.setCompany(meetingcompany.getText().toString());
                    updateConfInfo = true;
                }

                AutoCompleteTextView meetingabstract = (AutoCompleteTextView) findViewById(R.id.meeting_abstract);
                if (!conference.getConferenceAbstract().equals(meetingabstract.getText().toString())) {
                    conference.setConferenceAbstract(meetingabstract.getText().toString());
                    updateConfInfo = true;
                }

                AutoCompleteTextView nickname = (AutoCompleteTextView) findViewById(R.id.meeting_nick);
                String myNickname = conference.findNickname(config.getUserId());
                if (!nickname.getText().toString().equals(myNickname)) {
                    conference.setNicknameForUser(config.getUserId(), nickname.getText().toString());
                    updateConfInfo = true;
                }

                if (updateConfInfo) {
                    ServerAPI.ConferenceData conferenceData = ServerAPI.getInstance().updateConference(conference);
                    if (conferenceData.getCode() != ServerError.OK) {
                        Toast.makeText(this, "更新会议信息失败", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }

                conference = null;
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            infoView.setVisibility(show ? View.GONE : View.VISIBLE);
            infoView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    infoView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            progressView.setVisibility(show ? View.VISIBLE : View.GONE);
            progressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    progressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            progressView.setVisibility(show ? View.VISIBLE : View.GONE);
            infoView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    class PullConferenceInfo extends AsyncTask {

        @Override
        protected Object doInBackground(Object[] params) {
            MeetingDao meetingDao = config.getMeetingInfo();
            if (meetingDao != null) {
                //try get conference info from server immediately
                final ServerAPI.ConferenceData data = ServerAPI.getInstance().requestConferenceInfo(conferenceId, password);
                if (data.getCode() == ServerError.OK) {
                    //if ok, update the data in meeting dao
                    meetingDao.setConference(data.getConference());
                } else {
                    ConferenceInfoActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Resources res = ConferenceInfoActivity.this.getResources();
                            Toast.makeText(ConferenceInfoActivity.this, String.format(res.getString(R.string.network_slow_code), data.getCode()), Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                conference = new ConferenceDao();
                conference.fill(meetingDao.getConference());
                String hostId = conference.getHostId();
                isHost = hostId.equals(config.getUserInfo().get_id());
                //only fetch alive users and host
                List<String> userIds = meetingDao.getAliveUsers();
                boolean containHost = false;
                for (String id : userIds) {
                    if (id.equals(hostId)) {
                        containHost = true;
                        break;
                    }
                }
                if (!containHost)
                    userIds.add(hostId);

                ServerAPI.UserListData userListData = ServerAPI.getInstance().getUserList(userIds);
                final int code = userListData.getCode();
                if (code == ServerError.OK) {
                    //find out host info
                    List<UserDao> users = userListData.getUsers();
                    int index = 0;
                    for (UserDao user : users) {
                        if (user.get_id().equals(hostId)) {
                            break;
                        }
                        index++;
                    }

                    if (index < users.size()) {
                        hostInfo = users.get(index);
                    }

                    aliveUsers = users;
                }
                else {
                    ConferenceInfoActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Resources res = ConferenceInfoActivity.this.getResources();
                            Toast.makeText(ConferenceInfoActivity.this, String.format(res.getString(R.string.network_slow_code), code), Toast.LENGTH_SHORT).show();
                        }
                    });
                    aliveUsers = null;
                }
            } else
                conference = null;

            return conference;
        }

        @Override
        protected void onPostExecute(Object obj) {
            if (conference != null) {
                setData();
            }

            showProgress(false);
        }
    }

    private void setTextValue(int id, String value, boolean canEditor) {
        AutoCompleteTextView view = (AutoCompleteTextView) ConferenceInfoActivity.this.findViewById(id);
        view.setText(value);
        view.setEnabled(canEditor);
    }

    private void setData(){
        setTextValue(R.id.meeting_name, conference.getName(), isHost);
        setTextValue(R.id.meeting_location, conference.getLocation(), isHost);
        setTextValue(R.id.meeting_company, conference.getCompany(), isHost);
        setTextValue(R.id.meeting_abstract, conference.getConferenceAbstract(), isHost);
        setTextValue(R.id.meeting_password, conference.getPassword(), isHost);

        if (hostInfo != null)
            setTextValue(R.id.meeting_host, hostInfo.getName(), false);
        else
            setTextValue(R.id.meeting_host, "没有找到用户信息", false);

        TextView members = (TextView)findViewById(R.id.meeting_members);

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String datetime[] = format.format(conference.getDate().getTime()).split(" ");
        setTextValue(R.id.meeting_date, datetime[0], isHost);
        setTextValue(R.id.meeting_time, datetime[1], isHost);

        members.setText(Integer.toString(conference.getUsers().size()) + "人");
        if (isHost) {
            btnExit.setText("结束会议");
        } else {
            btnExit.setText("退出会议");
        }

        if (aliveUsers != null) {
            String myId = config.getUserInfo().get_id();
            setTextValue(R.id.meeting_nick, conference.findNickname(myId), true);

            MembersAdapter adapter = new MembersAdapter(aliveUsers, conference);
            view = (GridView) ConferenceInfoActivity.this.findViewById(R.id.members);
            view.setAdapter(adapter);
            view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Intent it = new Intent(ConferenceInfoActivity.this, UserInfoActivity.class);
                    it.putExtra("name", aliveUsers.get(position).getName());
                    it.putExtra("company", aliveUsers.get(position).getCompany());
                    it.putExtra("title", aliveUsers.get(position).getJobTitle());
                    it.putExtra("url", aliveUsers.get(position).getAvatarUrl());
                    it.putExtra("wx", aliveUsers.get(position).getWeixinOpenId());
                    startActivity(it);
                }
            });
        } else {
            Log.e(TAG, "failed to get user info");
        }
    }

    class MembersAdapter extends BaseAdapter {

        private List<UserDao> users;
        private ConferenceDao conference;

        public MembersAdapter(List<UserDao> users, ConferenceDao conference) {
            this.users = users;
            this.conference = conference;
        }

        @Override
        public int getCount() {
            return users == null? 0 : users.size();
        }

        @Override
        public Object getItem(int position) {
            return users.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder item;
            if (convertView == null) {
                convertView = LayoutInflater.from(ConferenceInfoActivity.this).inflate(R.layout.grid_user_info, parent, false);
                item = new ViewHolder();
                item.image = (ImageView) convertView.findViewById(R.id.user_image);
                item.name = (TextView) convertView.findViewById(R.id.user_name);
                convertView.setTag(item);
            } else {
                item = (ViewHolder) convertView.getTag();
            }

            String userId = users.get(position).get_id();
            item.name.setText(conference.findNickname(userId));

            DisplayMetrics displaymetrics = new DisplayMetrics();
            ConferenceInfoActivity.this.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
            int size = displaymetrics.widthPixels / 4;
            Bitmap bitmap = ThumbnailUtils.extractThumbnail(ServerAPI.getInstance().getImageBitmap(users.get(position).getAvatarUrl()), size, size);
            item.image.setImageBitmap(bitmap);
            if (bitmap == null) {
                ConferenceInfoActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Resources res = ConferenceInfoActivity.this.getResources();
                        Toast.makeText(ConferenceInfoActivity.this, String.format(res.getString(R.string.network_slow_text), "加载头像失败"), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            return convertView;
        }
    }

    class ViewHolder {
        ImageView image;
        TextView name;
    }
}
