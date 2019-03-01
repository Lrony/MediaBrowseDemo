package com.lrony.mediabrowsedemo.utils;

import android.annotation.SuppressLint;
import android.media.MediaMetadata;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.RequiresApi;

/**
 * Holder class that encapsulates a MediaMetadata and allows the actual metadata to be modified
 * without requiring to rebuild the collections the metadata is in.
 */
@SuppressLint("NewApi")
public class Song implements Parcelable {

    private MediaMetadata mMetadata;
    private long mSongId;
    private long mSortKey;

    public Song(long songId, MediaMetadata metadata, Long sortKey) {
        mMetadata = metadata;
        mSongId = songId;
        if (sortKey != null) {
            mSortKey = sortKey;
        }
    }

    public long getSongId() {
        return mSongId;
    }

    public long getSortKey() {
        return mSortKey;
    }

    public void setSortKey(long sortKey) {
        mSortKey = sortKey;
    }

    public MediaMetadata getMetadata() {
        return mMetadata;
    }

    public void setMetadata(MediaMetadata metadata) {
        mMetadata = metadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || o.getClass() != Song.class) {
            return false;
        }

        Song that = (Song) o;

        return mSongId == that.getSongId();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public int hashCode() {
        return Long.hashCode(mSongId);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(mSortKey);
        out.writeLong(mSongId);
        out.writeParcelable(mMetadata, flags);
    }

    public static final Creator<Song> CREATOR = new Creator<Song>() {
        public Song createFromParcel(Parcel in) {
            MediaMetadata metadata = in.readParcelable(null);
            long songId = in.readLong();
            long sortKey = in.readLong();
            return new Song(songId, metadata, sortKey);
        }

        public Song[] newArray(int size) {
            return new Song[size];
        }
    };
}
