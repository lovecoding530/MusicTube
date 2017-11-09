package com.bhagathsing.android.mytube;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;

public class MyApplication extends MultiDexApplication {
    // Called when the application is starting, before any other application objects have been created.
    // Overriding this method is totally optional!
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        MyApplication.context = getApplicationContext();
        // Required initialization logic here!
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        MultiDex.install(this);
    }

    public static Context getAppContext() {
        return MyApplication.context;
    }
}