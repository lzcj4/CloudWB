package com.ifingers.yunwb.bluetooth;

import java.util.List;

public interface IrmtInterface {
    public abstract void onGestureGet(int GestureNo);
    public abstract void onTouchUp(List<TouchScreen.TouchPoint> mTouchList);
    public abstract void onTouchDown(List<TouchScreen.TouchPoint> mTouchList);
    public abstract void onTouchMove(List<TouchScreen.TouchPoint> mTouchList);
    public abstract void onSnapshot(int mSnapShot);
    public abstract void onIdGet(long touchScreenID);
    public abstract void onError(int ErrorCode);
    public abstract void onBLconnected();
}
