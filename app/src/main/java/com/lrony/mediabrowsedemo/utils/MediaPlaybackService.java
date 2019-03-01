package com.lrony.mediabrowsedemo.utils;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.service.media.MediaBrowserService;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.lrony.mediabrowsedemo.R;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.lrony.mediabrowsedemo.utils.MediaIDHelper.MEDIA_ID_MUSICS_BY_ALBUM;
import static com.lrony.mediabrowsedemo.utils.MediaIDHelper.MEDIA_ID_MUSICS_BY_ARTIST;
import static com.lrony.mediabrowsedemo.utils.MediaIDHelper.MEDIA_ID_MUSICS_BY_FOLDER;
import static com.lrony.mediabrowsedemo.utils.MediaIDHelper.MEDIA_ID_MUSICS_BY_SONG;
import static com.lrony.mediabrowsedemo.utils.MediaIDHelper.MEDIA_ID_ROOT;

/**
 * Created by Lrony on 19-2-22.
 * <p>
 * Provides "background" audio playback capabilities, allowing the
 * user to switch between activities without stopping playback.
 */
@SuppressLint("NewApi")
public class MediaPlaybackService extends MediaBrowserService implements Playback.Callback {

    private static final String TAG = "MediaPlaybackService";

    // Delay stopSelf by using a handler.
    private static final int STOP_DELAY = 30000;

    public static final String ACTION_CMD = "com.android.music.ACTION_CMD";
    public static final String CMD_NAME = "CMD_NAME";
    public static final String CMD_PAUSE = "CMD_PAUSE";
    public static final String CMD_REPEAT = "CMD_PAUSE";
    public static final String REPEAT_MODE = "REPEAT_MODE";

    public enum RepeatMode {REPEAT_NONE, REPEAT_ALL, REPEAT_CURRENT}

    // Music catalog manager
    private MusicProvider mMusicProvider;
    private MediaSession mSession;
    // "Now playing" queue:
    private List<MediaSession.QueueItem> mPlayingQueue = null;
    private int mCurrentIndexOnQueue = -1;
    // Indicates whether the service was started.
    private boolean mServiceStarted;
    private DelayedStopHandler mDelayedStopHandler = new DelayedStopHandler(this);
    private Playback mPlayback;
    // Default mode is repeat none
    private RepeatMode mRepeatMode = RepeatMode.REPEAT_NONE;
    // Extra information for this session
    private Bundle mExtras;

    public MediaPlaybackService() {
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();
        mPlayingQueue = new ArrayList<>();
        mMusicProvider = new MusicProvider(this);

        // Start a new MediaSession
        mSession = new MediaSession(this, "MediaPlaybackService");
        // Set extra information
        mExtras = new Bundle();
        mExtras.putInt(REPEAT_MODE, mRepeatMode.ordinal());
        mSession.setExtras(mExtras);
        // Enable callbacks from MediaButtons and TransportControls
        mSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS
                | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
        PlaybackState.Builder stateBuilder = new PlaybackState.Builder().setActions(
                PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PLAY_PAUSE);
        mSession.setPlaybackState(stateBuilder.build());
        // MediaSessionCallback() has methods that handle callbacks from a media controller
        mSession.setCallback(new MediaSessionCallback());
        // Set the session's token so that client activities can communicate with it.
        setSessionToken(mSession.getSessionToken());

        mPlayback = new Playback(this, mMusicProvider);
        mPlayback.setState(PlaybackState.STATE_NONE);
        mPlayback.setCallback(this);
        mPlayback.start();

        updatePlaybackState(null);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");

    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        Log.d(TAG, "OnGetRoot: clientPackageName=" + clientPackageName + "; clientUid=" + clientUid
                + " ; rootHints=" + rootHints);
        return new BrowserRoot(MEDIA_ID_ROOT, null);
    }

    @Override
    public void onLoadChildren(@NonNull final String parentMediaId, @NonNull final Result<List<MediaBrowser.MediaItem>> result) {
        Log.d(TAG, "OnLoadChildren: parentMediaId=" + parentMediaId);
        //  Browsing not allowed
        if (parentMediaId == null) {
            result.sendResult(null);
            return;
        }

        if (!mMusicProvider.isInitialized()) {
            // Use result.detach to allow calling result.sendResult from another thread:
            result.detach();

            mMusicProvider.retrieveMediaAsync(new MusicProvider.MusicProviderCallback() {
                @Override
                public void onMusicCatalogReady(boolean success) {
                    Log.d(TAG, "Received catalog result, success:  " + String.valueOf(success));
                    if (success) {
                        onLoadChildren(parentMediaId, result);
                    } else {
                        result.sendResult(Collections.<MediaBrowser.MediaItem>emptyList());
                    }
                }
            });

        } else {
            // If our music catalog is already loaded/cached, load them into result immediately
            List<MediaBrowser.MediaItem> mediaItems = new ArrayList<>();

            switch (parentMediaId) {
                case MEDIA_ID_ROOT:
                    Log.d(TAG, "OnLoadChildren.ROOT");
                    mediaItems.add(new MediaBrowser.MediaItem(new MediaDescription.Builder()
                            .setMediaId(MEDIA_ID_MUSICS_BY_ARTIST)
                            .setTitle(getString(R.string.media_list_title_artists))
                            .build(),
                            MediaBrowser.MediaItem.FLAG_BROWSABLE));
                    mediaItems.add(new MediaBrowser.MediaItem(new MediaDescription.Builder()
                            .setMediaId(MEDIA_ID_MUSICS_BY_ALBUM)
                            .setTitle(getString(R.string.media_list_title_albums))
                            .build(),
                            MediaBrowser.MediaItem.FLAG_BROWSABLE));
                    mediaItems.add(new MediaBrowser.MediaItem(new MediaDescription.Builder()
                            .setMediaId(MEDIA_ID_MUSICS_BY_SONG)
                            .setTitle(getString(R.string.media_list_title_songs))
                            .build(),
                            MediaBrowser.MediaItem.FLAG_BROWSABLE));
                    mediaItems.add(new MediaBrowser.MediaItem(new MediaDescription.Builder()
                            .setMediaId(MEDIA_ID_MUSICS_BY_FOLDER)
                            .setTitle(getString(R.string.media_list_title_folders))
                            .build(),
                            MediaBrowser.MediaItem.FLAG_BROWSABLE));
                    break;
                case MEDIA_ID_MUSICS_BY_ARTIST:
                    Log.d(TAG, "OnLoadChildren.ARTIST");
                    for (String artist : mMusicProvider.getArtists()) {
                        MediaBrowser.MediaItem item = new MediaBrowser.MediaItem(
                                new MediaDescription.Builder()
                                        .setMediaId(MediaIDHelper.createBrowseCategoryMediaID(
                                                MEDIA_ID_MUSICS_BY_ARTIST, artist))
                                        .setTitle(artist)
                                        .build(),
                                MediaBrowser.MediaItem.FLAG_BROWSABLE);
                        mediaItems.add(item);
                    }
                    break;
                case MEDIA_ID_MUSICS_BY_ALBUM:
                    Log.d(TAG, "OnLoadChildren.ALBUM");
                    loadAlbum(mMusicProvider.getAlbums(), mediaItems);
                    break;
                case MEDIA_ID_MUSICS_BY_SONG:
                    Log.d(TAG, "OnLoadChildren.SONG");
                    String hierarchyAwareMediaID = MediaIDHelper.createBrowseCategoryMediaID(
                            parentMediaId, MEDIA_ID_MUSICS_BY_SONG);
                    loadSong(mMusicProvider.getMusicList(), mediaItems, hierarchyAwareMediaID);
                    break;
                case MEDIA_ID_MUSICS_BY_FOLDER:
                    Log.d(TAG, "OnLoadChildren.FOLDER");
                    for (String folder : mMusicProvider.getFolders()) {
                        MediaBrowser.MediaItem item = new MediaBrowser.MediaItem(
                                new MediaDescription.Builder()
                                        .setMediaId(MediaIDHelper.createBrowseCategoryMediaID(
                                                MEDIA_ID_MUSICS_BY_FOLDER, folder))
                                        .setTitle(folder)
                                        .build(),
                                MediaBrowser.MediaItem.FLAG_BROWSABLE);
                        mediaItems.add(item);
                    }
                    break;
                default:
                    if (parentMediaId.startsWith(MEDIA_ID_MUSICS_BY_ARTIST)) {
                        String artist = MediaIDHelper.getHierarchy(parentMediaId)[1];
                        Log.d(TAG, "OnLoadChildren.SONGS_BY_ARTIST  artist=" + artist);
                        loadAlbum(mMusicProvider.getAlbumByArtist(artist), mediaItems);
                    } else if (parentMediaId.startsWith(MEDIA_ID_MUSICS_BY_ALBUM)) {
                        String album = MediaIDHelper.getHierarchy(parentMediaId)[1];
                        Log.d(TAG, "OnLoadChildren.SONGS_BY_ALBUM  album=" + album);
                        loadSong(mMusicProvider.getMusicsByAlbum(album), mediaItems, parentMediaId);
                    } else if (parentMediaId.startsWith(MEDIA_ID_MUSICS_BY_FOLDER)) {
                        String folder = MediaIDHelper.getHierarchy(parentMediaId)[1];
                        Log.d(TAG, "OnLoadChildren.SONGS_BY_FOLDER  folder=" + folder);
                        loadSong(mMusicProvider.getMusicsByFolder(folder), mediaItems, parentMediaId);
                    } else {
                        Log.w(TAG, "Skipping unmatched parentMediaId: " + parentMediaId);
                    }
                    break;
            }
            Log.d(TAG,
                    "OnLoadChildren sending " + mediaItems.size() + " results for "
                            + parentMediaId);
            result.sendResult(mediaItems);
            updateMetadata();
        }
    }

    private void loadPlayingQueue(List<MediaBrowser.MediaItem> mediaItems, String parentId) {
        for (MediaSession.QueueItem queueItem : mPlayingQueue) {
            MediaBrowser.MediaItem mediaItem =
                    new MediaBrowser.MediaItem(queueItem.getDescription(), MediaBrowser.MediaItem.FLAG_PLAYABLE);
            mediaItems.add(mediaItem);
        }
    }

    private void loadSong(
            Iterable<MediaMetadata> songList, List<MediaBrowser.MediaItem> mediaItems, String parentId) {
        for (MediaMetadata metadata : songList) {
            String hierarchyAwareMediaID =
                    MediaIDHelper.createMediaID(metadata.getDescription().getMediaId(), parentId);
            Bundle songExtra = new Bundle();
            songExtra.putLong(MediaMetadata.METADATA_KEY_DURATION,
                    metadata.getLong(MediaMetadata.METADATA_KEY_DURATION));
            String title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
            String artistName = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
            Bitmap albumArt = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
            MediaBrowser.MediaItem item = new MediaBrowser.MediaItem(new MediaDescription.Builder()
                    .setMediaId(hierarchyAwareMediaID)
                    .setTitle(title)
                    .setSubtitle(artistName)
                    .setIconBitmap(albumArt)
                    .setExtras(songExtra)
                    .build(),
                    MediaBrowser.MediaItem.FLAG_PLAYABLE);
            mediaItems.add(item);
        }
    }

    private void loadAlbum(Iterable<MediaMetadata> albumList, List<MediaBrowser.MediaItem> mediaItems) {
        for (MediaMetadata albumMetadata : albumList) {
            String albumName = albumMetadata.getString(MediaMetadata.METADATA_KEY_ALBUM);
            String artistName = albumMetadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
            Bundle albumExtra = new Bundle();
            albumExtra.putLong(MediaMetadata.METADATA_KEY_NUM_TRACKS,
                    albumMetadata.getLong(MediaMetadata.METADATA_KEY_NUM_TRACKS));
            MediaBrowser.MediaItem item = new MediaBrowser.MediaItem(
                    new MediaDescription.Builder()
                            .setMediaId(MediaIDHelper.createBrowseCategoryMediaID(
                                    MEDIA_ID_MUSICS_BY_ALBUM, albumName))
                            .setTitle(albumName)
                            .setSubtitle(artistName)
                            .setIconBitmap(
                                    albumMetadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART))
                            .setExtras(albumExtra)
                            .build(),
                    MediaBrowser.MediaItem.FLAG_BROWSABLE);
            mediaItems.add(item);
        }
    }

    private final class MediaSessionCallback extends MediaSession.Callback {

        @Override
        public void onPlay() {
            Log.d(TAG, "play");

            if (mPlayingQueue == null || mPlayingQueue.isEmpty()) {
                mPlayingQueue = QueueHelper.getRandomQueue(mMusicProvider);
                mSession.setQueue(mPlayingQueue);
                mSession.setQueueTitle(getString(R.string.usb_audio_random_queue_title));
                // start playing from the beginning of the queue
                mCurrentIndexOnQueue = 0;
            }

            if (mPlayingQueue != null && !mPlayingQueue.isEmpty()) {
                handlePlayRequest();
            }
        }

        @Override
        public void onSkipToQueueItem(long queueId) {
            Log.d(TAG, "OnSkipToQueueItem:" + queueId);

            if (mPlayingQueue != null && !mPlayingQueue.isEmpty()) {
                // set the current index on queue from the music Id:
                mCurrentIndexOnQueue = QueueHelper.getMusicIndexOnQueue(mPlayingQueue, queueId);
                // play the music
                handlePlayRequest();
            }
        }

        @Override
        public void onSeekTo(long position) {
            Log.d(TAG, "onSeekTo:" + position);
            mPlayback.seekTo((int) position);
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            Log.d(TAG, "playFromMediaId mediaId:" + mediaId + "  extras=" + extras);

            // The mediaId used here is not the unique musicId. This one comes from the
            // MediaBrowser, and is actually a "hierarchy-aware mediaID": a concatenation of
            // the hierarchy in MediaBrowser and the actual unique musicID. This is necessary
            // so we can build the correct playing queue, based on where the track was
            // selected from.
            mPlayingQueue = QueueHelper.getPlayingQueue(mediaId, mMusicProvider);
            mSession.setQueue(mPlayingQueue);
            String queueTitle = getString(R.string.usb_audio_browse_musics_by_genre_subtitle,
                    MediaIDHelper.extractBrowseCategoryValueFromMediaID(mediaId));
            mSession.setQueueTitle(queueTitle);

            if (mPlayingQueue != null && !mPlayingQueue.isEmpty()) {
                // set the current index on queue from the media Id:
                mCurrentIndexOnQueue = QueueHelper.getMusicIndexOnQueue(mPlayingQueue, mediaId);

                if (mCurrentIndexOnQueue < 0) {
                    Log.e(TAG, "playFromMediaId: media ID " + mediaId
                            + " could not be found on queue. Ignoring.");
                } else {
                    // play the music
                    handlePlayRequest();
                }
            }
        }

        @Override
        public void onPause() {
            Log.d(TAG, "pause. current state=" + mPlayback.getState());
            handlePauseRequest();
        }

        @Override
        public void onStop() {
            Log.d(TAG, "stop. current state=" + mPlayback.getState());
            handleStopRequest(null);
        }

        @Override
        public void onSkipToNext() {
            Log.d(TAG, "skipToNext");
            mCurrentIndexOnQueue++;
            if (mPlayingQueue != null && mCurrentIndexOnQueue >= mPlayingQueue.size()) {
                // This sample's behavior: skipping to next when in last song returns to the
                // first song.
                mCurrentIndexOnQueue = 0;
            }
            if (QueueHelper.isIndexPlayable(mCurrentIndexOnQueue, mPlayingQueue)) {
                handlePlayRequest();
            } else {
                Log.e(TAG,
                        "skipToNext: cannot skip to next. next Index=" + mCurrentIndexOnQueue
                                + " queue length="
                                + (mPlayingQueue == null ? "null" : mPlayingQueue.size()));
                handleStopRequest("Cannot skip");
            }
        }

        @Override
        public void onSkipToPrevious() {
            Log.d(TAG, "skipToPrevious");
            mCurrentIndexOnQueue--;
            if (mPlayingQueue != null && mCurrentIndexOnQueue < 0) {
                // This sample's behavior: skipping to previous when in first song restarts the
                // first song.
                mCurrentIndexOnQueue = 0;
            }
            if (QueueHelper.isIndexPlayable(mCurrentIndexOnQueue, mPlayingQueue)) {
                handlePlayRequest();
            } else {
                Log.e(TAG,
                        "skipToPrevious: cannot skip to previous. previous Index="
                                + mCurrentIndexOnQueue + " queue length="
                                + (mPlayingQueue == null ? "null" : mPlayingQueue.size()));
                handleStopRequest("Cannot skip");
            }
        }

        @Override
        public void onPlayFromSearch(String query, Bundle extras) {
            Log.d(TAG, "playFromSearch  query=" + query);

            if (TextUtils.isEmpty(query)) {
                // A generic search like "Play music" sends an empty query
                // and it's expected that we start playing something. What will be played depends
                // on the app: favorite playlist, "I'm feeling lucky", most recent, etc.
                mPlayingQueue = QueueHelper.getRandomQueue(mMusicProvider);
            } else {
                mPlayingQueue = QueueHelper.getPlayingQueueFromSearch(query, mMusicProvider);
            }

            Log.d(TAG, "playFromSearch  playqueue.length=" + mPlayingQueue.size());
            mSession.setQueue(mPlayingQueue);

            if (mPlayingQueue != null && !mPlayingQueue.isEmpty()) {
                // immediately start playing from the beginning of the search results
                mCurrentIndexOnQueue = 0;

                handlePlayRequest();
            } else {
                // if nothing was found, we need to warn the user and stop playing
                handleStopRequest(getString(R.string.usb_audio_no_search_results));
            }
        }

        @Override
        public void onCustomAction(String action, Bundle extras) {
            Log.d(TAG, "onCustomAction action=" + action + ", extras=" + extras);
            switch (action) {
                case CMD_REPEAT:
                    mRepeatMode = RepeatMode.values()[extras.getInt(REPEAT_MODE)];
                    mExtras.putInt(REPEAT_MODE, mRepeatMode.ordinal());
                    mSession.setExtras(mExtras);
                    Log.d(TAG, "modified repeatMode=" + mRepeatMode);
                    break;
                default:
                    Log.d(TAG, "Unkown action=" + action);
                    break;
            }
        }
    }

    /**
     * Handle a request to pause music
     */
    private void handlePauseRequest() {
        Log.d(TAG, "handlePauseRequest: mState=" + mPlayback.getState());
        mPlayback.pause();
        // reset the delayed stop handler.
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);
    }

    /**
     * Handle a request to stop music
     */
    private void handleStopRequest(String withError) {
        Log.d(
                TAG, "handleStopRequest: mState=" + mPlayback.getState() + " error=" + withError);
        mPlayback.stop(true);
        // reset the delayed stop handler.
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);

        updatePlaybackState(withError);

        // service is no longer necessary. Will be started again if needed.
        stopSelf();
        mServiceStarted = false;
    }

    private void handlePlayRequest() {
        Log.d(TAG, "handlePlayRequest: mState=" + mPlayback.getState());

        mDelayedStopHandler.removeCallbacksAndMessages(null);
        if (!mServiceStarted) {
            Log.v(TAG, "Starting service");
            // The MusicService needs to keep running even after the calling MediaBrowser
            // is disconnected. Call startService(Intent) and then stopSelf(..) when we no longer
            // need to play media.
            startService(new Intent(getApplicationContext(), MediaPlaybackService.class));
            mServiceStarted = true;
        }

        if (!mSession.isActive()) {
            mSession.setActive(true);
        }

        if (QueueHelper.isIndexPlayable(mCurrentIndexOnQueue, mPlayingQueue)) {
            updateMetadata();
            mPlayback.play(mPlayingQueue.get(mCurrentIndexOnQueue));
        }
    }

    @SuppressLint("WrongConstant")
    private void updateMetadata() {
        if (!QueueHelper.isIndexPlayable(mCurrentIndexOnQueue, mPlayingQueue)) {
            Log.e(TAG, "Can't retrieve current metadata.");
            updatePlaybackState(getResources().getString(R.string.usb_audio_error_no_metadata));
            return;
        }
        MediaSession.QueueItem queueItem = mPlayingQueue.get(mCurrentIndexOnQueue);
        String musicId =
                MediaIDHelper.extractMusicIDFromMediaID(queueItem.getDescription().getMediaId());
        MediaMetadata track = mMusicProvider.getMusicById(Long.parseLong(musicId)).getMetadata();
        final String trackId = track.getString(MediaMetadata.METADATA_KEY_MEDIA_ID);
        if (!musicId.equals(trackId)) {
            IllegalStateException e = new IllegalStateException("track ID should match musicId.");
            Log.d(TAG, "track ID should match musicId." + " musicId=" + musicId +
                    " trackId=" + trackId +
                    " mediaId from queueItem=" + queueItem.getDescription().getMediaId() +
                    " title from queueItem=" + queueItem.getDescription().getTitle() +
                    " mediaId from track=" + track.getDescription().getMediaId() +
                    " title from track=" + track.getDescription().getTitle() +
                    " source.hashcode from track=" +
                    track.getString(MusicProvider.CUSTOM_METADATA_TRACK_SOURCE).hashCode() + e);
            throw e;
        }
        Log.d(TAG, "Updating metadata for MusicID= " + musicId);
        mSession.setMetadata(track);

        // Set the proper album artwork on the media session, so it can be shown in the
        // locked screen and in other places.
        if (track.getDescription().getIconBitmap() == null
                && track.getDescription().getIconUri() != null) {
            String albumUri = track.getDescription().getIconUri().toString();
            AlbumArtCache.getInstance().fetch(albumUri, new AlbumArtCache.FetchListener() {
                @Override
                public void onFetched(String artUrl, Bitmap bitmap, Bitmap icon) {
                    MediaSession.QueueItem queueItem = mPlayingQueue.get(mCurrentIndexOnQueue);
                    MediaMetadata track = mMusicProvider.getMusicById(Long.parseLong(trackId)).getMetadata();
                    track = new MediaMetadata
                            .Builder(track)

                            // set high resolution bitmap in METADATA_KEY_ALBUM_ART. This is
                            // used, for
                            // example, on the lockscreen background when the media session
                            // is active.
                            .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, bitmap)

                            // set small version of the album art in the DISPLAY_ICON. This
                            // is used on
                            // the MediaDescription and thus it should be small to be
                            // serialized if
                            // necessary..
                            .putBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON, icon)

                            .build();

                    mMusicProvider.updateMusic(Long.parseLong(trackId), track);

                    // If we are still playing the same music
                    String currentPlayingId = MediaIDHelper.extractMusicIDFromMediaID(
                            queueItem.getDescription().getMediaId());
                    if (trackId.equals(currentPlayingId)) {
                        mSession.setMetadata(track);
                    }
                }
            });
        }
    }

    private void updatePlaybackState(String error) {
        if (mPlayback == null) return;
        Log.d(TAG, "updatePlaybackState, playback state=" + mPlayback.getState());
        long position = PlaybackState.PLAYBACK_POSITION_UNKNOWN;
        if (mPlayback != null && mPlayback.isConnected()) {
            position = mPlayback.getCurrentStreamPosition();
        }

        PlaybackState.Builder stateBuilder =
                new PlaybackState.Builder().setActions(getAvailableActions());

        int state = mPlayback.getState();

        // If there is an error message, send it to the playback state:
        if (error != null) {
            // Error states are really only supposed to be used for errors that cause playback to
            // stop unexpectedly and persist until the user takes action to fix it.
            stateBuilder.setErrorMessage(error);
            state = PlaybackState.STATE_ERROR;
        }
        stateBuilder.setState(state, position, 1.0f, SystemClock.elapsedRealtime());

        // Set the activeQueueItemId if the current index is valid.
        if (QueueHelper.isIndexPlayable(mCurrentIndexOnQueue, mPlayingQueue)) {
            MediaSession.QueueItem item = mPlayingQueue.get(mCurrentIndexOnQueue);
            stateBuilder.setActiveQueueItemId(item.getQueueId());
        }

        mSession.setPlaybackState(stateBuilder.build());
    }

    private long getAvailableActions() {
        long actions = PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PLAY_FROM_MEDIA_ID
                | PlaybackState.ACTION_PLAY_FROM_SEARCH;
        if (mPlayingQueue == null || mPlayingQueue.isEmpty()) {
            return actions;
        }
        if (mPlayback.isPlaying()) {
            actions |= PlaybackState.ACTION_PAUSE;
        }
        if (mCurrentIndexOnQueue > 0) {
            actions |= PlaybackState.ACTION_SKIP_TO_PREVIOUS;
        }
        if (mCurrentIndexOnQueue < mPlayingQueue.size() - 1) {
            actions |= PlaybackState.ACTION_SKIP_TO_NEXT;
        }
        return actions;
    }

    @Override
    public void onCompletion() {
        Log.d(TAG, "onCompletion");
        // The media player finished playing the current song, so we go ahead
        // and start the next.
        if (mPlayingQueue != null && !mPlayingQueue.isEmpty()) {
            switch (mRepeatMode) {
                case REPEAT_ALL:
                    // Increase the index
                    mCurrentIndexOnQueue++;
                    // Restart queue when reaching the end
                    if (mCurrentIndexOnQueue >= mPlayingQueue.size()) {
                        mCurrentIndexOnQueue = 0;
                    }
                    break;
                case REPEAT_CURRENT:
                    // Do not change the index
                    break;
                case REPEAT_NONE:
                default:
                    // Increase the index
                    mCurrentIndexOnQueue++;
                    // Stop the queue when reaching the end
                    if (mCurrentIndexOnQueue >= mPlayingQueue.size()) {
                        handleStopRequest(null);
                        return;
                    }
                    break;
            }
            handlePlayRequest();
        } else {
            // If there is nothing to play, we stop and release the resources:
            handleStopRequest(null);
        }
    }

    @Override
    public void onPlaybackStatusChanged(int state) {
        Log.d(TAG, "onPlaybackStatusChanged: " + state);
        updatePlaybackState(null);
    }

    @Override
    public void onError(String error) {
        Log.d(TAG, "onError: " + error);
        updatePlaybackState(error);
    }

    /**
     * A simple handler that stops the service if playback is not active (playing)
     */
    private static class DelayedStopHandler extends Handler {

        private final WeakReference<MediaPlaybackService> mWeakReference;

        private DelayedStopHandler(MediaPlaybackService service) {
            mWeakReference = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            MediaPlaybackService service = mWeakReference.get();
            if (service != null && service.mPlayback != null) {
                if (service.mPlayback.isPlaying()) {
                    Log.d(TAG, "Ignoring delayed stop since the media player is in use.");
                    return;
                }
                Log.d(TAG, "Stopping service with delay handler.");
                service.stopSelf();
                service.mServiceStarted = false;
            }
        }
    }
}
