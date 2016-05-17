package com.ifingers.yunwb.dao;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 *  Conference data
 * Created by Macoo on 2/9/2016.
 */
public class ConferenceDao implements Serializable {

    private String name;
    private String password;
    private ArrayList<String> users;
    private HashMap<String, String> nicknames;//key = user id, value = nickname, for easy fetch
    private Calendar date;
    private String location;
    private String company;
    private String conferenceAbstract;
    private ArrayList<String> images;
    private String conferenceId;
    private String techBridgeId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public ArrayList<String> getUsers() {
        return users;
    }

    public void setUsers(ArrayList<String> users, ArrayList<String> nicknameList) {
        this.users = users;
        this.nicknames = new HashMap<>();
        for (int i = 0; i < users.size(); i++) {
            nicknames.put(users.get(i), nicknameList.get(i));
        }
    }

    public void setNicknameForUser(String userId, String nickname) {
        nicknames.put(userId, nickname);
    }

    public Calendar getDate() {
        return date;
    }

    public void setDate(Calendar date) {
        this.date = date;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getConferenceAbstract() {
        return conferenceAbstract;
    }

    public void setConferenceAbstract(String conferenceAbstract) {
        this.conferenceAbstract = conferenceAbstract;
    }

    public ArrayList<String> getImages() {
        return images;
    }

    public void setImages(ArrayList<String> images) {
        this.images = images;
    }

    public String getConferenceId() {
        return conferenceId;
    }

    public void setConferenceId(String conferenceId) {
        this.conferenceId = conferenceId;
    }

    public String getTechBridgeId() {
        return techBridgeId;
    }

    public void setTechBridgeId(String techBridgeId) {
        this.techBridgeId = techBridgeId;
    }

    public String findNickname(String userId) {
        return nicknames.get(userId);
    }

    public String getHostId() {
        return users.get(0);
    }

    public void fill(Map map) {
        this.setConferenceId((String) map.get("_id"));
        this.setName((String) map.get("name"));
        this.setPassword((String) map.get("password"));
        this.setLocation((String) map.get("location"));
        this.setCompany((String) map.get("company"));
        this.setConferenceAbstract((String) map.get("abstract"));

        long longVal = (long)(double)map.get("datetime");
        Date date = new Date(longVal);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        this.setDate(calendar);

        ArrayList<String> users = (ArrayList<String>)map.get("users");
        ArrayList<String> nicknameList = (ArrayList<String>)map.get("nicknames");
        this.setUsers(users, nicknameList);

        this.setImages((ArrayList)map.get("images"));
        this.setTechBridgeId((String) map.get("techBridgeId"));
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("_id", conferenceId);
        map.put("name", name);
        map.put("password", password);

        if (date != null) {
            map.put("datetime", date.getTime().getTime());
        }

        map.put("location", location);
        map.put("company", company);
        map.put("abstract", conferenceAbstract);
        map.put("users", users);
        ArrayList<String> nicknameList = new ArrayList<>();
        for (String uid : users) {
            String nickname = nicknames.get(uid);
            nicknameList.add(nickname);
        }
        map.put("nicknames", nicknameList);
        map.put("images", images);
        map.put("techBridgeId", techBridgeId);
        return map;
    }
}
