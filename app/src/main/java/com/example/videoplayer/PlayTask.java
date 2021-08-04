package com.example.videoplayer;

import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;

import java.io.IOException;

public class PlayTask implements Runnable {
    private static final int MSG_PLAY_STOPPED = 0;

    private VideoPlayer videoPlayer;
    private VideoPlayer.PlayerFeedback feedback;
    private Thread thread;
    private LocalHandler localHandler;

    private final Object stopLock = new Object();
    private final Object pauseLock = new Object();
    private boolean stopped = false;
    private boolean pause = false;

    public PlayTask(VideoPlayer videoPlayer, VideoPlayer.PlayerFeedback feedback) {
        this.videoPlayer = videoPlayer;
        this.feedback = feedback;

        localHandler = new LocalHandler();
    }

    public void execute() {
        thread = new Thread(this, "Video Player");
        thread.start();
    }

    public void pause() {
        if(pause) {
            return;
        }
        synchronized (pauseLock) {
            pause = true;
        }
        
        videoPlayer.pause();
    }

    public void movePrevious() {

    }

    public void moveNext() {

    }

    public void seekTo(int time) {
        //TODO
    }

    public void waitForStop() {
        synchronized (stopLock) {
            while(!stopped) {
                try {
                    stopLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void run() {
        try {
            videoPlayer.play();
        } finally {
            synchronized (stopLock) {
                stopped = true;
                stopLock.notifyAll();
            }

            localHandler.sendMessage(
                    localHandler.obtainMessage(MSG_PLAY_STOPPED, feedback));
        }
    }

    private static class LocalHandler extends Handler {
        @Override
        public void handleMessage(@NonNull Message msg) {
            int what = msg.what;

            switch(what) {
                case MSG_PLAY_STOPPED:
                    VideoPlayer.PlayerFeedback feedback = (VideoPlayer.PlayerFeedback) msg.obj;
                    feedback.playbackStopped();
                    break;
                default:
                    throw new RuntimeException("Unknown msg " + what);
            }
        }
    }
}
