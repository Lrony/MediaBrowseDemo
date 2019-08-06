package com.lrony.mediabrowsedemo.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

public class BootCompletedReceiver extends BroadcastReceiver {

    private static final String TAG = "BootCompletedReceiver";

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive recevie boot completed.");
        Intent startIntent = new Intent();
        startIntent.setAction("com.android.music.ACTION_PLAY_HISTORY");
        startIntent.setPackage("com.lrony.mediabrowsedemo");
        context.startForegroundService(startIntent);
    }
}
