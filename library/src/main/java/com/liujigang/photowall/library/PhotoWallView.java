package com.liujigang.photowall.library;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;


import java.util.ArrayList;
import java.util.Random;

/**
 * Created by liujigang on 2014/12/25 0025.
 */
public class PhotoWallView extends ScrollView {

    private int columnCount = 3;
    private int columnWidth;
    private ArrayList<LinearLayout> mList;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 11:
                    addImageView();
                    handler.sendEmptyMessageDelayed(11, 15000);
                    break;
            }
        }
    };
    private int p = 0;

    private void addImageView() {
        for (int i = p * 20; i < 20 + p * 20; i++) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -1);
            ImageView imageView = new ImageView(getContext());
            imageView.setLayoutParams(params);
            imageView.setScaleType(ImageView.ScaleType.FIT_XY);
            imageView.setPadding(5, 5, 5, 5);

            mList.get(i % 3).addView(imageView);
        }
        if (p > Images.imageUrls.length / 20)
            p = 0;
        p++;
    }

    public PhotoWallView(Context context) {
        super(context);
        init();
    }

    public PhotoWallView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PhotoWallView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        post(new Runnable() {
            @Override
            public void run() {
                Context context = getContext();
                columnWidth = getWidth() / columnCount;

                LinearLayout rootLinearLayout = new LinearLayout(context);
                rootLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
                mList = new ArrayList<>();
                for (int i = 0; i < columnCount; i++) {
                    LinearLayout itemLinearLayout = new LinearLayout(context);
                    itemLinearLayout.setOrientation(LinearLayout.VERTICAL);
                    itemLinearLayout.setLayoutParams(new LinearLayout.LayoutParams(columnWidth, -2));
                    rootLinearLayout.addView(itemLinearLayout);
                    mList.add(itemLinearLayout);
                    addItemView(itemLinearLayout);
                }
                PhotoWallView.this.removeAllViews();
                PhotoWallView.this.addView(rootLinearLayout, new ScrollView.LayoutParams(-1, -2));
            }
        });
        //联网拿第一批数据
        handler.sendEmptyMessageDelayed(11, 100);
    }

    private void addItemView(LinearLayout itemLinearLayout) {
        Random random = new Random();
        int count = random.nextInt(20) + 1;
        for (int i = 0; i < count; i++) {
            ImageView imageView = new ImageView(getContext());
            imageView.setImageResource(R.drawable.ic_launcher);
            itemLinearLayout.addView(imageView);
        }
    }


}
