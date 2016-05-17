package com.ifingers.yunwb.dao;

/**
 * Created by Macoo on 1/25/2016.
 */
public class ImageListItem {

    private String mIconResPath;
    private String mTitle;
    private int mIcon;
    private boolean showFromSDK;

    public  ImageListItem(){

    }

    public ImageListItem(String resPath, String title, int icon){
        mIconResPath = resPath;
        mTitle = title;
        mIcon = icon;
        showFromSDK = false;
    }

    public ImageListItem(String resPath, String title, int icon, boolean isShowFromSDK){
        mIconResPath = resPath;
        mTitle = title;
        mIcon = icon;
        this.showFromSDK = isShowFromSDK;
    }

    public String getmIconResPath() {
        return mIconResPath;
    }

    public String getmTitle() {
        return mTitle;
    }

    public int getmIcon() {
        return mIcon;
    }

    public boolean getShowFromSDK() {return showFromSDK;}
}
