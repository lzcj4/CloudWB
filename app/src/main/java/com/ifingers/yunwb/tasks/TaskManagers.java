package com.ifingers.yunwb.tasks;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.ifingers.yunwb.IViewDataUpdater;

import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


/**
 * pass thread msg to view
 * Created by Macoo on 1/28/2016.
 */
public class TaskManagers {

    private volatile static TaskManagers mInstance;
    private Handler mHandler;
    private IViewDataUpdater mContext;
    private LinkedList<ICommonTask> mTasks = new LinkedList<>();
    private ScheduledExecutorService mScheduleTaskExecutor;

    public static TaskManagers getInstance() {
        if (mInstance == null) {
            synchronized (TaskManagers.class) {
                if (mInstance == null) {
                    mInstance = new TaskManagers();
                }
            }
        }

        return mInstance;
    }

    public void setMainView(IViewDataUpdater context){
        mContext = context;
    }

    private TaskManagers() {
        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {
                TaskMsg msg = TaskMsg.values()[inputMessage.what];
                ICommonTask task = (ICommonTask)inputMessage.obj;
                mContext.handleModelMsg(msg, task);
            }
        };
        mScheduleTaskExecutor = Executors.newScheduledThreadPool(5);

    }

    public void startTasks() {
//        mTasks.add(new HostTask(this));
//        for(final ICommonTask task : mTasks){
//            mScheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {
//                @Override
//                public void run() {
//                    task.start();
//                }
//            }, 5, 5, TimeUnit.SECONDS);
//        }
    }

    public void stopTasks(){
        mScheduleTaskExecutor.shutdown();
    }

    public void handleTaskMsg(ICommonTask task, TaskMsg msg) {

        Message resourceUpdated = mHandler.obtainMessage(msg.ordinal(), task);
        resourceUpdated.sendToTarget();
//        switch (msg) {
//            case ResourceUpdated:
//                Message resourceUpdated = mHandler.obtainMessage(msg.ordinal(), task);
//                resourceUpdated.sendToTarget();
//                break;
//            default:
//                break; // do nothing
//        }
    }
}
