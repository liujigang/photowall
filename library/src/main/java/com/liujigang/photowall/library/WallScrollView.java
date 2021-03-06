package com.liujigang.photowall.library;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 自定义的ScrollView，在其中动态地对图片进行添加。
 *
 * @author guolin
 */
public class WallScrollView extends ScrollView implements OnTouchListener {

    /**
     * 每页要加载的图片数量
     */
    public int PAGE_SIZE = 20;

    /**
     * 记录当前已加载到第几页
     */
    private int page;

    /**
     * 每一列的宽度
     */
    private int columnWidth;
    /**
     * 所有的子孩子
     */
    private List<LinearLayout> list;

    private boolean loadOnce;

    /**
     * 对图片进行管理的工具类
     */
    private ImageLoader imageLoader;

    /**
     * 记录所有正在下载或等待下载的任务。
     */
    private static Set<LoadImageTask> taskCollection;

    /**
     * MyScrollView下的直接子布局。
     */
    private static ViewGroup scrollLayout;

    /**
     * MyScrollView布局的高度。
     */
    private static int scrollViewHeight;

    /**
     * 记录上垂直方向的滚动距离。
     */
    private static int lastScrollY = -1;

    /**
     * 记录所有界面上的图片，用以可以随时控制对图片的释放。
     */
    private List<ImageView> imageViewList = new ArrayList<>();

    /**
     * 在Handler中进行图片可见性检查的判断，以及加载更多图片的操作。
     */
    private Handler handler = new Handler() {

        public void handleMessage(Message msg) {
            int scrollY = WallScrollView.this.getScrollY();
            if (scrollY == lastScrollY) {
                if (scrollViewHeight + scrollY >= scrollLayout.getHeight() && taskCollection.isEmpty()) {
//                    toMoreChild();
                    WallScrollView.this.loadMoreImages();
                }
                WallScrollView.this.checkVisibility();
            } else {
                lastScrollY = scrollY;
                handler.sendEmptyMessageDelayed(10, 10);
            }
        }
    };
//    @Override
//    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
//        super.onScrollChanged(l, t, oldl, oldt);
//        Log.d("ImageLoader", "滚动状态发生变化");
//        int scrollY = getScrollY();
//        if (scrollViewHeight + scrollY >= scrollLayout.getHeight() && taskCollection.isEmpty()) {
//            toMoreChild();
//            loadMoreImages();
//        }
//        checkVisibility();
//    }

    private synchronized void toMoreChild() {
        if (imageViewList.size() < 50)
            return;

        for (int i = 0; i < list.size(); i++) {
            list.get(i).removeAllViews();
        }
        imageViewList.clear();
        taskCollection.clear();
    }

    public WallScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        imageLoader = ImageLoader.getInstance();
        taskCollection = new HashSet<>();
        setOnTouchListener(this);
    }

    /**
     * 进行一些关键性的初始化操作，获取MyScrollView的高度，以及得到第一列的宽度值。并在这里开始加载第一页的图片。
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (changed && !loadOnce) {
            scrollViewHeight = getHeight();
            scrollLayout = (ViewGroup) getChildAt(0);

            list = new ArrayList<>();
            int childCount = scrollLayout.getChildCount();
            for (int i = 0; i < childCount; i++) {
                LinearLayout childAt = (LinearLayout) scrollLayout.getChildAt(i);
                if (childAt.getVisibility() == VISIBLE)
                    list.add(childAt);
            }
            childCount = list.size();
            columnLength = new int[childCount];
            columnWidth = (getWidth() - getPaddingLeft() - getPaddingRight()) / childCount;
            loadOnce = true;
            loadMoreImages();
        }
    }

    /**
     * 监听用户的触屏事件，如果用户手指离开屏幕则开始进行滚动检测。
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            handler.sendEmptyMessage(10);
        }
        return false;
    }

    /**
     * 开始加载下一页的图片，每张图片都会开启一个异步线程去下载。
     */
    public void loadMoreImages() {
        if (hasSDCard()) {

            int startIndex = page * PAGE_SIZE;
            int endIndex = page * PAGE_SIZE + PAGE_SIZE;
            if (startIndex < Images.imageUrls.length) {

                Toast.makeText(getContext(), "正在加载...", Toast.LENGTH_SHORT).show();

                endIndex = endIndex > Images.imageUrls.length ? Images.imageUrls.length : endIndex;

                for (int i = startIndex; i < endIndex; i++) {
                    LoadImageTask task = new LoadImageTask();
                    taskCollection.add(task);
                    task.execute(Images.imageUrls[i]);
                }
                page++;
            } else {
                Toast.makeText(getContext(), "已没有更多图片", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(), "未发现SD卡", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 遍历imageViewList中的每张图片，对图片的可见性进行检查，如果图片已经离开屏幕可见范围，则将图片替换成一张空图。
     */
    public void checkVisibility() {
        for (int i = 0; i < imageViewList.size(); i++) {
            ImageView imageView = imageViewList.get(i);
            int borderTop = (Integer) imageView.getTag(R.string.border_top);
            int borderBottom = (Integer) imageView.getTag(R.string.border_bottom);
            if (borderBottom > getScrollY() && borderTop < getScrollY() + scrollViewHeight) {
                String imageUrl = (String) imageView.getTag(R.string.image_url);
                Bitmap bitmap = imageLoader.getBitmapFromMemoryCache(imageUrl);
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap);
                } else {
                    LoadImageTask task = new LoadImageTask(imageView);
                    task.execute(imageUrl);
                }
            } else {
                imageView.setImageResource(R.drawable.empty_photo);
            }
        }
    }

    /**
     * 判断手机是否有SD卡。
     *
     * @return 有SD卡返回true，没有返回false。
     */
    private boolean hasSDCard() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    private int[] columnLength;


    /** ------------------------------------------------------------------------------------------- */
    /**
     * 异步下载图片的任务。
     *
     * @author guolin
     */
    class LoadImageTask extends AsyncTask<String, Void, Bitmap> {

        /**
         * 图片的URL地址
         */
        private String mImageUrl;

        /**
         * 可重复使用的ImageView
         */
        private ImageView mImageView;

        public LoadImageTask() {
        }

        /**
         * 将可重复使用的ImageView传入
         *
         * @param imageView 要加载的控件
         */
        public LoadImageTask(ImageView imageView) {
            mImageView = imageView;
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            mImageUrl = params[0];
            Bitmap imageBitmap = imageLoader.getBitmapFromMemoryCache(mImageUrl);
            if (imageBitmap == null) {
                imageBitmap = loadImage(mImageUrl);
            }
            return imageBitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                Log.d("ImageLoader", "添加了一张图片");
                double ratio = bitmap.getWidth() / (columnWidth * 1.0);
                int scaledHeight = (int) (bitmap.getHeight() / ratio);
                addImage(bitmap, columnWidth, scaledHeight);
            }
            taskCollection.remove(this);
        }

        /**
         * 根据传入的URL，对图片进行加载。如果这张图片已经存在于SD卡中，则直接从SD卡里读取，否则就从网络上下载
         *
         * @param imageUrl 图片的URL地址
         * @return 加载到内存的图片
         */
        private Bitmap loadImage(String imageUrl) {
            File imageFile = new File(getImagePath(imageUrl));
            if (!imageFile.exists()) {
                downloadImage(imageUrl);
            }
            if (imageUrl != null) {
                Bitmap bitmap = imageLoader.getBitmapFromMemoryCache(imageFile
                        .getPath());
                if (bitmap == null)
                    bitmap = ImageLoader.decodeSampledBitmapFromResource(imageFile.getPath(), columnWidth);
                if (bitmap != null) {
                    imageLoader.addBitmapToMemoryCache(imageUrl, bitmap);
                    return bitmap;
                }
            }
            return null;
        }

        /**
         * 向ImageView中添加一张图片
         *
         * @param bitmap      待添加的图片
         * @param imageWidth  图片的宽度
         * @param imageHeight 图片的高度
         */
        private void addImage(Bitmap bitmap, int imageWidth, int imageHeight) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(imageWidth, imageHeight);
            if (mImageView != null) {
                mImageView.setImageBitmap(bitmap);
            } else {
                ImageView imageView = new ImageView(getContext());
                LinearLayout linearLayout = findColumnToAdd(imageView, imageHeight);
                if (linearLayout != null) {
                    imageView.setLayoutParams(params);
                    imageView.setImageBitmap(bitmap);
                    imageView.setScaleType(ScaleType.FIT_XY);
                    imageView.setPadding(5, 5, 5, 5);
                    imageView.setTag(R.string.image_url, mImageUrl);
                    linearLayout.addView(imageView);
                    imageViewList.add(imageView);
                }
                imageView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(getContext(),"点击了一些",Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        private int getMinHeight() {
            int temp = columnLength[0];
            int index = 0;
            for (int i = 1; i < columnLength.length; i++) {
                if (temp > columnLength[i]) {
                    temp = columnLength[i];
                    index = i;
                }
            }
            return index;
        }

        /**
         * 找到此时应该添加图片的一列。原则就是对三列的高度进行判断，当前高度最小的一列就是应该添加的一列。
         *
         * @param imageView
         * @param imageHeight
         * @return 应该添加图片的一列
         */
        private LinearLayout findColumnToAdd(ImageView imageView, int imageHeight) {
            int size = list.size();
            if (list == null || size < 1) {
                return null;
            }
            int i = getMinHeight();
            LinearLayout linearLayout = list.get(i);

            imageView.setTag(R.string.border_top, columnLength[i]);
            columnLength[i] += imageHeight;
            imageView.setTag(R.string.border_bottom, columnLength[i]);
            return linearLayout;
        }

        /**
         * 将图片下载到SD卡缓存起来。
         *
         * @param imageUrl 图片的URL地址。
         */
        private void downloadImage(String imageUrl) {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                Log.d("TAG", "monted sdcard");
            } else {
                Log.d("TAG", "has no sdcard");
            }
            FileOutputStream fos;
            BufferedOutputStream bos = null;
            BufferedInputStream bis = null;
            File imageFile = null;
            try {
                HttpClient client = new DefaultHttpClient();
                HttpGet get = new HttpGet(Uri.encode(imageUrl, "@#&=*+-_.,:!?()/~'%"));
                HttpResponse response = client.execute(get);
                HttpEntity entity = response.getEntity();
                int code = response.getStatusLine().getStatusCode();
                if (code == 200) {
                    bis = new BufferedInputStream(entity.getContent());
                    imageFile = new File(getImagePath(imageUrl));
                    fos = new FileOutputStream(imageFile);
                    bos = new BufferedOutputStream(fos);
                    byte[] b = new byte[1024 * 10];
                    int length;
                    while ((length = bis.read(b)) != -1) {
                        bos.write(b, 0, length);
                        bos.flush();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (bis != null) {
                        bis.close();
                    }
                    if (bos != null) {
                        bos.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (imageFile != null) {
                Bitmap bitmap = ImageLoader.decodeSampledBitmapFromResource(
                        imageFile.getPath(), columnWidth);
                if (bitmap != null) {
                    imageLoader.addBitmapToMemoryCache(imageUrl, bitmap);
                }
            }
        }

        /**
         * 获取图片的本地存储路径。
         *
         * @param imageUrl 图片的URL地址
         * @return 图片的本地存储路径
         */
        private String getImagePath(String imageUrl) {
            int lastSlashIndex = imageUrl.lastIndexOf("/");
            String imageName = imageUrl.substring(lastSlashIndex + 1);
            String imageDir = Environment.getExternalStorageDirectory().getPath() + "/PhotoWallFalls/";
            File file = new File(imageDir);
            if (!file.exists()) {
                file.mkdirs();
            }
            String imagePath = imageDir + imageName;
            return imagePath;
        }
    }

    /**
     * -------------------------------------------------------------------------------------------
     */

    public void destroy() {
        imageViewList.clear();
        imageViewList = null;
        taskCollection.clear();
        taskCollection = null;
        imageLoader.clearMemoryCache();
    }

}