package com.anywherecommerce.android.sdk.sampleapp;

import android.app.Activity;
import android.app.Application;

import com.anywherecommerce.android.sdk.SDKManager;

import java.lang.reflect.Field;
import java.util.Map;

public class AppManager extends Application {

    public void onCreate()
    {
        super.onCreate();

        // The first step should always be to initialize the SDK.
        SDKManager.initialize(this);
    }
}
