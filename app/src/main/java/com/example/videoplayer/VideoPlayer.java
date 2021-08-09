package com.example.videoplayer;

import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoPlayer {
    // ---------------------------------------------------------------
    // Constants.
    //

    private final int TIMEOUT_USEC = 1000;


    // ---------------------------------------------------------------
    // variables.
    //

    private AssetFileDescriptor videoFile; // 영상 파일
    private Surface outputSurface; // 출력 Surface

    private MediaExtractor videoExtractor;
    private MediaExtractor audioExtractor;
    private MediaCodec videoDecoder;
    private MediaCodec audioDecoder;
    private MediaCodec.BufferInfo videoBufferInfo;
    private MediaCodec.BufferInfo audioBufferInfo;

    private int video_duration; // 영상 길이





    // ---------------------------------------------------------------
    // interface.
    //

    private FrameCallBack frameCallBack;

    public interface FrameCallBack {
        // 부분 렌더링 직후 호출되는 콜백 메서드 -> ui 업데이트
        void postRender(long presentationTimeUsec);
    }



    // ---------------------------------------------------------------
    // Constructor
    //

    VideoPlayer(AssetFileDescriptor videoFile, Surface outputSurface, FrameCallBack frameCallBack) {
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
        videoExtractor = new MediaExtractor();
        videoExtractor.setDataSource(videoFile);

        audioExtractor = new MediaExtractor();
        audioExtractor.setDataSource(videoFile);

        videoBufferInfo = new MediaCodec.BufferInfo();
        audioBufferInfo = new MediaCodec.BufferInfo();
        
        // 첫번째 트랙 디코딩 설정
        if(!initWithFirstVideoTrack()) {
            throw new IOException("can't open video file.");
        }

        if(!initWithFirstAudioTrack()) {
            throw new IOException("can't open audio file.");
        }
        
        // 디코딩 시작
        videoDecoder.start();
    }


    /**
     * release resources.
     */
    public void close() {
        videoDecoder.stop();
        videoDecoder.release();
        videoDecoder = null;

        videoExtractor.release();
        videoExtractor = null;

        audioExtractor.release();
        audioExtractor = null;
    }


    /**
     * videoFile을 sampleSize 만큼 읽어서 inputBuffer에 저장
     * @return videoFile 끝까지 읽은 경우 -> true
     */
    public boolean bufferVideoToInputBuffer() {
        int inputVideoBufIndex = videoDecoder.dequeueInputBuffer(TIMEOUT_USEC);


        // 읽을 수 있는 상태인 경우
        if(inputVideoBufIndex >= 0) {
            ByteBuffer inputBuf = videoDecoder.getInputBuffer(inputVideoBufIndex);
            int sampleSize = videoExtractor.readSampleData(inputBuf, 0);

            // 영상 끝까지 모두 읽은 경우
            if(sampleSize < 0) {
                videoDecoder.queueInputBuffer(inputVideoBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                return true;
            }

            // 읽을 영상이 남은 경우
            else {
                videoDecoder.queueInputBuffer(inputVideoBufIndex, 0, sampleSize, videoExtractor.getSampleTime(), 0);
                videoExtractor.advance();
            }
        }

        return false;
    }

    public boolean bufferAudioToInputBuffer() {
        int inputAudioBufIndex = audioDecoder.dequeueInputBuffer(TIMEOUT_USEC);
        if(inputAudioBufIndex >= 0) {
            ByteBuffer inputAudioBuf = audioDecoder.getInputBuffer(inputAudioBufIndex);
            int sampleSize = audioExtractor.readSampleData(inputAudioBuf, 0);

            if(sampleSize < 0) {
                audioDecoder.queueInputBuffer(inputAudioBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                return true;
            }

            else {
                audioDecoder.queueInputBuffer(inputAudioBufIndex, 0, sampleSize, audioExtractor.getSampleTime(), 0);
                audioExtractor.advance();
            }
        }

        return false;
    }


    /**
     * outputBuffer 읽어서 Surface에 렌더링
     * @param outputVideoBufIndex
     * @return
     */
    public boolean renderVideoFromOutputBuffer(int outputVideoBufIndex) {
        long curPresentationTimeMs = videoBufferInfo.presentationTimeUs / 1000;

        // 영상 출력
        videoDecoder.releaseOutputBuffer(outputVideoBufIndex, true);
        Log.d("테스트", "release!");

        // 영상 렌더링 직후 -> ui 업데이트(SeekBar, TextView)를 위한 콜백 메서드 호출
        if(frameCallBack != null) {
            frameCallBack.postRender(curPresentationTimeMs);
        }

        // 영상 끝까지 출력 완료한 경우
        if((videoBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            return true;
        }

        return false;
    }

    public boolean renderAudioFromOutputBuffer(int outputAudioBufIndex) {
        long curPresentationTimeMs = audioBufferInfo.presentationTimeUs / 1000;

        audioDecoder.releaseOutputBuffer(outputAudioBufIndex, true);

        if((audioBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
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
        videoExtractor.seekTo(seekTimeMs * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        audioExtractor.seekTo(seekTimeMs * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC);



        // flush input&output Buffer
        videoDecoder.flush();
        audioDecoder.flush();


    }


    public long getPresentationVideoTimeMs() {
        return videoBufferInfo.presentationTimeUs / 1000;
    }

    public long getPresentationAudioTimeMs() {
        return audioBufferInfo.presentationTimeUs / 1000;
    }

    public int getVideoCodecStatus() {
        return videoDecoder.dequeueOutputBuffer(videoBufferInfo, TIMEOUT_USEC);
    }

    public int getAudioCodecStatus() {
        return audioDecoder.dequeueOutputBuffer(audioBufferInfo, TIMEOUT_USEC);
    }

    public int getVideoDuration() {
        return video_duration;
    }


    // ---------------------------------------------------------------
    // internal methods.
    //

    /**
     * 재생 가능한 첫 번째 Track으로 이동
     * @return
     * @throws IOException
     */
    private boolean initWithFirstVideoTrack() throws IOException {
        int numTracks = videoExtractor.getTrackCount();
        Log.d("테스트", "track: " + numTracks);

        for(int i = 0; i < numTracks; i++) {
            MediaFormat format = videoExtractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);

            if(mime.startsWith("video/")) {
                // 실행 가능한 첫 번째 Track 선택
                videoExtractor.selectTrack(i);

                // decoder 생성
                videoDecoder = MediaCodec.createDecoderByType(mime);
                videoDecoder.configure(format, outputSurface, null, 0);
                
                // 영상 길이 변수 세팅
                video_duration = (int) (format.getLong(MediaFormat.KEY_DURATION) / 1000);

                return true;
            }
        }
        return true;
    }

    private boolean initWithFirstAudioTrack() throws IOException {
        int numTracks = audioExtractor.getTrackCount();

        for(int i = 0; i < numTracks; i++) {
            MediaFormat format = audioExtractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);

            if(mime.startsWith("audio/")) {
                audioExtractor.selectTrack(i);

                audioDecoder = MediaCodec.createDecoderByType(mime);
                audioDecoder.configure(format, null, null, 0);

                return true;
            }
        }
        return false;
    }


}
