package com.ifingers.yunwb.tasks;

/**
 *
 * Created by Macoo on 1/28/2016.
 */
public interface ICommonTask<T> {
    void start();
    void stop();
    T getData();
    void forcePush();
}
