package com.ifingers.yunwb.dao;

import java.util.LinkedList;

/**
 * object from server, contain frames
 * Created by Macoo on 2/6/2016.
 */
public class FrameSet {
    private int seqNum;
    private int frameSize;

    public int getFrameSize() {
        return frameSize;
    }

    public void setFrameSize(int frameSize) {
        this.frameSize = frameSize;
    }

    private LinkedList<Frame> frames = new LinkedList<>();

    public int getSeqNum() {
        return seqNum;
    }

    public void setSeqNum(int seqNum) {
        this.seqNum = seqNum;
    }

    public void addFrame(Frame frame){
        frames.add(frame);
    }

    public LinkedList<Frame> getFrames() {
        return frames;
    }
}
