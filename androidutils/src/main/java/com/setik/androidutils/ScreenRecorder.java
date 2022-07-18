package com.setik.androidutils;


import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.RequiresApi;

import com.hbisoft.hbrecorder.*;
import com.unity3d.player.UnityPlayer;
import com.unity3d.player.UnityPlayerActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ScreenRecorder implements HBRecorderListener {

    public static ScreenRecorder instance = new ScreenRecorder();

    private String saveFolder = "ScreenRecorder";
    HBRecorder hbRecorder;
    ContentValues contentValues;
    Uri mUri;
    ContentResolver resolver;
    boolean customSetting = false;

    @Override
    public void HBRecorderOnStart() {
        UnityPlayer.UnitySendMessage("ScreenRecorder", "SuccessCallback", "start_record");
    }

    @Override
    public void HBRecorderOnComplete() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //Update gallery depending on SDK Level
            if (hbRecorder.wasUriSet()) {
                updateGalleryUri();
            } else {
                refreshGalleryFile();
            }
        }
        UnityPlayer.UnitySendMessage("ScreenRecorder", "SuccessCallback", "complete_record");
    }

    @Override
    public void HBRecorderOnError(int errorCode, String reason) {
        UnityPlayer.UnitySendMessage("ScreenRecorder", "ErrorCallback", reason);
    }

    public static void Initialize() {
        instance.init();
    }

    public static void StartRecord(String saveFolder) {
        instance.setUpSaveFolder(saveFolder);
        instance.startRecording();
    }

    public static void StopRecord() {
        instance.stopRecording();
    }

    public static boolean isRecording() {
        return instance.hbRecorder.isBusyRecording();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void refreshGalleryFile() {
        MediaScannerConnection.scanFile(UnityPlayer.currentActivity,
                new String[]{hbRecorder.getFilePath()}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("ExternalStorage", "Scanned " + path + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);
                    }
                });
    }

    private void updateGalleryUri() {
        contentValues.clear();
        contentValues.put(MediaStore.Video.Media.IS_PENDING, 0);
        UnityPlayer.currentActivity.getContentResolver().update(mUri, contentValues, null, null);
    }

    private void init() {
        hbRecorder = new HBRecorder(UnityPlayer.currentActivity, this);
    }

    private void setUpSaveFolder(String folderName) {
        this.saveFolder = folderName;
    }

    private void setupVideo(int width, int height, int bitRate, int fps, boolean audioEnabled) {
        hbRecorder.enableCustomSettings();
        hbRecorder.setScreenDimensions(height, width);
        hbRecorder.setVideoFrameRate(fps);
        hbRecorder.setVideoBitrate(bitRate);
        hbRecorder.setAudioBitrate(128000);
        hbRecorder.setAudioSamplingRate(44100);
        hbRecorder.isAudioEnabled(audioEnabled);
        customSetting = true;
    }
    private void setupVideo(int width, int height, int bitRate, int fps, boolean audioEnabled, String encoder) {
        hbRecorder.enableCustomSettings();
        hbRecorder.setScreenDimensions(height, width);
        hbRecorder.setVideoFrameRate(fps);
        hbRecorder.setVideoBitrate(bitRate);
        hbRecorder.setVideoEncoder(encoder);
        hbRecorder.setAudioBitrate(128000);
        hbRecorder.setAudioSamplingRate(44100);
        hbRecorder.isAudioEnabled(audioEnabled);
        customSetting = true;
    }

    private void startRecording() {
        if (!customSetting)
            quickSettings();

        Intent requestIntent = new Intent(UnityPlayer.currentActivity, RequestScreenRecordActivity.class);
        UnityPlayer.currentActivity.startActivity(requestIntent);
    }

    private void stopRecording() {
        hbRecorder.stopScreenRecording();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private  void quickSettings() {
        hbRecorder.setAudioBitrate(128000);
        hbRecorder.setAudioSamplingRate(44100);
        hbRecorder.recordHDVideo(false);
        hbRecorder.isAudioEnabled(true);
        hbRecorder.setNotificationTitle("Recording your screen");
        hbRecorder.setNotificationDescription("Drag down to stop the recording");
    }

    public void startRecording_internal(int resultCode, Intent data) {
        setOutputPath();
        hbRecorder.startScreenRecording(data, resultCode, UnityPlayer.currentActivity);
    }

    //For Android 10> we will pass a Uri to HBRecorder
    //This is not necessary - You can still use getExternalStoragePublicDirectory
    //But then you will have to add android:requestLegacyExternalStorage="true" in your Manifest
    //IT IS IMPORTANT TO SET THE FILE NAME THE SAME AS THE NAME YOU USE FOR TITLE AND DISPLAY_NAME
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setOutputPath() {
        String filename = generateFileName();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            resolver = UnityPlayer.currentActivity.getContentResolver();
            contentValues = new ContentValues();
            contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/" + saveFolder);
            contentValues.put(MediaStore.Video.Media.TITLE, filename);
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
            mUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues);
            //FILE NAME SHOULD BE THE SAME
            hbRecorder.setFileName(filename);
            hbRecorder.setOutputUri(mUri);
        } else {
            createFolder();
            hbRecorder.setOutputPath(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES) + saveFolder);
        }
    }
    //Create Folder
    //Only call this on Android 9 and lower (getExternalStoragePublicDirectory is deprecated)
    //This can still be used on Android 10> but you will have to add android:requestLegacyExternalStorage="true" in your Manifest
    private void createFolder() {
        File f1 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), saveFolder);
        if (!f1.exists()) {
            if (f1.mkdirs()) {
                Log.i("Folder ", "created");
            }
        }
    }

    //Generate a timestamp to be used as a file name
    private String generateFileName() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault());
        Date curDate = new Date(System.currentTimeMillis());
        return formatter.format(curDate).replace(" ", "");
    }
}
