package com.sandrios.sandriosCamera.internal;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.support.annotation.IntRange;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.sandrios.sandriosCamera.internal.configuration.CameraConfiguration;
import com.sandrios.sandriosCamera.internal.ui.camera.Camera1Activity;
import com.sandrios.sandriosCamera.internal.ui.camera2.Camera2Activity;
import com.sandrios.sandriosCamera.internal.utils.CameraHelper;

import java.util.List;

/**
 * Sandrios Camera Builder Class
 * Created by Arpit Gandhi on 7/6/16.
 */
public class SandriosCamera {

    private SandriosCamera mInstance = null;
    private Activity mActivity;
    private int requestCode;
    private int mediaAction = CameraConfiguration.MEDIA_ACTION_BOTH;
    private boolean showPicker = true;
    private int type = 501;
    private boolean enableImageCrop = false;
    private long videoSize = -1;

    /***
     * Creates SandriosCamera instance with default configuration set to both.
     *
     * @param activity - fromList which request was invoked
     * @param code     - request code which will return in onActivityForResult
     */
    public SandriosCamera(Activity activity, @IntRange(from = 0) int code) {
        mInstance = this;
        mActivity = activity;
        requestCode = code;
    }

    public SandriosCamera setShowPickerType(int type) {
        this.type = type;
        return mInstance;
    }

    public SandriosCamera setShowPicker(boolean showPicker) {
        this.showPicker = showPicker;
        return mInstance;
    }

    public SandriosCamera setMediaAction(int mediaAction) {
        this.mediaAction = mediaAction;
        return mInstance;
    }

    public SandriosCamera enableImageCropping(boolean enableImageCrop) {
        this.enableImageCrop = enableImageCrop;
        return mInstance;
    }

    public SandriosCamera setVideoFileSize(int fileSize) {
        this.videoSize = fileSize;
        return mInstance;
    }

    public void launchCamera() {

        MultiplePermissionsListener listener = new MultiplePermissionsListener() {
            @Override
            public void onPermissionsChecked(MultiplePermissionsReport report) {
                if(report.areAllPermissionsGranted()) {
                    launchIntent();
                }
            }

            @Override
            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken token) {
                token.continuePermissionRequest();
            }
        };

        Dexter.withActivity(mActivity)
                .withPermissions(
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO)
                .withListener(listener).check();
    }

    private void launchIntent() {
        if (CameraHelper.hasCamera(mActivity)) {
            Intent cameraIntent;
            if (CameraHelper.hasCamera2(mActivity)) {
                cameraIntent = new Intent(mActivity, Camera2Activity.class);
            } else {
                cameraIntent = new Intent(mActivity, Camera1Activity.class);
            }
            cameraIntent.putExtra(CameraConfiguration.Arguments.REQUEST_CODE, requestCode);
            cameraIntent.putExtra(CameraConfiguration.Arguments.SHOW_PICKER, showPicker);
            cameraIntent.putExtra(CameraConfiguration.Arguments.PICKER_TYPE, type);
            cameraIntent.putExtra(CameraConfiguration.Arguments.MEDIA_ACTION, mediaAction);
            cameraIntent.putExtra(CameraConfiguration.Arguments.ENABLE_CROP, enableImageCrop);
            if (videoSize > 0) {
                cameraIntent.putExtra(CameraConfiguration.Arguments.VIDEO_FILE_SIZE, videoSize * 1024 * 1024);
            }
            mActivity.startActivityForResult(cameraIntent, requestCode);
        }
    }
}
