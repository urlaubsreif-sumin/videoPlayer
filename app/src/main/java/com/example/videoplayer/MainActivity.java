package com.example.videoplayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
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

public class MainActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {
    private static final String LOG_TAG = "player";
    MediaSessionCompat mediaSession;
    MediaControllerCompat mediaController;
    PlaybackStateCompat.Builder statebuilder;
    MediaPlayer mediaPlayer;

    private boolean mSurfaceTextureReady = false;

    // -----------------------------------------------------------------------
    // lifecycles
    //

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setMediaSession();

        setStatebuilder();

        setMediaController();

        setMediaPlayer();

        ImageButton play = (ImageButton) findViewById(R.id.playBtn);

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextureView textureView = (TextureView) findViewById(R.id.textureView);
                SurfaceTexture st = textureView.getSurfaceTexture();
                Surface surface = new Surface(st);
                mediaPlayer.setSurface(surface);
            }
        });

        TextureView textureView = (TextureView) findViewById(R.id.textureView);
        textureView.setSurfaceTextureListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mediaPlayer.release();
        mediaPlayer = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediaSession.release();
    }




    // -----------------------------------------------------------------------
    // SurfaceTexture lifecycles
    //

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
        mSurfaceTextureReady = true;
        updateControls();
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
        //ignore
    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
        mSurfaceTextureReady = false;
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
        //ignore
    }

    private void updateControls() {
        ImageButton playBtn = (ImageButton) findViewById(R.id.playBtn);
        playBtn.setEnabled(mSurfaceTextureReady);
    }


    //--------------------------------------------------------------------------
    //
    //

    public void setMediaSession() {
        // Create a MediaSessionCompat
        mediaSession = new MediaSessionCompat(this, LOG_TAG);

        // Enable callbacks from MediaButtons and TransportControls
        // All media sessions are expected to handle media button events / trasport controls now -> deprecated
        mediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        // Do not let MediaButtons restart the player when the  app is not visible
        mediaSession.setMediaButtonReceiver(null);

        // MySessionCallback has methods that handle callbacks from a media controller
        mediaSession.setCallback(new MySessionCallback());
        mediaSession.setActive(true);
    }

    public void setMediaController() {
        // Create a MediaControllerCompat
        mediaController = new MediaControllerCompat(this, mediaSession);

        MediaControllerCompat.setMediaController(this, mediaController);
    }

    public void setStatebuilder() {
        // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player.
        statebuilder = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PLAY_PAUSE);

        mediaSession.setPlaybackState(statebuilder.build());
    }

    public void setMediaPlayer() {
        // Create Mediaplayer
        mediaPlayer = MediaPlayer.create(this, R.raw.video);



    }
}