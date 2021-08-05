package videoplayer2;

import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.SystemClock;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoPlayer {
    private AssetFileDescriptor videoFile;
    private Surface outputSurface;
    private FrameCallBack frameCallBack;

    private MediaExtractor extractor;
    private MediaCodec codec;
    private MediaCodec.BufferInfo bufferInfo;

    private int video_duration;
    private int video_height;
    private int video_weight;

    private final int TIMEOUT_USEC = 1000;



    // ---------------------------------------------------------------
    // interface.
    //

    public interface FrameCallBack {
        // 부분 렌더링 이후 호출 -> seekBar 조정
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
    //
    //

    public void open() throws IOException {
        extractor = new MediaExtractor();
        extractor.setDataSource(videoFile);
        bufferInfo = new MediaCodec.BufferInfo();
        
        // 첫번째 트랙부터 codec 시작
        if(!initWithFirstTrack()) {
            throw new IOException("can't open video file.");
        }

        codec.start();
    }

    public void close() {
        codec.stop();
        codec.release();
        codec = null;

        extractor.release();
        extractor = null;
    }

    /**
     * videoFile 읽어서 inputBuffer에 저장
     * @return videoFile 끝까지 읽은 경우 -> true
     */
    public boolean inputBuffer() {
        int inputBufIndex = codec.dequeueInputBuffer(TIMEOUT_USEC);

        if(inputBufIndex >= 0) {
            ByteBuffer inputBuf = codec.getInputBuffer(inputBufIndex);
            int sampleSize = extractor.readSampleData(inputBuf, 0);
            Log.d("테스트", "sample size: " + sampleSize);
            // 영상 끝까지 모두 읽은 경우
            if(sampleSize < 0) {
                codec.queueInputBuffer(inputBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                return true;
            }

            // 읽을 영상이 남은 경우
            else {
                codec.queueInputBuffer(inputBufIndex, 0, sampleSize, extractor.getSampleTime(), 0);
                extractor.advance();
            }
        }
        return false;
    }

    public boolean outputBuffer(int codecStatus) {
        long curPresentationTimeMs = bufferInfo.presentationTimeUs / 1000;


        // 영상 출력
        Log.d("테스트", "codec status: " + codecStatus);
        codec.releaseOutputBuffer(codecStatus, true);

        // 현재 재생 시간에 맞게 SeekBar 조정하기 위한 콜백
        if(frameCallBack != null) {
            frameCallBack.postRender(curPresentationTimeMs);
        }

        // 영상 끝까지 출력한 경우
        if((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            return true;
        }

        return false;
    }

    public void seekTo(long seekTimeMs) {
        extractor.seekTo(seekTimeMs * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        codec.flush();
    }

    public long getPresentationTimeMs() {
        return bufferInfo.presentationTimeUs / 1000;
    }

    public int getCodecStatus() {
        return codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
    }

    public int getVideoDuration() {
        return video_duration;
    }

    // ---------------------------------------------------------------
    //
    //

    private boolean initWithFirstTrack() throws IOException {
        int numTracks = extractor.getTrackCount();

        for(int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);

            if(mime.startsWith("video/")) {
                extractor.selectTrack(i);

                codec = MediaCodec.createDecoderByType(mime);
                codec.configure(format, outputSurface, null, 0);

                video_duration = (int) (format.getLong(MediaFormat.KEY_DURATION) / 1000);

                return true;
            }
        }

        return false;
    }



}
