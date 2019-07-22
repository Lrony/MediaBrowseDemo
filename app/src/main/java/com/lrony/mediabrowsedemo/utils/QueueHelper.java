/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lrony.mediabrowsedemo.utils;

import android.annotation.SuppressLint;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static com.lrony.mediabrowsedemo.utils.MediaIDHelper.MEDIA_ID_MUSICS_BY_ALBUM;
import static com.lrony.mediabrowsedemo.utils.MediaIDHelper.MEDIA_ID_MUSICS_BY_ARTIST;
import static com.lrony.mediabrowsedemo.utils.MediaIDHelper.MEDIA_ID_MUSICS_BY_FOLDER;
import static com.lrony.mediabrowsedemo.utils.MediaIDHelper.MEDIA_ID_MUSICS_BY_SEARCH;
import static com.lrony.mediabrowsedemo.utils.MediaIDHelper.MEDIA_ID_MUSICS_BY_SONG;

/**
 * Utility class to help on queue related tasks.
 */
@SuppressLint("NewApi")
class QueueHelper {

    private static final String TAG = "QueueHelper";

    static List<MediaSession.QueueItem> getPlayingQueue(
            String mediaId, MusicProvider musicProvider) {
        // extract the browsing hierarchy from the media ID:
        String[] hierarchy = MediaIDHelper.getHierarchy(mediaId);

        if (hierarchy.length != 2) {
            Log.d(TAG, "Could not build a playing queue for this mediaId: " + mediaId);
            return null;
        }

        String categoryType = hierarchy[0];
        String categoryValue = hierarchy[1];
        Log.d(TAG, "Creating playing queue for " + categoryType + ",  " + categoryValue);

        Iterable<MediaMetadata> tracks = null;
        // This sample only supports genre and by_search category types.
        switch (categoryType) {
            case MEDIA_ID_MUSICS_BY_SONG:
                tracks = musicProvider.getMusicList();
                break;
            case MEDIA_ID_MUSICS_BY_ALBUM:
                tracks = musicProvider.getMusicsByAlbum(categoryValue);
                break;
            case MEDIA_ID_MUSICS_BY_FOLDER:
                tracks = musicProvider.getMusicsByFolder(categoryValue);
                break;
            case MEDIA_ID_MUSICS_BY_ARTIST:
                Log.d(TAG, "Not supported");
                break;
            default:
                break;
        }

        if (tracks == null) {
            Log.d(
                    TAG, "Unrecognized category type: " + categoryType + " for mediaId " + mediaId);
            return null;
        }

        return convertToQueue(tracks, hierarchy[0], hierarchy[1]);
    }

    static List<MediaSession.QueueItem> getPlayingQueueFromSearch(
            String query, MusicProvider musicProvider) {
        Log.d(TAG, "Creating playing queue for musics from search " + query);

        return convertToQueue(musicProvider.searchMusic(query), MEDIA_ID_MUSICS_BY_SEARCH, query);
    }

    static int getMusicIndexOnQueue(Iterable<MediaSession.QueueItem> queue, String mediaId) {
        int index = 0;
        for (MediaSession.QueueItem item : queue) {
            if (mediaId.equals(item.getDescription().getMediaId())) {
                return index;
            }
            index++;
        }
        return -1;
    }

    static int getMusicIndexOnQueue(Iterable<MediaSession.QueueItem> queue, long queueId) {
        int index = 0;
        for (MediaSession.QueueItem item : queue) {
            if (queueId == item.getQueueId()) {
                return index;
            }
            index++;
        }
        return -1;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private static List<MediaSession.QueueItem> convertToQueue(
            Iterable<MediaMetadata> tracks, String... categories) {
        List<MediaSession.QueueItem> queue = new ArrayList<>();
        int count = 0;
        for (MediaMetadata track : tracks) {
            // We create a hierarchy-aware mediaID, so we know what the queue is about by looking
            // at the QueueItem media IDs.
            String hierarchyAwareMediaID =
                    MediaIDHelper.createMediaID(track.getDescription().getMediaId(), categories);
            long duration = track.getLong(MediaMetadata.METADATA_KEY_DURATION);
            MediaDescription.Builder descriptionBuilder = new MediaDescription.Builder();
            MediaDescription description = track.getDescription();
            Bundle extras = description.getExtras();
            if (extras == null) {
                extras = new Bundle();
            }
            extras.putLong(MediaMetadata.METADATA_KEY_DURATION, duration);
            descriptionBuilder.setExtras(extras)
                    .setMediaId(hierarchyAwareMediaID)
                    .setTitle(description.getTitle())
                    .setSubtitle(track.getString(MediaMetadata.METADATA_KEY_ARTIST))
                    .setIconBitmap(description.getIconBitmap())
                    .setIconUri(description.getIconUri())
                    .setMediaUri(description.getMediaUri())
                    .setDescription(description.getDescription());

            // We don't expect queues to change after created, so we use the item index as the
            // queueId. Any other number unique in the queue would work.
            MediaSession.QueueItem item =
                    new MediaSession.QueueItem(descriptionBuilder.build(), count++);
            queue.add(item);
        }
        return queue;
    }

    /**
     * Create a random queue. For simplicity sake, instead of a random queue, we create a
     * queue using the first genre.
     *
     * @param musicProvider the provider used for fetching music.
     * @return list containing {@link MediaSession.QueueItem}'s
     */
    static List<MediaSession.QueueItem> getRandomQueue(MusicProvider musicProvider) {
        Iterator<String> genres = musicProvider.getArtists().iterator();
        if (!genres.hasNext()) {
            return Collections.emptyList();
        }
        String genre = genres.next();
        Iterable<MediaMetadata> tracks = musicProvider.getMusicsByAlbum(genre);

        return convertToQueue(tracks, MEDIA_ID_MUSICS_BY_ARTIST, genre);
    }

    static boolean isIndexPlayable(int index, List<MediaSession.QueueItem> queue) {
        return (queue != null && index >= 0 && index < queue.size());
    }
}
