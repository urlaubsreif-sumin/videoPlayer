package videoplayer2;

import android.media.MediaCodec;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;

public class PlayTask implements Runnable {
    private static final int MSG_PLAY_PAUSED = 0;
    private static final int MSG_PLAY_RESUMED = 1;
    private static final int MSG_PLAY_POSITION_CHANGED = 2;
    private static final int MSG_PLAY_DURATION_CHANGED = 3;

    private VideoPlayer player;

    private boolean inEOS = false;
    private boolean outEOS = false;

    Object pauseLock;
    boolean playing;
    boolean stop;

    Object seekLock;
    long seekTimeMs;
    long curTimeMs;

    Thread thread;

    public PlayTask(VideoPlayer player) throws IOException {
        this.player = player;

        pauseLock = new Object();
        seekLock = new Object();

        player.open();

        thread = new Thread(this, "Video Player");
        thread.start();
        
        // 정지 상태에서 시작
        pause();
    }

    public void play() {
        if(playing) {
            return;
        }

        synchronized (pauseLock) {
            playing = true;
            pauseLock.notifyAll();
        }
    }

    public void pause() {
        if(!playing) {
            return;
        }

        synchronized (pauseLock) {
            playing = false;
        }
    }

    public boolean checkPause() {
        boolean paused = false;

        synchronized (pauseLock) {
            if(!playing) {
                paused = true;

                while(!playing && stop == false) {
                    try {
                        pauseLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return paused;
    }

    public void seekTo(long position) {
        synchronized (seekLock) {
            seekTimeMs = position;
        }
    }

    public void previous(int unitTimeMs) {
        synchronized (seekLock) {
            seekTimeMs = curTimeMs - unitTimeMs;
        }
    }

    public void next(int unitTimeMs) {
        synchronized (seekLock) {
            seekTimeMs = curTimeMs + unitTimeMs;
        }
    }

    public void release() {
        stop = true;
        thread.interrupt();
        player.close();
    }

    @Override
    public void run() {
        curTimeMs = 0;
        long startTimeMs = System.currentTimeMillis();
        long lastNotificationTimeMs = 0;

        while(!stop) {
            if(checkPause()) {
                startTimeMs = System.currentTimeMillis() - curTimeMs;
            }
            synchronized (seekLock) {
                if(seekTimeMs != -1) {
                    player.seekTo(seekTimeMs);

                    curTimeMs = seekTimeMs;

                    startTimeMs = System.currentTimeMillis() - curTimeMs;
                    seekTimeMs = -1;
                }
            }
            if(!inEOS) {
                inEOS = player.inputBuffer();
            }

            if(!outEOS) {
                int codecStatus = player.getCodecStatus();
                if(codecStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    Log.d("테스트", "INFO_TRY_AGAIN_LATER");
                }
                else if(codecStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    Log.d("테스트", "INFO_OUTPUT_FORMAT_CHANGED");
                }
                else if(codecStatus < 0) {
                    Log.d("테스트", "UNEXPECTED");
                }
                else {
                    curTimeMs = player.getPresentationTimeMs();
                    Log.d("테스트", "curPresentationTimeMs: " + curTimeMs);

                    if((SystemClock.currentThreadTimeMillis() - lastNotificationTimeMs) > 10) {

                        lastNotificationTimeMs = SystemClock.currentThreadTimeMillis();
                    }

                    while(curTimeMs > (System.currentTimeMillis() - startTimeMs)) {
                        /*
                        try {
                            thread.wait(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                         */
                        if(!playing) {
                            break;
                        }
                        synchronized (seekLock) {
                            if(seekTimeMs != -1) {
                                break;
                            }
                        }
                    }

                    outEOS = player.outputBuffer(codecStatus);
                }
            }
            
            // 영상이 끝까지 재생된 경우
            if(inEOS && outEOS) {
                Log.d("테스트", "EOS!!");
                inEOS = false;
                outEOS = false;
                pause();
                seekTo(0);
            }
        }
    }
}
