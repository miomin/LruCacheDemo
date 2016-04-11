package scu.miomin.com.lrucachedemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class PhotoAdapter extends BaseAdapter implements AbsListView.OnScrollListener {

    // 从network下载图片的线程集合
    private List<ImageDownloadTask> mDownloadTaskList;
    private LruCache<String, Bitmap> mLruCache;

    // 引用外部的变量
    private WeakReference<GridView> mGridView;
    private WeakReference<List<String>> urls;
    private WeakReference<Context> mContext;


    // 可见项的第一项的index
    private int mFirstVisibleIndex;

    // 可见项的个数
    private int mVisibleItemCount;

    // 是不是第一次打开Activity
    private boolean isFirstOpen = true;

    public PhotoAdapter(Context context, GridView mGridView, List<String> urls) {
        this.mContext = new WeakReference<Context>(context);
        this.urls = new WeakReference<List<String>>(urls);
        this.mGridView = new WeakReference<GridView>(mGridView);
        this.mGridView.get().setOnScrollListener(this);
        mDownloadTaskList = new ArrayList<>();
        // 初始化图片缓存池
        initCache();
    }

    private void initCache() {

        // 获取应用的max heap size
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        // Android官方教学文档推荐LruCache的size为heap size的1/8
        int cacheSize = maxMemory / 8;

        mLruCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                if (bitmap != null) {
                    return bitmap.getByteCount() / 1024;
                }
                return 0;
            }
        };
    }

    @Override
    public int getCount() {
        return urls.get().size();
    }

    @Override
    public Object getItem(int position) {
        return urls.get().get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        viewHolder holder = null;

        if (convertView == null) {
            convertView = LayoutInflater.from(mContext.get()).inflate(R.layout.layout_item, parent, false);
            holder = new viewHolder();
            holder.mImageView = (ImageView) convertView.findViewById(R.id.imageView);
            holder.mTextView = (TextView) convertView.findViewById(R.id.textView);
            convertView.setTag(holder);
        } else {
            holder = (viewHolder) convertView.getTag();
        }

        String url = urls.get().get(position);
        //imageview与url绑定，防止错乱显示
        holder.mImageView.setTag(MD5Tools.decodeString(url));
        holder.mTextView.setText("第" + position + "项");

        if (!holder.mImageView.getTag().equals(url)) {
            showImageView(holder.mImageView, url);
        }

        return convertView;
    }

    /**
     * convertView复用
     */
    private class viewHolder {
        ImageView mImageView;
        TextView mTextView;
    }

    /**
     * 给ImageView设置Bitmap
     */
    private void showImageView(ImageView imageView, String url) {

        // 对url进行md5编码
        String key = MD5Tools.decodeString(url);
        // 先从cache中找bitmap缓存
        Bitmap bitmap = get(key);

        if (bitmap != null) {
            // 如果缓存命中
            imageView.setImageBitmap(bitmap);
        } else {
            // 如果cache miss
            imageView.setBackgroundResource(R.color.color_five);
        }
    }

    /**
     * 将Bitmap put 到 cache中
     */
    private void put(String key, Bitmap bitmap) {

        if (get(key) == null) {
            mLruCache.put(key, bitmap);
        }
    }

    /**
     * 在Cache中查找bitmap，如果miss则返回null
     */
    private Bitmap get(String key) {
        return mLruCache.get(key);
    }

    /**
     * 从网络下载图片
     */
    private Bitmap loadBitmap(String urlStr) {

        HttpURLConnection connection = null;
        Bitmap bitmap = null;
        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setDoInput(true);
            connection.connect();
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream mInputStream = connection.getInputStream();
                bitmap = BitmapFactory.decodeStream(mInputStream);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return bitmap;
    }

    /**
     * 取消所有的下载任务
     */
    public void cancelAllTask() {

        if (mDownloadTaskList != null) {
            for (int i = 0; i < mDownloadTaskList.size(); i++) {
                mDownloadTaskList.get(i).cancel(true);
            }
        }
    }

    /**
     * 加载可见项的图片
     */
    private void loadVisibleBitmap(int mFirstVisibleItem, int mVisibleItemCount) {

        for (int i = mFirstVisibleItem; i < mFirstVisibleItem + mVisibleItemCount; i++) {
            final String url = urls.get().get(i);
            String key = MD5Tools.decodeString(url);
            Bitmap bitmap = get(key);
            ImageView mImageView;
            if (bitmap != null) {
                //缓存中存在该图片的话就设置给ImageView
                mImageView = (ImageView) mGridView.get().findViewWithTag(MD5Tools.decodeString(url));
                if (mImageView != null) {
                    mImageView.setImageBitmap(bitmap);
                }
            } else {
                //不存在的话就开启一个异步线程去下载
                ImageDownloadTask task = new ImageDownloadTask(this);
                mDownloadTaskList.add(task);
                task.execute(url);
            }
        }
    }

    /**
     * 从网络下载图片的异步task
     */
    static class ImageDownloadTask extends AsyncTask<String, Void, Bitmap> {

        private String url;
        private WeakReference<PhotoAdapter> photoAdapter;

        public ImageDownloadTask(PhotoAdapter photoAdapter) {
            this.photoAdapter = new WeakReference<PhotoAdapter>(photoAdapter);
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            //在后台开始下载图片
            url = params[0];
            Bitmap bitmap = photoAdapter.get().loadBitmap(url);
            if (bitmap != null) {
                //把下载好的图片放入LruCache中
                String key = MD5Tools.decodeString(url);
                photoAdapter.get().put(key, bitmap);
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            //把下载好的图片显示出来
            ImageView mImageView = (ImageView) photoAdapter.get().mGridView.get().findViewWithTag(MD5Tools.decodeString(url));
            if (mImageView != null && bitmap != null) {
                mImageView.setImageBitmap(bitmap);
                photoAdapter.get().mDownloadTaskList.remove(this);//把下载好的任务移除
            }
        }
    }

    /**
     * 监听GridView的滑动状态
     */
    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

        //GridView停止滑动时，去加载可见项的图片
        if (scrollState == SCROLL_STATE_IDLE) {
            loadVisibleBitmap(mFirstVisibleIndex, mVisibleItemCount);
        } else {
            //GridView开始滑动时，取消所有加载任务
            cancelAllTask();
        }
    }

    /**
     * 监听并更新GridView滑动过程中的可见项
     */
    @Override
    public void onScroll(AbsListView view, int firstVisibleIndex, int visibleItemCount, int totalItemCount) {

        mFirstVisibleIndex = firstVisibleIndex;
        mVisibleItemCount = visibleItemCount;

        // 第一次打开，加载可见项
        if (isFirstOpen && visibleItemCount > 0) {
            loadVisibleBitmap(mFirstVisibleIndex, mVisibleItemCount);
            isFirstOpen = false;
        }
    }
}
