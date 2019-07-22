package com.lrony.mediabrowsedemo.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class SharedPreferencesUtil {

    private static final String TAG = "SharedPreferencesUtil";

    private static final String SP_NAME = "UsbMediaCfg";

    private static final String K_MEDIA_BROWSE_ID = "K_MEDIA_BROWSE_ID";
    private static final String K_MUSIC_DATA = "K_MUSIC_DATA";
    private static final String K_MUSIC_POSITION = "K_MUSIC_POSITION";

    private static SharedPreferencesUtil mSelf;

    private SharedPreferences mPreferences;
    private SharedPreferences.Editor mEditor;

    private SharedPreferencesUtil() {
    }

    public static SharedPreferencesUtil getInstance() {
        if (mSelf == null) {
            mSelf = new SharedPreferencesUtil();
        }
        return mSelf;
    }

    public void initialize(Context context) {
        mPreferences = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        mEditor = mPreferences.edit();
    }

    public synchronized void putMediaBrowseId(String id) {
        Log.d(TAG, "putMediaBrowseId: " + id);
        mEditor.putString(K_MEDIA_BROWSE_ID, id);
        mEditor.commit();
    }

    public synchronized String getMediaBrowseId() {
        return mPreferences.getString(K_MEDIA_BROWSE_ID, "");
    }

    public synchronized void putMusicData(String data) {
        Log.d(TAG, "putMusicData: " + data);
        mEditor.putString(K_MUSIC_DATA, data);
        mEditor.commit();
    }

    public synchronized String getMusicData() {
        return mPreferences.getString(K_MUSIC_DATA, "");
    }

    public synchronized void putMusicPosition(long position) {
        Log.d(TAG, "putMusicPosition: " + position);
        mEditor.putLong(K_MUSIC_POSITION, position);
        mEditor.commit();
    }

    public synchronized long getMusicPosition() {
        return mPreferences.getLong(K_MUSIC_POSITION, 0);
    }
}
