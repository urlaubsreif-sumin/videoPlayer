package com.example.videoplayer;

import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

public class AudioPlayer {

    // ---------------------------------------------------------------
    // Constants.
    //

    private final int TIMEOUT_USEC = 1000;


    // ---------------------------------------------------------------
    // variables.
    //

    private AssetFileDescriptor videoFile; // 영상 파일
    private Surface outputSurface; // 출력 Surface

    private MediaExtractor extractor;
    private MediaCodec decoder;
    private MediaCodec.BufferInfo bufferInfo;

    // ---------------------------------------------------------------
    // interface.
    //

    private AudioPlayer.FrameCallBack frameCallBack;

    public interface FrameCallBack {
        // 부분 렌더링 직후 호출되는 콜백 메서드 -> ui 업데이트
        void postRender(long presentationTimeUsec);
    }



    AudioPlayer(AssetFileDescriptor videoFile, Surface outputSurface, AudioPlayer.FrameCallBack frameCallBack) {
        this.videoFile = videoFile;
        this.outputSurface = outputSurface;
        this.frameCallBack = frameCallBack;
    }


    // ---------------------------------------------------------------
    // public
    //

    /**
     * Player 시작
     * @throws IOException
     */
    public void open() throws IOException {
        extractor = new MediaExtractor();
        extractor.setDataSource(videoFile);
        bufferInfo = new MediaCodec.BufferInfo();

        // 첫번째 트랙 디코딩 설정
        if(!initWithFirstTrack()) {
            throw new IOException("can't open audio file.");
        }

        // 디코딩 시작
        decoder.start();
    }


    /**
     * release resources.
     */
    public void close() {
        decoder.stop();
        decoder.release();
        decoder = null;

        extractor.release();
        extractor = null;
    }


    /**
     * videoFile을 sampleSize 만큼 읽어서 inputBuffer에 저장
     * @return videoFile 끝까지 읽은 경우 -> true
     */
    public boolean bufferToInputBuffer() {
        int inputBufIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC);

        // 읽을 수 있는 상태인 경우
        if(inputBufIndex >= 0) {
            ByteBuffer inputBuf = decoder.getInputBuffer(inputBufIndex);
            int sampleSize = extractor.readSampleData(inputBuf, 0);

            // 영상 끝까지 모두 읽은 경우
            if(sampleSize < 0) {
                decoder.queueInputBuffer(inputBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                return true;
            }

            // 읽을 영상이 남은 경우
            else {
                decoder.queueInputBuffer(inputBufIndex, 0, sampleSize, extractor.getSampleTime(), 0);
                extractor.advance();
            }
        }
        return false;
    }


    /**
     * outputBuffer 읽어서 Surface에 렌더링
     * @param outputBufIndex
     * @return
     */
    public boolean renderFromOutputBuffer(int outputBufIndex) {
        long curPresentationTimeMs = bufferInfo.presentationTimeUs / 1000;

        // 영상 출력
        decoder.releaseOutputBuffer(outputBufIndex, true);

        // 영상 렌더링 직후 -> ui 업데이트(SeekBar, TextView)를 위한 콜백 메서드 호출
        if(frameCallBack != null) {
            frameCallBack.postRender(curPresentationTimeMs);
        }

        // 영상 끝까지 출력 완료한 경우
        if((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            return true;
        }

        return false;
    }


    /**
     * 목표 재생 지점 탐색
     * @param seekTimeMs
     */
    public void seekTo(long seekTimeMs) {
        // 목표 지점까지 탐색
        extractor.seekTo(seekTimeMs * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC);

        // flush input&output Buffer
        decoder.flush();
    }

    public long getPresentationTimeMs() {
        return bufferInfo.presentationTimeUs / 1000;
    }

    public int getCodecStatus() {
        return decoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
    }


    // ---------------------------------------------------------------
    // internal methods.
    //

    /**
     * 재생 가능한 첫 번째 Track으로 이동
     * @return
     * @throws IOException
     */
    private boolean initWithFirstTrack() throws IOException {
        int numTracks = extractor.getTrackCount();

        for(int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);

            if(mime.startsWith("audio/")) {
                // 실행 가능한 첫 번째 Track 선택
                extractor.selectTrack(i);

                // decoder 생성
                decoder = MediaCodec.createDecoderByType(mime);
                decoder.configure(format, outputSurface, null, 0);

                return true;
            }
        }
        return false;
    }



}
