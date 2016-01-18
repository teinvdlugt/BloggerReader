package com.teinvdlugt.android.bloggerreader;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;


public abstract class CustomTabsActivity extends AppCompatActivity {

    protected CustomTabsHelper tabsHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tabsHelper = new CustomTabsHelper();
    }

    @Override
    protected void onStart() {
        super.onStart();
        tabsHelper.bindService(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        tabsHelper.unbindService(this);
    }
}
