package com.lrony.mediabrowsedemo.utils;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadata;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by Lrony on 19-2-22.
 * <p>
 * A provider of music contents to the music application, it reads external storage for any music
 * files, parse them and store them in this class for future use.
 */
@SuppressLint("NewApi")
public class MusicProvider {

    private static final String TAG = "MusicProvider";

    // Public constants
    public static final String UNKOWN = "UNKNOWN";
    // Uri source of this track
    public static final String CUSTOM_METADATA_TRACK_SOURCE = "__SOURCE__";

    // Content select criteria
    private static final String MUSIC_SELECT_FILTER = MediaStore.Audio.Media.IS_MUSIC + " != 0";
    private static final String MUSIC_SORT_ORDER = MediaStore.Audio.Media.TITLE + " ASC";

    // Categorized caches for music track data:
    private Context mContext;
    // Album Name --> list of Metadata
    private ConcurrentMap<String, List<MediaMetadata>> mMusicListByAlbum;
    // Artist Name --> Map of (album name --> album metadata)
    private ConcurrentMap<String, Map<String, MediaMetadata>> mArtistAlbumDb;
    // Folder Name --> list of Metadata
    private ConcurrentMap<String, List<MediaMetadata>> mMusicListByFolder;
    private List<MediaMetadata> mMusicList;
    private final ConcurrentMap<Long, Song> mMusicListById;

    enum State {NON_INITIALIZED, INITIALIZING, INITIALIZED}

    private volatile State mCurrentState = State.NON_INITIALIZED;

    public MusicProvider(Context context) {
        mContext = context;
        mMusicListByAlbum = new ConcurrentHashMap<>();
        mArtistAlbumDb = new ConcurrentHashMap<>();
        mMusicListByFolder = new ConcurrentHashMap<>();
        mMusicList = new ArrayList<>();
        mMusicListById = new ConcurrentHashMap<>();
    }

    public boolean isInitialized() {
        return mCurrentState == State.INITIALIZED;
    }

    /**
     * Get an iterator over the list of artists
     *
     * @return list of artists
     */
    public Iterable<String> getArtists() {
        if (mCurrentState != State.INITIALIZED) {
            return Collections.emptyList();
        }
        return mArtistAlbumDb.keySet();
    }

    /**
     * Get an iterator over the list of albums
     *
     * @return list of albums
     */
    public Iterable<MediaMetadata> getAlbums() {
        if (mCurrentState != State.INITIALIZED) {
            return Collections.emptyList();
        }
        ArrayList<MediaMetadata> albumList = new ArrayList<>();
        for (Map<String, MediaMetadata> artist_albums : mArtistAlbumDb.values()) {
            albumList.addAll(artist_albums.values());
        }
        return albumList;
    }

    /**
     * Get an iterator over the list of folders
     *
     * @return list of folders
     */
    public Iterable<String> getFolders() {
        if (mCurrentState != State.INITIALIZED) {
            return Collections.emptyList();
        }
        return mMusicListByFolder.keySet();
    }

    public Iterable<MediaMetadata> getMusicList() {
        return mMusicList;
    }

    /**
     * Get albums of a certain artist
     */
    public Iterable<MediaMetadata> getAlbumByArtist(String artist) {
        if (mCurrentState != State.INITIALIZED || !mArtistAlbumDb.containsKey(artist)) {
            return Collections.emptyList();
        }
        return mArtistAlbumDb.get(artist).values();
    }

    /**
     * Get music tracks of the given album
     */
    public Iterable<MediaMetadata> getMusicsByAlbum(String album) {
        if (mCurrentState != State.INITIALIZED || !mMusicListByAlbum.containsKey(album)) {
            return Collections.emptyList();
        }
        return mMusicListByAlbum.get(album);
    }

    /**
     * Get music tracks of the given folder
     */
    public Iterable<MediaMetadata> getMusicsByFolder(String folder) {
        if (mCurrentState != State.INITIALIZED || !mMusicListByFolder.containsKey(folder)) {
            return Collections.emptyList();
        }
        return mMusicListByFolder.get(folder);
    }

    /**
     * Return the MediaMetadata for the given musicID.
     *
     * @param musicId The unique, non-hierarchical music ID.
     */
    public Song getMusicById(long musicId) {
        return mMusicListById.containsKey(musicId) ? mMusicListById.get(musicId) : null;
    }

    public Iterable<MediaMetadata> searchMusic(String titleQuery) {
        if (mCurrentState != State.INITIALIZED) {
            return Collections.emptyList();
        }
        ArrayList<MediaMetadata> result = new ArrayList<>();
        titleQuery = titleQuery.toLowerCase();
        for (Song song : mMusicListById.values()) {
            if (song.getMetadata()
                    .getString(MediaMetadata.METADATA_KEY_TITLE)
                    .toLowerCase()
                    .contains(titleQuery)) {
                result.add(song.getMetadata());
            }
        }
        return result;
    }

    public interface MusicProviderCallback {
        void onMusicCatalogReady(boolean success);
    }

    /**
     * Get the list of music tracks from disk and caches the track information
     * for future reference, keying tracks by musicId and grouping by genre.
     */
    @SuppressLint("StaticFieldLeak")
    public void retrieveMediaAsync(final MusicProviderCallback callback) {
        Log.d(TAG, "retrieveMediaAsync called");
        if (mCurrentState == State.INITIALIZED) {
            // Nothing to do, execute callback immediately
            callback.onMusicCatalogReady(true);
            return;
        }

        // Asynchronously load the music catalog in a separate thread
        new AsyncTask<Void, Void, State>() {
            @Override
            protected State doInBackground(Void... params) {
                if (mCurrentState == State.INITIALIZED) {
                    return mCurrentState;
                }
                mCurrentState = State.INITIALIZING;
                if (retrieveMedia()) {
                    mCurrentState = State.INITIALIZED;
                } else {
                    mCurrentState = State.NON_INITIALIZED;
                }
                return mCurrentState;
            }

            @Override
            protected void onPostExecute(State current) {
                if (callback != null) {
                    callback.onMusicCatalogReady(current == State.INITIALIZED);
                }
            }
        }
                .execute();
    }

    private synchronized boolean retrieveMedia() {
        Cursor cursor =
                mContext.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        null, MUSIC_SELECT_FILTER, null, MUSIC_SORT_ORDER);
        if (cursor == null) {
            Log.d(TAG, "Failed to retreive music: cursor is null");
            mCurrentState = State.NON_INITIALIZED;
            return false;
        }
        if (!cursor.moveToFirst()) {
            Log.d(TAG, "Failed to move cursor to first row (no query result)");
            cursor.close();
            return true;
        }
        int idColumn = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
        int titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
        int pathColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
        do {
            Log.i(TAG,
                    "Music ID: " + cursor.getString(idColumn)
                            + " Title: " + cursor.getString(titleColumn));
            long thisId = cursor.getLong(idColumn);
            String thisPath = cursor.getString(pathColumn);
            MediaMetadata metadata = retrievMediaMetadata(thisId, thisPath);
            Log.i(TAG, "MediaMetadata: " + metadata);
            if (metadata == null) {
                continue;
            }
            Song thisSong = new Song(thisId, metadata, null);
            // Construct per feature database
            mMusicList.add(metadata);
            mMusicListById.put(thisId, thisSong);
            addMusicToAlbumList(metadata);
            addMusicToArtistList(metadata);
            addMusicToFolderList(metadata);
        } while (cursor.moveToNext());
        cursor.close();
        return true;
    }

    private synchronized MediaMetadata retrievMediaMetadata(long musicId, String musicPath) {
        Log.d(TAG, "getting metadata for music: " + musicPath);
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        Uri contentUri = ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, musicId);
        if (!(new File(musicPath).exists())) {
            Log.d(TAG, "Does not exist, deleting item");
            mContext.getContentResolver().delete(contentUri, null, null);
            return null;
        }
        retriever.setDataSource(mContext, contentUri);
        String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        String album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
        String artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
        String durationString =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        long duration = durationString != null ? Long.parseLong(durationString) : 0;
        @SuppressLint("WrongConstant") MediaMetadata.Builder metadataBuilder =
                new MediaMetadata.Builder()
                        .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, String.valueOf(musicId))
                        .putString(CUSTOM_METADATA_TRACK_SOURCE, musicPath)
                        .putString(MediaMetadata.METADATA_KEY_TITLE, title != null ? title : UNKOWN)
                        .putString(MediaMetadata.METADATA_KEY_ALBUM, album != null ? album : UNKOWN)
                        .putString(
                                MediaMetadata.METADATA_KEY_ARTIST, artist != null ? artist : UNKOWN)
                        .putLong(MediaMetadata.METADATA_KEY_DURATION, duration);
        byte[] albumArtData = retriever.getEmbeddedPicture();
        Bitmap bitmap;
        if (albumArtData != null) {
            bitmap = BitmapFactory.decodeByteArray(albumArtData, 0, albumArtData.length);
            bitmap = MusicUtils.resizeBitmap(bitmap, MusicUtils.getDefaultAlbumArt(mContext));
            metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, bitmap);
        }
        retriever.release();
        return metadataBuilder.build();
    }

    private void addMusicToAlbumList(MediaMetadata metadata) {
        String thisAlbum = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM);
        if (thisAlbum == null) {
            thisAlbum = UNKOWN;
        }
        if (!mMusicListByAlbum.containsKey(thisAlbum)) {
            mMusicListByAlbum.put(thisAlbum, new ArrayList<MediaMetadata>());
        }
        mMusicListByAlbum.get(thisAlbum).add(metadata);
    }

    private void addMusicToArtistList(MediaMetadata metadata) {
        String thisArtist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
        if (thisArtist == null) {
            thisArtist = UNKOWN;
        }
        String thisAlbum = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM);
        if (thisAlbum == null) {
            thisAlbum = UNKOWN;
        }
        if (!mArtistAlbumDb.containsKey(thisArtist)) {
            mArtistAlbumDb.put(thisArtist, new ConcurrentHashMap<String, MediaMetadata>());
        }
        Map<String, MediaMetadata> albumsMap = mArtistAlbumDb.get(thisArtist);
        MediaMetadata.Builder builder;
        long count = 0;
        Bitmap thisAlbumArt = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
        if (albumsMap.containsKey(thisAlbum)) {
            MediaMetadata album_metadata = albumsMap.get(thisAlbum);
            count = album_metadata.getLong(MediaMetadata.METADATA_KEY_NUM_TRACKS);
            Bitmap nAlbumArt = album_metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
            builder = new MediaMetadata.Builder(album_metadata);
            if (nAlbumArt != null) {
                thisAlbumArt = null;
            }
        } else {
            builder = new MediaMetadata.Builder();
            builder.putString(MediaMetadata.METADATA_KEY_ALBUM, thisAlbum)
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, thisArtist);
        }
        if (thisAlbumArt != null) {
            builder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, thisAlbumArt);
        }
        builder.putLong(MediaMetadata.METADATA_KEY_NUM_TRACKS, count + 1);
        albumsMap.put(thisAlbum, builder.build());
    }

    private void addMusicToFolderList(MediaMetadata metadata) {
        @SuppressLint("WrongConstant")
        String thisFolder = metadata.getString(CUSTOM_METADATA_TRACK_SOURCE);
        int fileNameStart = thisFolder.lastIndexOf(File.separator);
        String dirName = UNKOWN;
        if (fileNameStart > 0) {
            String dirPath = thisFolder.substring(0, fileNameStart);
            int dirNameStart = dirPath.lastIndexOf(File.separator) + 1;
            dirName = dirPath.substring(dirNameStart, dirPath.length());
        }
        if (!mMusicListByFolder.containsKey(dirName)) {
            mMusicListByFolder.put(dirName, new ArrayList<MediaMetadata>());
        }
        mMusicListByFolder.get(dirName).add(metadata);
    }

    public synchronized void updateMusic(long musicId, MediaMetadata metadata) {
        Song song = mMusicListById.get(musicId);
        if (song == null) {
            return;
        }

        String oldGenre = song.getMetadata().getString(MediaMetadata.METADATA_KEY_GENRE);
        String newGenre = metadata.getString(MediaMetadata.METADATA_KEY_GENRE);

        song.setMetadata(metadata);

        // if genre has changed, we need to rebuild the list by genre
        if (!oldGenre.equals(newGenre)) {
            //            buildListsByGenre();
        }
    }
}
