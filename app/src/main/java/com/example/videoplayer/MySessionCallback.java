package com.example.videoplayer;

import android.support.v4.media.session.MediaSessionCompat;

/*
Android 버전 6.0 이하에서는 onPause() 콜백에서 플레이어를 중지함.
Android 버전 7.0 이상에서는 onStop() 콜백에서 플레이어를 중지함.
 */

public class MySessionCallback extends MediaSessionCompat.Callback {
    @Override
    public void onPlay() {
        super.onPlay();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onSkipToNext() {
        super.onSkipToNext();
    }

    @Override
    public void onSkipToPrevious() {
        super.onSkipToPrevious();
    }
}
