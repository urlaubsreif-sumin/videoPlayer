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
    private Surface surface;
    private AssetFileDescriptor video;
    private FrameCallBack frameCallBack;

    private int videoWidth;
    private int videoHeight;
    private long videoDuration;

    private boolean isStopRequested = true;


    public interface PlayerFeedback {
        void playbackStopped();
    }

    public interface FrameCallBack {
        // frame이 렌더링 되기 직전에 호출
        void preRender(long presentationTimeUsec);

        void postRender();
    }

    public VideoPlayer(AssetFileDescriptor video, Surface surface, FrameCallBack frameCallBack) {
        this.video = video;
        this.surface = surface;
        this.frameCallBack = frameCallBack;

        MediaExtractor extractor = null;
        try{
            extractor = new MediaExtractor();
            extractor.setDataSource(video);
            int trackIndex = selectTrack(extractor);
            if(trackIndex < 0) {
                throw new RuntimeException("No video track found.");
            }
            extractor.selectTrack(trackIndex);

            MediaFormat format = extractor.getTrackFormat(trackIndex);
            videoWidth = format.getInteger(MediaFormat.KEY_WIDTH);
            videoHeight = format.getInteger(MediaFormat.KEY_HEIGHT);
            videoDuration = format.getLong(MediaFormat.KEY_DURATION);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(extractor != null) {
                extractor.release();
            }
        }
    }

    public int getVideoWidth() {
        return videoWidth;
    }

    public int getVideoHeight() {
        return videoHeight;
    }

    public long getVideoDuration() {
        return videoDuration;
    }

    public void pause() {
        isStopRequested = true;
    }

    public void play() {
        MediaExtractor extractor = null;
        MediaCodec decoder = null;

        if(isStopRequested) {
           isStopRequested = false;
        } else {
            return;
        }

        try {
            extractor = new MediaExtractor();
            extractor.setDataSource(video);
            int trackIndex = selectTrack(extractor);
            if(trackIndex < 0) {
                throw new RuntimeException("No video track found.");
            }
            extractor.selectTrack(trackIndex);

            MediaFormat format = extractor.getTrackFormat(trackIndex);

            String mime = format.getString(MediaFormat.KEY_MIME);
            decoder = MediaCodec.createDecoderByType(mime);
            decoder.configure(format, surface, null, 0);

            decoder.start();
            doExtract(extractor, trackIndex, decoder, frameCallBack);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(decoder != null) {
                decoder.stop();
                decoder.release();
                decoder = null;
            }
            if(extractor != null) {
                extractor.release();
                extractor = null;
            }
        }
    }

    private static int selectTrack(MediaExtractor extractor) {
        int numTracks = extractor.getTrackCount();
        for(int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if(mime.startsWith("video/")) {
                return i;
            }
        }
        return -1;
    }

    private void doExtract(MediaExtractor extractor, int trackIndex, MediaCodec decoder, FrameCallBack frameCallBack) {
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        final int TIMEOUT_USEC = 1000;
        long firstInputTimeNsec = -1;
        int inputChunk = 0;

        boolean inEos = false; // 전체 인코딩 여부
        boolean outEos = false; // 전체 디코딩 여부

        while (!outEos) { // 전체 영상을 디코딩 할 때 까지 [부분 인코딩 -> 부분 디코딩] 반복
            if(isStopRequested) {
                return;
            }
            if (!inEos) {
                int inputBufIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC);
                if (inputBufIndex >= 0) {
                    if(firstInputTimeNsec == -1) {
                        firstInputTimeNsec = System.nanoTime();
                    }
                    ByteBuffer inputBuf = decoder.getInputBuffer(inputBufIndex);

                    int chunkSize = extractor.readSampleData(inputBuf, 0);
                    Log.d("테스트", "chunk size: " + chunkSize);
                    if (chunkSize < 0) { // 더 이상 인코딩 할 것이 없음 (EOS)
                        decoder.queueInputBuffer(inputBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        inEos = true;
                    } else {
                        long presentationTimeUs = extractor.getSampleTime();
                        decoder.queueInputBuffer(inputBufIndex, 0, chunkSize, presentationTimeUs, 0);
                        inputChunk++;
                        extractor.advance();
                    }
                } else {
                    //input buffer not available
                }
            }

            if (!outEos) {
                int decoderStatus = decoder.dequeueOutputBuffer(info, TIMEOUT_USEC);

                if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet;

                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat newFormat = decoder.getOutputFormat();

                } else if (decoderStatus < 0) {
                    throw new RuntimeException("unexpected result from decoder.dequeOutputBuffer");

                } else {
                    if(firstInputTimeNsec != 0) {
                        long nowNsec = System.nanoTime();
                        firstInputTimeNsec = 0;
                    }
                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        outEos = true;
                        isStopRequested = true;
                    }

                    boolean doRender = (info.size != 0);
                    if(doRender && frameCallBack != null) {
                        frameCallBack.preRender(info.presentationTimeUs);
                    }
                    decoder.releaseOutputBuffer(decoderStatus, doRender);
                    if(doRender && frameCallBack != null) {
                        frameCallBack.postRender();
                    }
                }
            }
        }
    }
}
