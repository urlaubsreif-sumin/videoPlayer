package com.example.videoplayer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {

    @RequiresApi(24)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextureView textureView = (TextureView) findViewById(R.id.textureView);


        //-------------------------------------------------------------

        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                createVideoThread(new Surface(surface));
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

    private void createVideoThread(Surface surface) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                MediaExtractor extractor = new MediaExtractor();
                MediaCodec decoder = null;

                try {
                    extractor.setDataSource(getResources().openRawResourceFd(R.raw.video));
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
        });

        thread.start();
    }

    private void doExtract(MediaExtractor extractor, MediaCodec decoder) {
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

        boolean inEos = false;
        boolean outEos = false;

        while(!outEos) {
            if(!inEos) {

            }
        }
    }
}