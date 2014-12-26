package com.liujigang.photowall.library;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.AbsListView;
import android.widget.ListAdapter;

/**
 * Created by liujigang on 2014/12/26 0026.
 */
public class MyAbsListView extends AbsListView {
    public MyAbsListView(Context context) {
        super(context);
    }

    public MyAbsListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyAbsListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public ListAdapter getAdapter() {
        return null;
    }

    @Override
    public void setSelection(int position) {

    }
}
