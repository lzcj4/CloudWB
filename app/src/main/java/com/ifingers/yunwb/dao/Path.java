package com.ifingers.yunwb.dao;

import java.util.LinkedList;

/**
 * for draw path
 * Created by Macoo on 2/2/2016.
 */
public class Path {

    private LinkedList<TouchPoint> points = new LinkedList<>();

    private int groupId;

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public void addPoint(TouchPoint point){
        points.add(point);
    }

    public LinkedList<TouchPoint> getPoints() {
        return points;
    }
}

