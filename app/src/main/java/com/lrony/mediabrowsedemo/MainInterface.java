package com.lrony.mediabrowsedemo;

import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.PlaybackState;

import java.util.List;

/**
 * Created by Lrony on 19-2-25.
 */
public interface MainInterface {

    void updateAudioList(String parentId, List<MediaBrowser.MediaItem> list);

    void onMediaBrowserConnected(MediaController mediaController);

    void onMediaBrowserConnectionSuspended();

    void onPlaybackStateChanged(PlaybackState state);

    void onMetadataChanged(MediaMetadata metadata);
}
