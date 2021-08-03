package com.example.videoplayer;

import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoThread extends Thread {
    private static final int TIMEOUT_USEC = 100000;
    Surface surface;
    AssetFileDescriptor video;

    VideoThread(Surface surface, AssetFileDescriptor video) {
        this.surface = surface;
        this.video = video;
    }

    @Override
    public void run() {
        super.run();
        MediaExtractor extractor = new MediaExtractor();
        MediaCodec decoder = null;

        try {
            extractor.setDataSource(video);
            Log.d("테스트", extractor.getTrackCount() + "");
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat format = extractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                extractor.selectTrack(i);
                decoder = MediaCodec.createDecoderByType(mime);
                decoder.configure(format, surface, null, 0);
                break;
            }

            if (decoder == null) {
                return;
            }

            decoder.start();

            doExtract(extractor, decoder);

            decoder.stop();
            decoder.release();
            extractor.release();

        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private void doExtract(MediaExtractor extractor, MediaCodec decoder) {
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        
        boolean inEos = false; // 전체 인코딩 여부
        boolean outEos = false; // 전체 디코딩 여부
        
        while (!outEos) { // 전체 영상을 디코딩 할 때 까지 [부분 인코딩 -> 부분 디코딩] 반복
            if (!inEos) {
                int inputBufIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC);
                if (inputBufIndex >= 0) {
                    ByteBuffer inputBuf = decoder.getInputBuffer(inputBufIndex);

                    int chunkSize = extractor.readSampleData(inputBuf, 0);
                    if (chunkSize < 0) { // 더 이상 인코딩 할 것이 없음 (EOS)
                        decoder.queueInputBuffer(inputBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        inEos = true;
                    } else {
                        long presentationTimeUs = extractor.getSampleTime();
                        decoder.queueInputBuffer(inputBufIndex, 0, chunkSize, presentationTimeUs, 0);
                        extractor.advance();
                    }
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
                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        decoder.releaseOutputBuffer(decoderStatus, false);
                        outEos = true;
                    } else {
                        decoder.releaseOutputBuffer(decoderStatus, true);
                    }
                }
            }
        }
    }
}