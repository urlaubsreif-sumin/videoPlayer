package videoplayer2;

import android.content.res.AssetFileDescriptor;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.videoplayer.R;
import com.example.videoplayer.databinding.ActivityMainBinding;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

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
        
        // View Binding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        // FrameCallBack 세팅: Frame이 렌더링 될 때 마다 SeekBar 값 조정
        setFrameCallBack();
    
        // TextureView 세팅
        initTextureView();

        // SeekBar Listner 세팅
        binding.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                
                if(touch) { // 사용자의 직접적인 터치에 의해 조정될 경우 -> 영상 재생 지점 이동
                    playTask.seekTo(progress * 1000);
                }
                
                // 현재 재생 지점에 맞게 TextView 세팅
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
    // OnClickListner
    //

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.playBtn:
                playTask.play();
                break;

            case R.id.pauseBtn:
                playTask.pause();
                break;

            case R.id.nextBtn:
                playTask.next(5000); // +5s 이동
                break;
                
            case R.id.previousBtn:
                playTask.previous(5000); // -5s 이동
                break;
        }
    }

    // ---------------------------------------------------------------
    // internal methods.
    //

    private void initTextureView() {
        // textureView Listener 세팅
        binding.textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {

            // TextureView 준비 완료 -> PlayTask, VideoPlayer 초기화
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                try {
                    // 영상 파일
                    AssetFileDescriptor videoFile = getResources().openRawResourceFd(R.raw.video);

                    // VideoPlayer 초기화
                    videoPlayer = new VideoPlayer(videoFile, new Surface(surface), frameCallBack);

                    // PlayTask 초기화
                    playTask = new PlayTask(videoPlayer);

                    // 영상 길이에 맞게 View 업데이트
                    int duration = videoPlayer.getVideoDuration() / 1000;
                    setDurationOnView(duration);

                    // play/pause/next/previous 버튼 ClickListener 세팅
                    setOnClickListener();

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
    }


    /**
     * Frame이 렌더링 될 때 호출되는 콜백 메서드
     */
    private void setFrameCallBack() {
        frameCallBack = new VideoPlayer.FrameCallBack() {
            @Override
            public void postRender(long presentationTimeUsec) {
                // seekbar 조정
                binding.seekBar.setProgress((int) (presentationTimeUsec / 1000));
            }
        };
    }

    /**
     * Play / Pause / Next / Previous 버튼 ClickListener 설정
     */
    private void setOnClickListener() {
        binding.playBtn.setOnClickListener(this);
        binding.pauseBtn.setOnClickListener(this);
        binding.nextBtn.setOnClickListener(this);
        binding.previousBtn.setOnClickListener(this);
    }

    /**
     * 영상 전체 길이에 맞게 View 업데이트
     *  - SeekBar Max값 조정
     *  - 전체 영상 길이를 나타내는 TextView 값 변경
     * @param duration
     */
    private void setDurationOnView(int duration) {
        binding.totalTime.setText(String.valueOf(duration) + "s");
        binding.seekBar.setMax(duration);
    }


}
