package com.liujigang.photowall;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Window;

import com.liujigang.photowall.library.WallScrollView;


public class MainActivity extends ActionBarActivity {

    private WallScrollView mWallScrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mWallScrollView = (WallScrollView) findViewById(R.id.sc_wallScrollView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mWallScrollView.destroy();
        mWallScrollView = null;
    }
}