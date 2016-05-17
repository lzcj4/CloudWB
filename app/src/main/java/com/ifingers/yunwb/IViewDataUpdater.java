package com.ifingers.yunwb;

import com.ifingers.yunwb.tasks.ICommonTask;
import com.ifingers.yunwb.tasks.TaskMsg;

/**
 * Created by Macoo on 1/28/2016.
 */
public interface IViewDataUpdater {
    void handleModelMsg(TaskMsg msg, ICommonTask task);
}
