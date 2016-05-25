package com.ifingers.yunwb.dao;

import java.util.HashMap;
import java.util.List;

/**
 * Created by SFY on 3/4/2016.
 */
public class MeetingDao {
    private String _id;
    private List<String> aliveAnonymous;
    private List<String> aliveUsers;
    private String folderPath;
    private String url;
    private HashMap<String, Object> conference;

    public String get_id() {
        return _id;
    }

    public List<String> getAliveAnonymous() {
        return aliveAnonymous;
    }

    public List<String> getAliveUsers() {
        return aliveUsers;
    }

    public String getFolderPath() {
        return folderPath;
    }

    public String getUrl() {
        return url;
    }

    public HashMap<String, Object> getConference() {
        return conference;
    }

    public void setConference(HashMap<String, Object> map) {
        conference = map;
    }
}
