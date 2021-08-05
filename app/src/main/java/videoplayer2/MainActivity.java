package videoplayer2;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.videoplayer.R;
import com.example.videoplayer.databinding.ActivityMainBinding;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    VideoPlayer.FrameCallBack frameCallBack;
    PlayTask playTask;
    VideoPlayer videoPlayer;

    boolean touch;

    // ---------------------------------------------------------------
    // lifecycles
    //

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        setFrameCallBack();

        binding.textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                AssetFileDescriptor videoFile = getResources().openRawResourceFd(R.raw.video);
                videoPlayer = new VideoPlayer(videoFile, new Surface(surface), frameCallBack);

                try {
                    playTask = new PlayTask(videoPlayer);
                    int duration = videoPlayer.getVideoDuration() / 1000;
                    binding.totalTime.setText(String.valueOf(duration) + "s");
                    binding.seekBar.setMax(duration);
                } catch (IOException e) {
                    e.printStackTrace();
                }
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

        binding.playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playTask.play();

            }
        });

        binding.pauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playTask.pause();
            }
        });

        binding.previousBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playTask.previous(5000);
            }
        });

        binding.nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playTask.next(5000);
            }
        });

        binding.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                if(touch) {
                    playTask.seekTo(progress * 1000);
                    Log.d("테스트", String.valueOf(progress * 1000));
                }
                binding.curTime.setText((progress) + "s");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                touch = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                touch = false;
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        playTask.release();
    }

    // ---------------------------------------------------------------
    //
    //

    private void setFrameCallBack() {
        frameCallBack = new VideoPlayer.FrameCallBack() {
            @Override
            public void postRender(long presentationTimeUsec) {
                //seekbar 조정
                binding.seekBar.setProgress((int) (presentationTimeUsec / 1000));
            }
        };
    }
}
