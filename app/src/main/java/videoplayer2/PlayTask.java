package videoplayer2;

import android.media.MediaCodec;

import java.io.IOException;

public class PlayTask implements Runnable {
    
    private VideoPlayer player;
    Thread thread; // 영상 재생 Thread

    private boolean inEOS = false; // 영상 파일 -> inputBuffer 이동 완료 여부
    private boolean outEOS = false; // outputBuffer -> 출력 완료 여부

    Object pauseLock;
    boolean playing; // 재생 상태 (재생 or 일시중지)
    boolean isPlayerValid; // Player 유효

    Object seekLock;
    long seekTimeMs; // 목표 재생 지점
    long curTimeMs; // 현재 재생 지점

    

    // ---------------------------------------------------------------
    // Constructor.
    //
    
    public PlayTask(VideoPlayer player) throws IOException {
        this.player = player;

        initLocks();

        // Player 시작
        player.open();
        isPlayerValid = true;
        
        // 영상 재생 Thread 시작
        thread = new Thread(this, "Video Player");
        thread.start();
        
        // 처음부터 시작
        playFromStart();
    }




    // ---------------------------------------------------------------
    // public methods.
    //

    /**
     * Play 버튼 클릭 시 호출 -> 재생 상태로 변경
     */
    public void play() {
        if(playing) {
            return;
        }

        synchronized (pauseLock) {
            playing = true;
            pauseLock.notifyAll();
        }
    }

    /**
     * Pause 버튼 클릭 시 호출 -> 일시중지 상태로 변경
     */
    public void pause() {
        if(!playing) {
            return;
        }

        synchronized (pauseLock) {
            playing = false;
        }
    }

    /**
     * 재생 지점 변경
     * @param position
     */
    public void seekTo(long position) {
        synchronized (seekLock) {
            seekTimeMs = position;
        }
    }

    /**
     * 특정 시간 만큼 앞으로 이동
     * @param unitTimeMs : 앞으로 이동할 시간
     */
    public void previous(int unitTimeMs) {
        synchronized (seekLock) {
            seekTimeMs = curTimeMs - unitTimeMs;
        }
    }

    /**
     * 특정 시간 만큼 뒤로 이동
     * @param unitTimeMs : 뒤로 이동할 시간
     */
    public void next(int unitTimeMs) {
        synchronized (seekLock) {
            seekTimeMs = curTimeMs + unitTimeMs;
        }
    }

    /**
     * release resources.
     */
    public void release() {
        isPlayerValid = false;
        thread.interrupt();
        player.close();
    }
    
    
    // ---------------------------------------------------------------
    // implements Runnable
    //

    @Override
    public void run() {
        curTimeMs = 0;
        long startTimeMs = System.currentTimeMillis();

        while(isPlayerValid) {
            
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
            
            // 파일 -> inputBuffer 이동
            if(!inEOS) {
                inEOS = player.inputBuffer();
            }
            
            // outputBuffer -> 출력
            if(!outEOS) {
                // 코덱 상태 -> 출력 가능한 상태인지 확인
                int codecStatus = player.getCodecStatus();
                
                if(codecStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // INFO_TRY_AGAIN_LATER
                }

                else if(codecStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // INFO_OUTPUT_FORMAT_CHANGED
                }

                else if(codecStatus < 0) {
                    // UNEXPECTED RESULTS
                }

                else { // 출력 가능한 상태

                    curTimeMs = player.getPresentationTimeMs(); // 현재 재생 위치

                    // 영상 시간 & 실제 시간 동기화 작업
                    while(curTimeMs > (System.currentTimeMillis() - startTimeMs)) {

                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

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
            
            // 끝까지 재생 완료된 경우 -> 처음으로
            if(inEOS && outEOS) {
                playFromStart();
            }
        }
    }

    // ---------------------------------------------------------------
    // internal methods.
    //

    private void playFromStart() {
        inEOS = false;
        outEOS = false;
        pause();
        seekTo(0);
    }

    private void initLocks() {
        // Lock 초기화
        pauseLock = new Object();
        seekLock = new Object();
    }

    private boolean checkPause() {
        boolean paused = false;

        synchronized (pauseLock) {
            if(!playing) {
                paused = true;

                while(!playing && !isPlayerValid == false) {
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
}
