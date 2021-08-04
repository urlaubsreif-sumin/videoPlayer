package videoplayer2;

import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.view.Surface;

public class VideoPlayer {
    private MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

    private AssetFileDescriptor videoFile;
    private Surface outputSurface;
    private FrameCallBack frameCallBack;

    private MediaExtractor extractor;

    public interface PlayerFeedback {
        void playbackPaused();
        void playbackResumed();
        void playbackPositionChanged();
    }

    public interface FrameCallBack {
        // 부분 렌더링 이후 호출 -> seekBar 조정
        void postRender(long presentationTimeUsec);
    }

    VideoPlayer(Surface outputSurface, FrameCallBack frameCallBack) {
        this.outputSurface = outputSurface;
        this.frameCallBack = frameCallBack;



    }


}
