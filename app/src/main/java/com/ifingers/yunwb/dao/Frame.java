package com.ifingers.yunwb.dao;

import java.util.LinkedList;

/**
 * Get/Post from/to server
 * Created by Macoo on 2/5/2016.
 */
public class Frame {
    private LinkedList<Path> paths = new LinkedList<>();

    public Frame(){

    }

    public int getNumber(){
        if (paths == null)
            return 0;
        else
            return paths.size();
    }

    public LinkedList<Path> getPaths() {
        return paths;
    }

    public void addPath(Path path){
        paths.add(path);

    }
}
