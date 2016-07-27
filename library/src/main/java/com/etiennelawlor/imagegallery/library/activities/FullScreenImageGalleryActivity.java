package com.etiennelawlor.imagegallery.library.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.etiennelawlor.imagegallery.library.R;
import com.etiennelawlor.imagegallery.library.adapters.FullScreenImageGalleryAdapter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FullScreenImageGalleryActivity extends AppCompatActivity implements FullScreenImageGalleryAdapter.FullScreenImageLoader {

    // region Constants
    public static final String KEY_IMAGES = "KEY_IMAGES";
    public static final String KEY_POSITION = "KEY_POSITION";
    // endregion

    // region Views
    private Toolbar toolbar;
    private ViewPager viewPager;
    // endregion

    private static MenuItem loading;
    private static MenuItem downloading;

    // region Member Variables
    private List<String> images;
    private int position;
    private static FullScreenImageGalleryAdapter.FullScreenImageLoader fullScreenImageLoader;
    private static FullScreenImageGalleryAdapter.FullScreenImageDownloader fullScreenImageDownloader;
    private static boolean showIconDownload;
    // endregion

    // region Listeners
    private final ViewPager.OnPageChangeListener viewPagerOnPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            if (viewPager != null) {
                viewPager.setCurrentItem(position);

                setActionBarTitle(position);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };
    // endregion

    // region Lifecycle Methods
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_full_screen_image_gallery);

        bindViews();

        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);


        Intent intent = getIntent();
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                images = extras.getStringArrayList(KEY_IMAGES);
                position = extras.getInt(KEY_POSITION);
            }
        }

        setUpViewPager();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeListeners();
    }
    // endregion

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_image, menu);
        if (showIconDownload)
            menu.getItem(0).setVisible(true);

        downloading = menu.getItem(0);

        loading = menu.add(0, 1, 0, "");
        loading.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        loading.setActionView(R.layout.action_bar_progress);
        loading.setVisible(false);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        else if (item.getItemId() == R.id.action_download){
            fullScreenImageDownloader.downloadFullScreenImage(images.get(position));
            return true;
        }
        else {
            return super.onOptionsItemSelected(item);
        }
    }

    // region FullScreenImageGalleryAdapter.FullScreenImageLoader Methods
    @Override
    public void loadFullScreenImage(ImageView iv, String imageUrl, int width, LinearLayout bglinearLayout) {
        fullScreenImageLoader.loadFullScreenImage(iv, imageUrl, width, bglinearLayout);
    }
    // endregion

    // region Helper Methods
    private void bindViews() {
        viewPager = (ViewPager) findViewById(R.id.vp);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
    }

    private void setUpViewPager() {
        ArrayList<String> imageList = new ArrayList<>();
        imageList.addAll(images);

        FullScreenImageGalleryAdapter fullScreenImageGalleryAdapter = new FullScreenImageGalleryAdapter(imageList);
        fullScreenImageGalleryAdapter.setFullScreenImageLoader(this);
        viewPager.setAdapter(fullScreenImageGalleryAdapter);
        viewPager.addOnPageChangeListener(viewPagerOnPageChangeListener);
        viewPager.setCurrentItem(position);

        setActionBarTitle(position);
    }

    private void setActionBarTitle(int position) {
        if (viewPager != null && images.size() > 1) {
            int totalPages = viewPager.getAdapter().getCount();
            this.position = position;
            ActionBar actionBar = getSupportActionBar();
            if(actionBar != null){
                actionBar.setTitle(String.format("%d/%d", (position + 1), totalPages));
            }
        }
    }

    private void removeListeners() {
        viewPager.removeOnPageChangeListener(viewPagerOnPageChangeListener);
    }

    public static void setFullScreenImageLoader(FullScreenImageGalleryAdapter.FullScreenImageLoader loader) {
        fullScreenImageLoader = loader;
    }

    public static void setFullScreenImageDownloader(FullScreenImageGalleryAdapter.FullScreenImageDownloader loader){
        fullScreenImageDownloader = loader;
    }

    public static void setShowIconDownload(boolean showIcon){
        showIconDownload = showIcon;
    }

    public static void showDownloadingFile(){
        loading.setVisible(true);
        downloading.setVisible(false);
    }

    public static void hideDownloadingFile(){
        loading.setVisible(false);
        downloading.setVisible(true);
    }

    // endregion
}
