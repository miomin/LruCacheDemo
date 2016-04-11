package scu.miomin.com.lrucachedemo;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity {
    private GridView mGridView;
    private List<String> datas;
    private Toolbar mToolbar;
    private PhotoAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.v("zxy", "cache:" + getCacheDir().getPath());
        Log.v("zxy", "Excache:" + getExternalCacheDir().getPath());
        initDatas();

        mGridView = (GridView) findViewById(R.id.gridView);
        mAdapter = new PhotoAdapter(this, mGridView, datas);
        mGridView.setAdapter(mAdapter);
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(MainActivity.this, "position=" + position + ",id=" + id, Toast.LENGTH_SHORT).show();
            }
        });

    }

    public void initDatas() {
        datas = new ArrayList<>();
        for (int i = 0; i < 55; i++) {
            datas.add(URLDatasTools.imageUrls[i]);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAdapter.cancelAllTask();//取消所有下载任务
    }
}
