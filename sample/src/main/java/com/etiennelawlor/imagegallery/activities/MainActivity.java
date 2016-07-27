package com.etiennelawlor.imagegallery.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.etiennelawlor.imagegallery.R;
import com.etiennelawlor.imagegallery.library.ImageGalleryFragment;
import com.etiennelawlor.imagegallery.library.activities.FullScreenImageGalleryActivity;
import com.etiennelawlor.imagegallery.library.activities.ImageGalleryActivity;
import com.etiennelawlor.imagegallery.library.adapters.FullScreenImageGalleryAdapter;
import com.etiennelawlor.imagegallery.library.adapters.ImageGalleryAdapter;
import com.etiennelawlor.imagegallery.library.enums.PaletteColorType;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by etiennelawlor on 8/20/15.
 */
public class MainActivity extends AppCompatActivity implements ImageGalleryAdapter.ImageThumbnailLoader, FullScreenImageGalleryAdapter.FullScreenImageLoader, FullScreenImageGalleryAdapter.FullScreenImageDownloader {

    // region Member Variables
    private PaletteColorType paletteColorType;
    private static int MY_REQUEST_CODE = 123;
    private String imageUrlBeforePermissions;
    // endregion

    // region Listeners
    @OnClick(R.id.image_gallery_activity_btn)
    public void onImageGalleryActivityButtonClicked() {
        Intent intent = new Intent(MainActivity.this, ImageGalleryActivity.class);

        String[] images = getResources().getStringArray(R.array.unsplash_images);
        Bundle bundle = new Bundle();
        bundle.putStringArrayList(ImageGalleryActivity.KEY_IMAGES, new ArrayList<>(Arrays.asList(images)));
        bundle.putString(ImageGalleryActivity.KEY_TITLE, "Unsplash Images");
        intent.putExtras(bundle);

        startActivity(intent);
    }

    @OnClick(R.id.image_gallery_fragment_btn)
    public void onImageGalleryFragmentButtonClicked() {
        Intent intent = new Intent(MainActivity.this, HostImageGalleryActivity.class);

        String[] images = getResources().getStringArray(R.array.unsplash_images);
        Bundle bundle = new Bundle();
        bundle.putStringArrayList(ImageGalleryFragment.KEY_IMAGES, new ArrayList<>(Arrays.asList(images)));
        bundle.putString(ImageGalleryActivity.KEY_TITLE, "Unsplash Images");
        intent.putExtras(bundle);

        startActivity(intent);
    }
    // endregion

    // region Lifecycle Methods
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        ImageGalleryActivity.setImageThumbnailLoader(this);
        ImageGalleryFragment.setImageThumbnailLoader(this);
        FullScreenImageGalleryActivity.setFullScreenImageLoader(this);
        FullScreenImageGalleryActivity.setFullScreenImageDownloader(this);
        FullScreenImageGalleryActivity.setShowIconDownload(true);

        // optionally set background color using Palette for full screen images
        paletteColorType = PaletteColorType.VIBRANT;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == MY_REQUEST_CODE) {
            if (permissions.length == 1 && permissions[0].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                savePhoto(imageUrlBeforePermissions);
            } else {
                // Permission was denied. Display an error message.
            }
        }
    }
    // endregion

    // region ImageGalleryAdapter.ImageThumbnailLoader Methods
    @Override
    public void loadImageThumbnail(ImageView iv, String imageUrl, int dimension) {
        if (!TextUtils.isEmpty(imageUrl)) {
            Picasso.with(iv.getContext())
                    .load(imageUrl)
                    .resize(dimension, dimension)
                    .centerCrop()
                    .into(iv);
        } else {
            iv.setImageDrawable(null);
        }
    }
    // endregion

    // region FullScreenImageGalleryAdapter.FullScreenImageLoader
    @Override
    public void loadFullScreenImage(final ImageView iv, String imageUrl, int width, final LinearLayout bgLinearLayout) {
        if (!TextUtils.isEmpty(imageUrl)) {
            Picasso.with(iv.getContext())
                    .load(imageUrl)
                    .resize(width, 0)
                    .into(iv, new Callback() {
                        @Override
                        public void onSuccess() {
                            Bitmap bitmap = ((BitmapDrawable) iv.getDrawable()).getBitmap();
                            Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
                                public void onGenerated(Palette palette) {
                                    applyPalette(palette, bgLinearLayout);
                                }
                            });
                        }

                        @Override
                        public void onError() {

                        }
                    });
        } else {
            iv.setImageDrawable(null);
        }
    }

    // region FullScreenImageGalleryAdapter.FullScreenImageDownloader
    @Override
    public void downloadFullScreenImage(String imageUrl) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            savePhoto(imageUrl);
        }
        else{
            imageUrlBeforePermissions = imageUrl;
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_REQUEST_CODE);
        }
    }
    // endregion

    // region Helper Methods
    private void applyPalette(Palette palette, LinearLayout bgLinearLayout){
        int bgColor = getBackgroundColor(palette);
        if (bgColor != -1)
            bgLinearLayout.setBackgroundColor(bgColor);
    }

    private int getBackgroundColor(Palette palette) {
        int bgColor = -1;

        int vibrantColor = palette.getVibrantColor(0x000000);
        int lightVibrantColor = palette.getLightVibrantColor(0x000000);
        int darkVibrantColor = palette.getDarkVibrantColor(0x000000);

        int mutedColor = palette.getMutedColor(0x000000);
        int lightMutedColor = palette.getLightMutedColor(0x000000);
        int darkMutedColor = palette.getDarkMutedColor(0x000000);

        if (paletteColorType != null) {
            switch (paletteColorType) {
                case VIBRANT:
                    if (vibrantColor != 0) { // primary option
                        bgColor = vibrantColor;
                    } else if (lightVibrantColor != 0) { // fallback options
                        bgColor = lightVibrantColor;
                    } else if (darkVibrantColor != 0) {
                        bgColor = darkVibrantColor;
                    } else if (mutedColor != 0) {
                        bgColor = mutedColor;
                    } else if (lightMutedColor != 0) {
                        bgColor = lightMutedColor;
                    } else if (darkMutedColor != 0) {
                        bgColor = darkMutedColor;
                    }
                    break;
                case LIGHT_VIBRANT:
                    if (lightVibrantColor != 0) { // primary option
                        bgColor = lightVibrantColor;
                    } else if (vibrantColor != 0) { // fallback options
                        bgColor = vibrantColor;
                    } else if (darkVibrantColor != 0) {
                        bgColor = darkVibrantColor;
                    } else if (mutedColor != 0) {
                        bgColor = mutedColor;
                    } else if (lightMutedColor != 0) {
                        bgColor = lightMutedColor;
                    } else if (darkMutedColor != 0) {
                        bgColor = darkMutedColor;
                    }
                    break;
                case DARK_VIBRANT:
                    if (darkVibrantColor != 0) { // primary option
                        bgColor = darkVibrantColor;
                    } else if (vibrantColor != 0) { // fallback options
                        bgColor = vibrantColor;
                    } else if (lightVibrantColor != 0) {
                        bgColor = lightVibrantColor;
                    } else if (mutedColor != 0) {
                        bgColor = mutedColor;
                    } else if (lightMutedColor != 0) {
                        bgColor = lightMutedColor;
                    } else if (darkMutedColor != 0) {
                        bgColor = darkMutedColor;
                    }
                    break;
                case MUTED:
                    if (mutedColor != 0) { // primary option
                        bgColor = mutedColor;
                    } else if (lightMutedColor != 0) { // fallback options
                        bgColor = lightMutedColor;
                    } else if (darkMutedColor != 0) {
                        bgColor = darkMutedColor;
                    } else if (vibrantColor != 0) {
                        bgColor = vibrantColor;
                    } else if (lightVibrantColor != 0) {
                        bgColor = lightVibrantColor;
                    } else if (darkVibrantColor != 0) {
                        bgColor = darkVibrantColor;
                    }
                    break;
                case LIGHT_MUTED:
                    if (lightMutedColor != 0) { // primary option
                        bgColor = lightMutedColor;
                    } else if (mutedColor != 0) { // fallback options
                        bgColor = mutedColor;
                    } else if (darkMutedColor != 0) {
                        bgColor = darkMutedColor;
                    } else if (vibrantColor != 0) {
                        bgColor = vibrantColor;
                    } else if (lightVibrantColor != 0) {
                        bgColor = lightVibrantColor;
                    } else if (darkVibrantColor != 0) {
                        bgColor = darkVibrantColor;
                    }
                    break;
                case DARK_MUTED:
                    if (darkMutedColor != 0) { // primary option
                        bgColor = darkMutedColor;
                    } else if (mutedColor != 0) { // fallback options
                        bgColor = mutedColor;
                    } else if (lightMutedColor != 0) {
                        bgColor = lightMutedColor;
                    } else if (vibrantColor != 0) {
                        bgColor = vibrantColor;
                    } else if (lightVibrantColor != 0) {
                        bgColor = lightVibrantColor;
                    } else if (darkVibrantColor != 0) {
                        bgColor = darkVibrantColor;
                    }
                    break;
                default:
                    break;
            }
        }

        return bgColor;
    }
    // endregion

    public void savePhoto(final String imageUrl){

        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            Log.e("SaveFile", "No External Storage!");
            return;
        }

        if (!TextUtils.isEmpty(imageUrl)) {

            FullScreenImageGalleryActivity.showDownloadingFile();
            new AsyncTask<String, String, Bitmap>(){

                @Override
                protected Bitmap doInBackground(String... params) {
                    try {
                        Bitmap bitmap = Picasso.with(MainActivity.this).load(imageUrl).get();
                        if(bitmap!=null){
                            try {
                                //create a file to write bitmap data
                                final String mFileName = getCacheDir().getAbsolutePath() + "/" + FilenameUtils.getName(imageUrl);
                                File mediafile =new File(mFileName);

                                //Convert bitmap to byte array
                                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                                bitmap.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
                                byte[] bitmapdata = bos.toByteArray();

                                //write the bytes in file
                                FileOutputStream fos = new FileOutputStream(mFileName);
                                fos.write(bitmapdata);
                                fos.flush();
                                fos.close();

                                //save the file
                                String targetPath = Environment.getExternalStoragePublicDirectory(
                                        Environment.DIRECTORY_PICTURES) + "/" + getResources().getString(R.string.app_name);
                                File dir = new File(targetPath);
                                if (!dir.mkdir() && !dir.isDirectory()) {
                                    Log.e("SaveFile", "Directory not created");
                                }
                                targetPath += "/" + FilenameUtils.getName(imageUrl);
                                dir = new File(targetPath);
                                Log.d("SaveFile", "dir " + dir);
                                try {
                                    FileUtils.copyFile(mediafile, dir);
                                } catch(IOException ex){
                                    Log.e("Save File", "IOException" + ex.getMessage());
                                }

                                //rescan gallery
                                Uri contentUri = Uri.fromFile(dir);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
                                    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                                    mediaScanIntent.setData(contentUri);
                                    sendBroadcast(mediaScanIntent);
                                }
                                else{
                                    sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, contentUri));
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        return bitmap;
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                }

                protected void onPostExecute(Bitmap bitmap) {
                    if(bitmap!=null) {
                        Toast.makeText(MainActivity.this, getResources().getString(R.string.file_saved), Toast.LENGTH_SHORT).show();
                    }
                    FullScreenImageGalleryActivity.hideDownloadingFile();
                }

            }.execute("");
        }
    }

}
