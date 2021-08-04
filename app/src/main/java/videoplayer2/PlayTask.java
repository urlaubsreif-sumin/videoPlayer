package videoplayer2;

import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;

public class PlayTask implements Runnable {
    private static final int MSG_PLAY_PAUSED = 0;
    private static final int MSG_PLAY_RESUMED = 1;
    private static final int MSG_PLAY_POSITION_CHANGED = 2;

    private VideoPlayer player;
    private VideoPlayer.PlayerFeedback feedback;
    LocalHandler handler;

    public PlayTask(VideoPlayer player, VideoPlayer.PlayerFeedback feedback) {
        this.player = player;
        this.feedback = feedback;

        handler = new LocalHandler();
    }

    public void play() {

    }

    public void resume() {

    }

    public void pause() {

    }

    public void seekTo(long seekTimeMs) {

    }

    public void previous(int ms) {

    }

    public void next(int ms) {

    }

    @Override
    public void run() {

    }

    private static class LocalHandler extends Handler {
        @Override
        public void handleMessage(@NonNull Message msg) {
            int what = msg.what;

            switch(what) {
                case MSG_PLAY_PAUSED:

                case MSG_PLAY_RESUMED:

                case MSG_PLAY_POSITION_CHANGED:

                default:
                    throw new RuntimeException("Unknown msg " + what);
            }
        }
    }
}
