package com.lrony.mediabrowsedemo;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.lrony.mediabrowsedemo.utils.MediaPlaybackService;

import java.util.List;

/**
 * Created by Lrony on 19-2-25.
 */
@SuppressLint("NewApi")
class MainModel {

    private static final String TAG = "MainModel";

    private Context mContext;
    private MainInterface mView;

    private MediaController mMediaController;
    private MediaBrowser mMediaBrowser;
    private MediaConnectionCallback mConnectionCallback;
    private MediaSubscriptionCallback mSubscriptionCallback;
    private MediaControllerCallback mMediaControllerCallback;

    private String mMediaId;

    MainModel(Context context, MainInterface view) {
        mContext = context;
        mView = view;
        mConnectionCallback = new MediaConnectionCallback();
        mSubscriptionCallback = new MediaSubscriptionCallback();
        mMediaControllerCallback = new MediaControllerCallback();
        mMediaBrowser = new MediaBrowser(context, new ComponentName(context, MediaPlaybackService.class),
                mConnectionCallback, null);
    }

    /**
     * Connect to mediaBrowser.
     */
    void mediaBrowserConnect() {
        Log.d(TAG, "mediaBrowserConnect");
        if (mMediaBrowser != null) {
            mMediaBrowser.connect();
        }
    }

    /**
     * Disconnect mediaBrowser.
     */
    void mediaBrowserDisconnect() {
        Log.d(TAG, "mediaBrowserDisconnect");
        if (mMediaController != null) {
            mMediaController.unregisterCallback(mMediaControllerCallback);
        }
        if (mMediaBrowser != null) {
            mMediaBrowser.disconnect();
        }
    }

    void requestData(String mediaId) {
        Log.d(TAG, "requestData mediaId: " + mediaId);
        if (!TextUtils.isEmpty(mMediaId)) {
            mMediaBrowser.unsubscribe(mMediaId);
        }
        mMediaBrowser.subscribe(mediaId, mSubscriptionCallback);
        mMediaId = mediaId;
    }

    void playFromMediaId(String mediaId, Bundle extras) {
        Log.d(TAG, "playFromMediaId: " + mediaId);
        if (mMediaController == null) {
            return;
        }
        mMediaController.getTransportControls().playFromMediaId(mediaId, extras);
    }

    long getPosition() {
        if (mMediaController == null) {
            return 0;
        }
        PlaybackState playbackState = mMediaController.getPlaybackState();
        if (playbackState == null) {
            return 0;
        }
        return playbackState.getPosition();
    }

    long getDuration() {
        if (mMediaController == null) {
            return 0;
        }
        MediaMetadata mediaMetadata = mMediaController.getMetadata();
        if (mediaMetadata == null) {
            Log.d(TAG, "getDuration mediaMetadata is null");
            return 0;
        }
        return mediaMetadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
    }

    void playPause() {
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

    private class MediaConnectionCallback extends MediaBrowser.ConnectionCallback {

        @Override
        public void onConnected() {
            Log.d(TAG, "onConnected");
            mMediaController = new MediaController(mContext, mMediaBrowser.getSessionToken());
            mMediaController.registerCallback(mMediaControllerCallback);

            mMediaController.getTransportControls().sendCustomAction(MediaPlaybackService.ACTION_PLAY_HISTORY, null);

            if (mView != null) {
                mView.onMediaBrowserConnected(mMediaController);
                mView.onMetadataChanged(mMediaController.getMetadata());
            }
        }

        @Override
        public void onConnectionFailed() {
            Log.d(TAG, "onConnectionFailed");
        }

        @Override
        public void onConnectionSuspended() {
            Log.d(TAG, "onConnectionSuspended");
            if (mView != null) {
                mView.onMediaBrowserConnectionSuspended();
            }
        }
    }

    private class MediaSubscriptionCallback extends MediaBrowser.SubscriptionCallback {

        @Override
        public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaBrowser.MediaItem> children) {
            super.onChildrenLoaded(parentId, children);
            Log.d(TAG, "onChildrenLoaded parentId: " + parentId + " ,size: " + children.size());
            if (mView != null) {
                mView.updateAudioList(parentId, children);
            }
        }

        @Override
        public void onError(@NonNull String parentId) {
            super.onError(parentId);
            Log.d(TAG, "onError: " + parentId);
        }
    }

    private class MediaControllerCallback extends MediaController.Callback {

        @Override
        public void onPlaybackStateChanged(@Nullable PlaybackState state) {
            Log.d(TAG, "onPlaybackStateChanged");
            if (mView != null) {
                mView.onPlaybackStateChanged(state);
            }
        }

        @Override
        public void onMetadataChanged(@Nullable MediaMetadata metadata) {
            Log.d(TAG, "onMetadataChanged");
            if (mView != null) {
                mView.onMetadataChanged(metadata);
            }
        }
    }
}
