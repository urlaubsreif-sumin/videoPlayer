package videoplayer2;

import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.videoplayer.R;

public class MainActivity extends AppCompatActivity {
    TextureView textureView;
    ImageButton playBtn;
    ImageButton pauseBtn;
    ImageButton previousBtn;
    ImageButton nextBtn;
    SeekBar seekBar;

    VideoPlayer.FrameCallBack frameCallBack;
    PlayTask playTask;
    VideoPlayer.PlayerFeedback feedback;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init_views();

        setFeedback();

        setFrameCallBack();

        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                VideoPlayer videoPlayer = new VideoPlayer(new Surface(surface), frameCallBack);
                playTask = new PlayTask(videoPlayer, feedback);
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

            }
        });

        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playTask.play();
            }
        });

        pauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playTask.pause();
            }
        });

        previousBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playTask.previous(5000);
            }
        });

        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playTask.next(5000);
            }
        });

    }

    public void init_views() {
        textureView = (TextureView) findViewById(R.id.textureView);
        playBtn = (ImageButton) findViewById(R.id.playBtn);
        pauseBtn = (ImageButton) findViewById(R.id.pauseBtn);
        previousBtn = (ImageButton) findViewById(R.id.previousBtn);
        nextBtn = (ImageButton) findViewById(R.id.nextBtn);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
    }

    private void setFeedback() {
        feedback = new VideoPlayer.PlayerFeedback() {
            @Override
            public void playbackPaused() {

            }

            @Override
            public void playbackResumed() {

            }

            @Override
            public void playbackPositionChanged() {

            }
        };
    }

    private void setFrameCallBack() {
        frameCallBack = new VideoPlayer.FrameCallBack() {
            @Override
            public void postRender(long presentationTimeUsec) {
                //seekbar 조정
                seekBar.setProgress((int) (presentationTimeUsec / 1000));
            }
        };
    }
}
