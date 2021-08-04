package com.example.videoplayer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.res.AssetFileDescriptor;
import android.graphics.SurfaceTexture;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    TextureView textureView;
    ImageButton playBtn;
    ImageButton pauseBtn;
    ImageButton previousBtn;
    ImageButton nextBtn;
    SeekBar seekBar;

    VideoPlayer videoPlayer;
    VideoPlayer.PlayerFeedback feedback;
    PlayTask playTask;



    @RequiresApi(24)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textureView = (TextureView) findViewById(R.id.textureView);
        playBtn = (ImageButton) findViewById(R.id.playBtn);
        pauseBtn = (ImageButton) findViewById(R.id.pauseBtn);
        previousBtn = (ImageButton) findViewById(R.id.previousBtn);
        nextBtn = (ImageButton) findViewById(R.id.nextBtn);
        seekBar = (SeekBar) findViewById(R.id.seekBar);


        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                AssetFileDescriptor video = getResources().openRawResourceFd(R.raw.video);
                VideoPlayer.FrameCallBack frameCallBack = new VideoPlayer.FrameCallBack() {
                    @Override
                    public void preRender(long presentationTimeUsec) {
                        seekBar.setProgress((int) (presentationTimeUsec / 10000));
                    }

                    @Override
                    public void postRender() {

                    }
                };

                videoPlayer = new VideoPlayer(video, new Surface(surface), frameCallBack);
                feedback = new VideoPlayer.PlayerFeedback() {
                    @Override
                    public void playbackStopped() {

                    }
                };
                playTask = new PlayTask(videoPlayer, feedback);

                seekBar.setMax((int) (videoPlayer.getVideoDuration() / 10000));

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
                playTask.execute();
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
                playTask.movePrevious();
            }
        });

        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playTask.moveNext();
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                playTask.seekTo(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }
}