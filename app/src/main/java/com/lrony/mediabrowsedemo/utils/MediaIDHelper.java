package com.lrony.mediabrowsedemo.utils;

import java.util.Arrays;

/**
 * Created by Lrony on 19-2-22.
 */
public class MediaIDHelper {

    private static final String TAG = "MediaIDHelper";

    // Media IDs used on browseable items of MediaBrowser
    public static final String MEDIA_ID_ROOT = "__ROOT__";
    public static final String MEDIA_ID_MUSICS_BY_ARTIST = "__BY_ARTIST__";
    public static final String MEDIA_ID_MUSICS_BY_ALBUM = "__BY_ALBUM__";
    public static final String MEDIA_ID_MUSICS_BY_SONG = "__BY_SONG__";
    public static final String MEDIA_ID_MUSICS_BY_FOLDER = "__BY_FOLDER__";
    public static final String MEDIA_ID_MUSICS_BY_SEARCH = "__BY_SEARCH__";

    private static final char CATEGORY_SEPARATOR = 31;
    private static final char LEAF_SEPARATOR = 30;

    public static String createMediaID(String musicID, String... categories) {
        // MediaIDs are of the form <categoryType>/<categoryValue>|<musicUniqueId>, to make it easy
        // to find the category (like genre) that a music was selected from, so we
        // can correctly build the playing queue. This is specially useful when
        // one music can appear in more than one list, like "by genre -> genre_1"
        // and "by artist -> artist_1".
        StringBuilder sb = new StringBuilder();
        if (categories != null && categories.length > 0) {
            sb.append(categories[0]);
            for (int i = 1; i < categories.length; i++) {
                sb.append(CATEGORY_SEPARATOR).append(categories[i]);
            }
        }
        if (musicID != null) {
            sb.append(LEAF_SEPARATOR).append(musicID);
        }
        return sb.toString();
    }

    public static String createBrowseCategoryMediaID(String categoryType, String categoryValue) {
        return categoryType + CATEGORY_SEPARATOR + categoryValue;
    }

    /**
     * Extracts unique musicID from the mediaID. mediaID is, by this sample's convention, a
     * concatenation of category (eg "by_genre"), categoryValue (eg "Classical") and unique
     * musicID. This is necessary so we know where the user selected the music from, when the music
     * exists in more than one music list, and thus we are able to correctly build the playing
     * queue.
     *
     * @param mediaID that contains the musicID
     * @return musicID
     */
    public static String extractMusicIDFromMediaID(String mediaID) {
        int pos = mediaID.indexOf(LEAF_SEPARATOR);
        if (pos >= 0) {
            return mediaID.substring(pos + 1);
        }
        return null;
    }

    /**
     * Extracts category and categoryValue from the mediaID. mediaID is, by this sample's
     * convention, a concatenation of category (eg "by_genre"), categoryValue (eg "Classical") and
     * mediaID. This is necessary so we know where the user selected the music from, when the music
     * exists in more than one music list, and thus we are able to correctly build the playing
     * queue.
     *
     * @param mediaID that contains a category and categoryValue.
     */
    public static String[] getHierarchy(String mediaID) {
        int pos = mediaID.indexOf(LEAF_SEPARATOR);
        if (pos >= 0) {
            mediaID = mediaID.substring(0, pos);
        }
        return mediaID.split(String.valueOf(CATEGORY_SEPARATOR));
    }

    public static String extractBrowseCategoryValueFromMediaID(String mediaID) {
        String[] hierarchy = getHierarchy(mediaID);
        if (hierarchy != null && hierarchy.length == 2) {
            return hierarchy[1];
        }
        return null;
    }

    private static boolean isBrowseable(String mediaID) {
        return mediaID.indexOf(LEAF_SEPARATOR) < 0;
    }

    public static String getParentMediaID(String mediaID) {
        String[] hierarchy = getHierarchy(mediaID);
        if (!isBrowseable(mediaID)) {
            return createMediaID(null, hierarchy);
        }
        if (hierarchy == null || hierarchy.length <= 1) {
            return MEDIA_ID_ROOT;
        }
        String[] parentHierarchy = Arrays.copyOf(hierarchy, hierarchy.length - 1);
        return createMediaID(null, parentHierarchy);
    }
}
