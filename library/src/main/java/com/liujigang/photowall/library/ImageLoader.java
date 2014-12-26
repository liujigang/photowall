package com.liujigang.photowall.library;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.util.LruCache;
import android.widget.AbsListView;

/**
 * 对图片进行管理的工具类。
 *
 * @author Tony
 */
@SuppressLint("NewApi")
public class ImageLoader {

    /**
     * 图片缓存技术的核心类，用于缓存所有下载好的图片，在程序内存达到设定值时会将最少最近使用的图片移除掉。
     */
    private static LruCache<String, Bitmap> mMemoryCache;

    /**
     * ImageLoader的实例。
     */
    private static ImageLoader mImageLoader;

    private ImageLoader() {
        // 获取应用程序最大可用内存
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheSize = maxMemory / 8;
        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount();
            }

            @Override
            protected synchronized void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
                oldValue.recycle();
                Log.i("ImageLoader", "内存中移除图片");
                oldValue = null;
//                if (evicted) {
//                    Log.i("ImageLoader", "内存不足,移除图片");
//                    oldValue.recycle();
//                    oldValue = null;
//                }
            }

        };
    }

    /**
     * 获取ImageLoader的实例。
     *
     * @return ImageLoader的实例。
     */
    public static ImageLoader getInstance() {
        if (mImageLoader == null) {
            mImageLoader = new ImageLoader();
        }
        return mImageLoader;
    }

    /**
     * 将一张图片存储到LruCache中。
     *
     * @param key    LruCache的键，这里传入图片的URL地址。
     * @param bitmap LruCache的键，这里传入从网络上下载的Bitmap对象。
     */
    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemoryCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    /**
     * 从LruCache中获取一张图片，如果不存在就返回null。
     *
     * @param key LruCache的键，这里传入图片的URL地址。
     * @return 对应传入键的Bitmap对象，或者null。
     */
    public Bitmap getBitmapFromMemoryCache(String key) {
        return mMemoryCache.get(key);
    }

    public static int calculateInSampleSize(BitmapFactory.Options options,
                                            int reqWidth) {
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (width > reqWidth) {
            inSampleSize = Math.round((float) width / (float) reqWidth);
        }
        return inSampleSize;
    }

    public static Bitmap decodeSampledBitmapFromResource(String pathName,int reqWidth) {
        // 第一次解析将inJustDecodeBounds设置为true，来获取图片大小
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(pathName, options);
        // 调用上面定义的方法计算inSampleSize值
        options.inSampleSize = calculateInSampleSize(options, reqWidth);
        // 使用获取到的inSampleSize值再次解析图片
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(pathName, options);
    }

    public void clearMemoryCache() {
        if (mMemoryCache != null) {
            mMemoryCache.evictAll();
        }
    }

}
