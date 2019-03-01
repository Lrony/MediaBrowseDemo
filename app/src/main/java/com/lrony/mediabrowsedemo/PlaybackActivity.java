package com.lrony.mediabrowsedemo;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

@SuppressLint("NewApi")
public class PlaybackActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "PlaybackActivity";

    private MediaBrowser mMediaBrowser;
    private MediaController mMediaController;

    private MediaConnectionCallback mConnectionCallback;
    private MediaControllerCallback mMediaControllerCallback;

    private ImageView mImgIcon;
    private ImageView mImgPre;
    private ImageView mImgPlayPause;
    private ImageView mImgNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playback);

        mConnectionCallback = new MediaConnectionCallback();
        mMediaControllerCallback = new MediaControllerCallback();

        // You can write like this.
        // mMediaBrowser = new MediaBrowser(this, new ComponentName(this, MediaPlaybackService.class),
        // mConnectionCallback, null);

        // You can also write like this.
        mMediaBrowser = new MediaBrowser(this, new ComponentName("com.lrony.mediabrowsedemo",
                "com.lrony.mediabrowsedemo.utils.MediaPlaybackService")
                , mConnectionCallback, null);
        mMediaBrowser.connect();

        mImgPre = findViewById(R.id.img_pre);
        mImgPlayPause = findViewById(R.id.img_play_pause);
        mImgNext = findViewById(R.id.img_next);
        mImgIcon = findViewById(R.id.img_icon);
        mImgPre.setOnClickListener(this);
        mImgPlayPause.setOnClickListener(this);
        mImgNext.setOnClickListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMediaController != null) {
            mMediaController.unregisterCallback(mMediaControllerCallback);
        }
        if (mMediaBrowser != null) {
            mMediaBrowser.disconnect();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.img_pre:
                mMediaController.getTransportControls().skipToPrevious();
                break;
            case R.id.img_play_pause:
                playPause();
                break;
            case R.id.img_next:
                mMediaController.getTransportControls().skipToNext();
                break;
            default:
                break;
        }
    }

    private class MediaConnectionCallback extends MediaBrowser.ConnectionCallback {

        @Override
        public void onConnected() {
            Log.d(TAG, "onConnected");
            mMediaController = new MediaController(PlaybackActivity.this, mMediaBrowser.getSessionToken());
            mMediaController.registerCallback(mMediaControllerCallback);
            setMediaController(mMediaController);
            updateTrackInfo(mMediaController.getMetadata());
            updatePlayPauseButtonImage(mMediaController.getPlaybackState());
        }

        @Override
        public void onConnectionFailed() {
            Log.d(TAG, "onConnectionFailed");
        }

        @Override
        public void onConnectionSuspended() {
            Log.d(TAG, "onConnectionSuspended");
            setMediaController(null);
        }
    }

    private class MediaControllerCallback extends MediaController.Callback {

        @Override
        public void onPlaybackStateChanged(@Nullable PlaybackState state) {
            Log.d(TAG, "onPlaybackStateChanged");
            updatePlayPauseButtonImage(state);
        }

        @Override
        public void onMetadataChanged(@Nullable MediaMetadata metadata) {
            Log.d(TAG, "onMetadataChanged");
            updateTrackInfo(metadata);
        }
    }

    private void updateTrackInfo(MediaMetadata metadata) {
        if (metadata == null) return;
        Bitmap albumArt = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
        mImgIcon.setImageBitmap(albumArt);
    }

    private void updatePlayPauseButtonImage(PlaybackState state) {
        Log.d(TAG, "updatePlayPauseButtonImage");
        if (state == null) {
            Log.d(TAG, "updatePlayPauseButtonImage state is null!!!");
            return;
        }
        if (state.getState() != PlaybackState.STATE_PLAYING) {
            mImgPlayPause.setImageResource(R.drawable.ic_play_arrow);
        } else {
            mImgPlayPause.setImageResource(R.drawable.ic_pause);
        }
    }

    private void playPause() {
        if (mMediaController == null) {
            return;
        }
        PlaybackState playbackState = mMediaController.getPlaybackState();
        if (playbackState == null) {
            return;
        }
        if (playbackState.getState() != PlaybackState.STATE_PLAYING) {
            play();
        } else {
            pause();
        }
    }

    private void play() {
        if (mMediaController == null) {
            return;
        }
        mMediaController.getTransportControls().play();
    }

    /**
     * Pause music.
     */
    private void pause() {
        if (mMediaController == null) {
            return;
        }
        mMediaController.getTransportControls().pause();
    }
}
