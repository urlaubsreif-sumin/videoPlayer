package com.example.videoplayer;

import java.io.IOException;

public class PlayTask implements Runnable {
    
    private VideoPlayer player;
    private Thread thread; // 영상 재생 Thread

    private boolean inEOS = false; // 영상 파일 -> inputBuffer 이동 완료 여부
    private boolean outEOS = false; // outputBuffer -> 출력 완료 여부

    private Object pauseLock;
    private Object seekLock;

    boolean isPlayerValid; // Player 유효
    boolean playing; // 재생 상태 (재생 or 일시중지)
    
    private long seekTimeMs; // 목표 재생 지점
    private long curTimeMs; // 현재 재생 지점

    

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
        moveToStart();
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
            // 재생 상태로 변경
            playing = true;
            
            // pauseLock 해제
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
            // 일시 중지 상태로 변경
            playing = false;
        }
    }

    /**
     * 재생 지점 변경
     * @param position
     */
    public void seekTo(long position) {
        synchronized (seekLock) {
            // 목표 재생 지점 설정
            seekTimeMs = position;
        }
    }

    /**
     * 특정 시간 만큼 앞으로 이동
     * @param unitTimeMs : 앞으로 이동할 시간
     */
    public void previous(int unitTimeMs) {
        synchronized (seekLock) {
            seekTimeMs = Math.max(0, curTimeMs - unitTimeMs); // 목표 재생 지점은 반드시 0이상
        }
    }

    /**
     * 특정 시간 만큼 뒤로 이동
     * @param unitTimeMs : 뒤로 이동할 시간
     */
    public void next(int unitTimeMs) {
        synchronized (seekLock) {
            seekTimeMs = Math.min(player.getVideoDuration(), curTimeMs + unitTimeMs); // 목표 재생 지점은 반드시 영상 길이 이하
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
        long startSystemTimeMs = System.currentTimeMillis(); // 실제 시작 시간

        while(isPlayerValid) {
            
            // Pause 상태이면 대기
            if(checkPause()) {
                // 재생 시작 시간 업데이트
                startSystemTimeMs = System.currentTimeMillis() - curTimeMs;
            }
            
            // 목표 재생 지점까지 이동
            synchronized (seekLock) {
                if(seekTimeMs != -1) {
                    player.seekTo(seekTimeMs);

                    curTimeMs = seekTimeMs;

                    startSystemTimeMs = System.currentTimeMillis() - curTimeMs;
                    seekTimeMs = -1;
                }
            }
            
            // 파일 -> inputBuffer 저장
            if(!inEOS) {
                inEOS = player.bufferToInputBuffer();
            }
            
            // outputBuffer -> 렌더링
            if(!outEOS) {
                
                int codecStatus = player.getCodecStatus();
                
                // 렌더링 가능한 상태이면
                if(codecStatus >= 0) {
                    curTimeMs = player.getPresentationTimeMs(); // 현재 재생 위치

                    // 재생 시간 & 실제 시간 동기화 작업
                    while(curTimeMs > (System.currentTimeMillis() - startSystemTimeMs)) {
                        try {
                            Thread.sleep(10); // 영상 재생 시간이 실제 흐른 시간 보다 긴 경우, 실제 시간이 재생 시간을 따라잡을 때까지 대기
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
                    
                    // 영상 렌더링
                    outEOS = player.renderFromOutputBuffer(codecStatus);
                }
            }
            
            // 끝까지 재생 완료된 경우 -> 처음으로
            if(inEOS && outEOS) {
                moveToStart();
            }
        }
    }

    // ---------------------------------------------------------------
    // internal methods.
    //

    /**
     * 첫 시작 지점으로 이동
     */
    private void moveToStart() {
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

    
    /**
     * Pause 상태가 해제될 때 까지 대기
     * @return
     */
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
