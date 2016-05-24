package com.ifingers.yunwb;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.ifingers.yunwb.dao.ImageListItem;
import com.ifingers.yunwb.tasks.ClientWbTask;
import com.ifingers.yunwb.tasks.HostWbTask;
import com.ifingers.yunwb.tasks.ICommonTask;
import com.ifingers.yunwb.tasks.TaskManagers;
import com.ifingers.yunwb.tasks.TaskMsg;
import com.ifingers.yunwb.utility.ActivityCode;
import com.ifingers.yunwb.utility.Constants;
import com.ifingers.yunwb.utility.GuiHelper;
import com.ifingers.yunwb.utility.LocalConferenceRecords;
import com.ifingers.yunwb.utility.PaintTool;
import com.ifingers.yunwb.utility.ServerAPI;
import com.ifingers.yunwb.utility.ServerError;
import com.ifingers.yunwb.utility.TBConfManager;
import com.ifingers.yunwb.utility.WbMessager;
import com.ifingers.yunwb.utility.WhiteboardTaskContext;
import com.tb.conf.api.struct.ant.CAntThumbnail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class WhiteBoardActivity extends AppCompatActivity implements IViewDataUpdater, SurfaceHolder.Callback {

    private String TAG = "WhiteBoardActivity";
    private FragmentManager mFragManager;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private LinkedList<ImageListItem> mData = null;
    private WbFragAdapter mAdapter = null;
    private TaskManagers mTaskManager = TaskManagers.getInstance();
    private ClientWbTask clientWbTask = null;
    private HostWbTask hostWbTask = null;
    private CanvasFragment canvasFragment;
    private AttachmentFragment attachmentFragment;
    private BlankFragment blankFragment;
    private WhiteboardTaskContext globalConfig = WhiteboardTaskContext.getInstance();
    private ProgressBar waitingBar;
    private boolean mute = false;   //current mute status
    private MenuItem audioStatusMenuItem;

    private boolean isHost;
    private int snapshotIndex = 1;
    private String currentDate;
    private String conferenceId;
    private String password;
    private String meetingUrl;
    private String meetingName;
    private String deviceName;
    private boolean drawerOpened;
    private String techBridgeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_white_board);
        ButterKnife.bind(this);

        if (mData != null)
            return;

        GuiHelper.setActionBarTitle(this, getIntent().getStringExtra(Constants.NameKey));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mData = new LinkedList<>();
        ImageListItem data = new ImageListItem("", getResources().getString(R.string.white_board), 0);
        mData.add(data);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mFragManager = getSupportFragmentManager();
        mDrawerList = (ListView) findViewById(R.id.fg_right_menu);

        mAdapter = new WbFragAdapter(mData, this, mFragManager);
        mDrawerList.setAdapter(mAdapter);
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        mTaskManager.setMainView(this);
        mTaskManager.startTasks();

        canvasFragment = new CanvasFragment();
        Bundle args = new Bundle();
        //args.putInt(CanvasFragment., position);
        canvasFragment.setArguments(args);

        attachmentFragment = new AttachmentFragment();
        Bundle argsAttach = new Bundle();
        attachmentFragment.setArguments(argsAttach);

        blankFragment = new BlankFragment();
        blankFragment.setArguments(new Bundle());

        // Insert the fragment by replacing any existing fragment
        mFragManager.beginTransaction()
                .replace(R.id.ly_content, canvasFragment)
                .commit();

        waitingBar = (ProgressBar) findViewById(R.id.waiting_progress);

        Intent it = getIntent();

        isHost = it.getBooleanExtra(Constants.HostKey, false);
        conferenceId = getIntent().getStringExtra(Constants.ConferenceIdKey);
        meetingUrl = getIntent().getStringExtra(Constants.UrlKey);
        password = getIntent().getStringExtra(Constants.PasswordKey);
        meetingName = getIntent().getStringExtra(Constants.NameKey);
        deviceName = getIntent().getStringExtra(Constants.DeviceNameKey);
        techBridgeId = getIntent().getStringExtra(Constants.TechBridgeIdKey);

        if (currentDate == null) {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            currentDate = formatter.format(new Date());
            String folder = LocalConferenceRecords.getRootPath(WhiteBoardActivity.this) + currentDate + "/" + conferenceId + "_" + meetingName;
            String folderPrivate = folder + "/private/";
            String folderPublic = folder + "/public/";
            File privateFolder = new File(folderPrivate);
            File publicFolder = new File(folderPublic);
            if (!privateFolder.exists()) {
                privateFolder.mkdirs();
            }

            if (!publicFolder.exists())
                publicFolder.mkdirs();
        }

        final String cname = meetingName;

//        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
//                Gravity.RIGHT);

        mDrawerLayout.setDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerClosed(View view) {
                drawerOpened = false;
//                mDrawerLayout.setDrawerLockMode(
//                        DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.RIGHT);
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                if (newState == DrawerLayout.STATE_DRAGGING) {
                    canvasFragment.getSurfaceView().setVisibility(View.INVISIBLE);
                } else if (newState == DrawerLayout.STATE_IDLE) {
                    if (drawerOpened)
                        canvasFragment.getSurfaceView().setVisibility(View.INVISIBLE);
                    else {
                        if (!attachmentFragment.isVisible()) {
                            canvasFragment.getSurfaceView().setVisibility(View.VISIBLE);
                            if (clientWbTask != null)
                                clientWbTask.forcePush();
                            else
                                hostWbTask.forcePush();
                        }
                    }
                }
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                String rootPublic = LocalConferenceRecords.getRootPath(WhiteBoardActivity.this) + currentDate + "/" + conferenceId + "_" + cname + "/public";
                String rootPrivate = LocalConferenceRecords.getRootPath(WhiteBoardActivity.this) + currentDate + "/" + conferenceId + "_" + cname + "/private";

                mData = new LinkedList<>();
                ImageListItem data = new ImageListItem("", getResources().getString(R.string.white_board), 0);
                mData.add(data);

                String publicFiles[] = new File(rootPublic).list();
                String privateFiles[] = new File(rootPrivate).list();

                addFile(publicFiles, 1, rootPublic);

                if (isTechBridgeEnabled()) {
                    ArrayList<CAntThumbnail> docs = TBConfManager.getDocList();
                    if (docs != null) {
                        for (CAntThumbnail doc : docs) {
                            ImageListItem item = new ImageListItem(doc.filePath, doc.name, doc.docId, true);
                            mData.add(item);
                        }
                    }
                }

                //comment out below, because only want to display public snapshot here
                //addFile(privateFiles, publicFiles.length + 1, rootPrivate);
                mAdapter.updateImageLinks(mData);
                canvasFragment.getSurfaceView().setVisibility(View.INVISIBLE);

                drawerOpened = true;
            }

//            @Override
//            public void onDrawerStateChanged(int newState) {
//                if (newState == DrawerLayout.STATE_DRAGGING) {
//                    canvasFragment.getSurfaceView().setVisibility(View.INVISIBLE);
//                }
//            }

            private void addFile(String[] files, int startIndex, String root) {
                int index = startIndex;
                if (files != null)
                    Arrays.sort(files);

                for (String file : files) {
                    ImageListItem data = new ImageListItem(root + "/" + file, file, index);
                    mData.add(data);
                    index++;
                }
            }
        });

        LinearLayout btnSnapshot = (LinearLayout) findViewById(R.id.action_snapshot);
        btnSnapshot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (canvasFragment.isVisible()) {
                    snapshot();
                }
            }
        });

        LinearLayout btnMembers = (LinearLayout) findViewById(R.id.wb_members);
        btnMembers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent it = new Intent(WhiteBoardActivity.this, ConferenceInfoActivity.class);
                it.putExtra("id", conferenceId);
                it.putExtra("password", password);
                startActivityForResult(it, ActivityCode.REQUEST_CONFERENCE_INFO);
            }
        });

        LinearLayout btnMore = (LinearLayout) findViewById(R.id.action_wb_more);
        btnMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent it = new Intent(WhiteBoardActivity.this, ConferenceRecordFolderActivity.class);
                it.putExtra("id", conferenceId);
                it.putExtra("date", currentDate);
                it.putExtra("name", meetingName);
                it.putExtra("alive", true);
                startActivity(it);
            }
        });

        muteAudio(mute);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (isHost && hostWbTask == null) {
            hostWbTask = new HostWbTask(meetingUrl, conferenceId, holder, WhiteBoardActivity.this, currentDate, meetingName, deviceName);
            new Thread(hostWbTask).start();
        } else if (!isHost && clientWbTask == null) {
            // join a conference
            clientWbTask = new ClientWbTask(conferenceId, password, TaskManagers.getInstance(),
                    holder, WhiteBoardActivity.this, currentDate, meetingName);
            new Thread(clientWbTask).start();
        }
        //NavUtils.navigateUpFromSameTask();

        if (canvasFragment.isVisible()) {
            if (clientWbTask != null)
                clientWbTask.forcePush();
            else if (hostWbTask != null)
                hostWbTask.forcePush();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.attach, menu);

        audioStatusMenuItem = menu.findItem(R.id.action_sound);
        if (isTechBridgeEnabled()) {
            int resId = mute ? R.mipmap.mute_status : R.mipmap.unmute_status;
            audioStatusMenuItem.setIcon(resId);
        } else {
            audioStatusMenuItem.setIcon(R.mipmap.mute_status);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                askForExitConference();
                return true;
            case R.id.action_attach:
                canvasFragment.getSurfaceView().setVisibility(View.INVISIBLE);
                mDrawerLayout.openDrawer(Gravity.RIGHT);
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED,
                        GravityCompat.END);
                return true;
            case R.id.action_sound:
                //check current status
                if (isTechBridgeEnabled()) {
                    int resId = !mute ? R.mipmap.mute_status : R.mipmap.unmute_status;
                    audioStatusMenuItem.setIcon(resId);
                    muteAudio(!mute);
                } else {
                    audioStatusMenuItem.setIcon(R.mipmap.mute_status);
                }

                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @OnClick({R.id.layout_rotate_left, R.id.layout_rotate_right})
    public void viewportRotate(View view) {
        if (view.getId() == R.id.layout_rotate_left) {
            PaintTool.getInstance().rotateLeft();
        } else {
            PaintTool.getInstance().rotateRight();
        }
    }

    public void showProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
        if (show)
            canvasFragment.getSurfaceView().setVisibility(View.INVISIBLE);
        else
            canvasFragment.getSurfaceView().setVisibility(View.VISIBLE);

        waitingBar.setVisibility(show ? View.VISIBLE : View.GONE);
        waitingBar.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                waitingBar.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    private boolean isTechBridgeEnabled() {
        return this.techBridgeId != null;
    }

    private void muteAudio(boolean mute) {
        if (isTechBridgeEnabled()) {
            this.mute = mute;
            TBConfManager.mute(mute);
        }
    }

    private void snapshot() {
        try {
            // image naming and path  to include sd card  appending name you choose for file
            String folder = WhiteBoardActivity.this.getApplicationInfo().dataDir + "/history/" + currentDate + "/" + conferenceId + "_" + meetingName;
            if (isHost)
                folder += "/public/";
            else
                folder += "/private/";

            String path = folder + "snapshot" + snapshotIndex + ".png";

            // create bitmap screen capture
            final Bitmap bitmap;
            if (clientWbTask != null)
                bitmap = clientWbTask.getData();
            else
                bitmap = hostWbTask.getData();

            File imageFile = new File(path);
            while (imageFile.exists()) {
                snapshotIndex++;
                path = folder + "snapshot" + snapshotIndex + ".png";
                imageFile = new File(path);
            }
            imageFile.createNewFile();
            final FileOutputStream outputStream = new FileOutputStream(imageFile);
            int quality = 100;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);

            outputStream.flush();
            outputStream.close();

            //upload to server
            if (isHost) {
                uploadSnapshot(bitmap, "snapshot" + snapshotIndex);
            }

            String msg = isHost ? "已保存到公共会议记录中" : "已保存到个人会议记录中";
            Toast.makeText(WhiteBoardActivity.this, msg, Toast.LENGTH_SHORT).show();
            snapshotIndex++;
        } catch (IOException e) {
            Toast.makeText(WhiteBoardActivity.this, "保存失败", Toast.LENGTH_SHORT).show();
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ActivityCode.REQUEST_CONFERENCE_INFO) {
            if (resultCode == ActivityCode.RESULT_EXIT_CONFERENCE) {
                exitConference();
            }
        }
    }

    @Override
    public void onBackPressed() {
        askForExitConference();
    }

    private void exitConference() {
        if (clientWbTask != null) {
            clientWbTask.stop();
            clientWbTask = null;
            if (techBridgeId != null) {
                TBConfManager.leaveConf(false);
            }
        }

        if (hostWbTask != null) {
            hostWbTask.stop();
            hostWbTask = null;
            if (techBridgeId != null) {
                TBConfManager.leaveConf(true);
            }
        }

        finish();
    }

    private void askForExitConference() {
        String title = getResources().getString(R.string.prompt_title_info);
        String content = isHost ? getResources().getString(R.string.confirm_end_meeting) : getResources().getString(R.string.confirm_exit_meeting);
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
                        exitConference();
                    }
                }
        );
    }

    private void uploadSnapshot(final Bitmap bitmap, final String name) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
                ServerAPI.ConferenceData result = ServerAPI.getInstance().uploadImage(conferenceId, name, bos.toByteArray());
                if (result.getCode() != ServerError.OK) {
                    mTaskManager.handleTaskMsg(null, TaskMsg.UploadImageFailed);
                }
            }
        }).start();
    }

    @Override
    public void handleModelMsg(TaskMsg msg, ICommonTask task) {
        if (task == null) {
            Toast.makeText(WhiteBoardActivity.this, getResources().getString(R.string.prompt_upload_snapshot_failure), Toast.LENGTH_LONG).show();
        } else if (task instanceof ClientWbTask) {
            if (msg == TaskMsg.ConferenceFinished) {
                if (techBridgeId != null) {
                    TBConfManager.leaveConf(false);
                }
                Toast.makeText(WhiteBoardActivity.this, getResources().getString(R.string.prompt_conference_finished), Toast.LENGTH_LONG).show();
                this.finish();
            } else if (msg == TaskMsg.JoinConferenceFailed) {
                Toast.makeText(WhiteBoardActivity.this, getResources().getString(R.string.prompt_join_failure), Toast.LENGTH_LONG).show();
                this.finish();
            } else if (msg == TaskMsg.LostFrame) {
                Toast.makeText(this, getResources().getString(R.string.server_connection_down), Toast.LENGTH_LONG).show();
            }
        } else if (msg == TaskMsg.AttachmentSelected) {
            AttachmentFragment frag = (AttachmentFragment) mFragManager.findFragmentById(R.id.ly_content);
            frag.updateImage((String) task.getData());
        } else if (msg == TaskMsg.SdkAttachmentSelected) {
            TBConfManager.showDoc((FrameLayout) findViewById(R.id.ly_content), Integer.parseInt((String) task.getData()));
        } else if (task instanceof HostWbTask) {
            if (msg == TaskMsg.WhiteboardHwSnapshot) {
                snapshot();
            } else if (msg == TaskMsg.ConferenceFinished) {
                if (techBridgeId != null) {
                    TBConfManager.leaveConf(true);
                }
                Toast.makeText(WhiteBoardActivity.this, getResources().getString(R.string.prompt_conference_finished), Toast.LENGTH_LONG).show();
                this.finish();
            }
        }
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            selectItem(parent, view, position, id);
        }
    }

    /**
     * Swaps fragments in the main content view
     */
    private void selectItem(AdapterView parent, View view, final int position, long id) {
        // Create a new fragment and specify the planet to show based on position

        if (position == 0) {
            // Insert the fragment by replacing any existing fragment
            TBConfManager.hideDoc();
            mFragManager.beginTransaction()
                    .replace(R.id.ly_content, canvasFragment)
                    .commit();
            mDrawerLayout.closeDrawer(Gravity.RIGHT);
        } else {
            TBConfManager.hideDoc();
//            ImageListItem item = (ImageListItem)mDrawerList.getItemAtPosition(position);
//            attachmentFragment.updateImage(item.getmIconResPath());
            ImageListItem item = (ImageListItem) mDrawerList.getItemAtPosition(position);

            if (item.getShowFromSDK())
                mFragManager.beginTransaction().replace(R.id.ly_content, blankFragment).commit();
            else
                mFragManager.beginTransaction().replace(R.id.ly_content, attachmentFragment).commit();

            mFragManager.executePendingTransactions();
            mTaskManager.handleTaskMsg(new ICommonTask<String>() {
                @Override
                public void start() {

                }

                @Override
                public void stop() {

                }

                @Override
                public String getData() {
                    ImageListItem item = (ImageListItem) mDrawerList.getItemAtPosition(position);
                    if (!item.getShowFromSDK())
                        return item.getmIconResPath();
                    else
                        return String.valueOf(item.getmIcon());
                }

                @Override
                public void forcePush() {

                }
            }, item.getShowFromSDK() ? TaskMsg.SdkAttachmentSelected : TaskMsg.AttachmentSelected);
        }
    }
}
