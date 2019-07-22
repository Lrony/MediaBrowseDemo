package com.lrony.mediabrowsedemo;

import android.app.Application;

import com.lrony.mediabrowsedemo.utils.SharedPreferencesUtil;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferencesUtil.getInstance().initialize(this);
    }
}
