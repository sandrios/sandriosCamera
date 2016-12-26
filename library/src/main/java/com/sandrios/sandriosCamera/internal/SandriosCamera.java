package com.sandrios.sandriosCamera.internal;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.support.annotation.IntRange;

import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;
import com.sandrios.sandriosCamera.internal.configuration.SandriosCameraConfiguration;
import com.sandrios.sandriosCamera.internal.ui.camera.Camera1Activity;
import com.sandrios.sandriosCamera.internal.ui.camera2.Camera2Activity;
import com.sandrios.sandriosCamera.internal.utils.CameraHelper;

import java.util.ArrayList;

/**
 * Created by Arpit Gandhi on 7/6/16.
 */
public class SandriosCamera {

    private Activity activity;
    private int requestCode;
    boolean showPicker;

    /***
     * Creates SandriosCamera instance with default configuration set to photo with medium quality.
     *
     * @param activity    - fromList which request was invoked
     * @param requestCode - request code which will return in onActivityForResult
     */
    public SandriosCamera(Activity activity, @IntRange(from = 0) int requestCode, boolean showPicker) {
        this.activity = activity;
        this.requestCode = requestCode;
        this.showPicker = showPicker;
    }

    public void launchCamera() {
        new TedPermission(activity)
                .setPermissionListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted() {
                        launchIntent();
                    }

                    @Override
                    public void onPermissionDenied(ArrayList<String> deniedPermissions) {

                    }
                })
                .setPermissions(
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO)
                .check();
    }

    private void launchIntent() {
        Intent cameraIntent;
        if (CameraHelper.hasCamera2(activity)) {
            cameraIntent = new Intent(activity, Camera2Activity.class);
        } else {
            cameraIntent = new Intent(activity, Camera1Activity.class);
        }
        cameraIntent.putExtra(SandriosCameraConfiguration.Arguments.REQUEST_CODE, requestCode);
        cameraIntent.putExtra(SandriosCameraConfiguration.Arguments.SHOW_PICKER, showPicker);
        activity.startActivityForResult(cameraIntent, requestCode);
    }
}
