package com.tangledbytes.androidrelib;

import android.app.Application;

public class ReApp extends Application {
    public static final String TAG = ReApp.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
        Init.init(this);
    }

}
