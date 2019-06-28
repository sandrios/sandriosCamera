package com.sandrios.sandriosCamera.internal;

import android.app.Activity;
import android.content.Intent;

import com.sandrios.sandriosCamera.internal.configuration.CameraConfiguration;
import com.sandrios.sandriosCamera.internal.ui.camera.Camera1Activity;
import com.sandrios.sandriosCamera.internal.ui.camera2.Camera2Activity;
import com.sandrios.sandriosCamera.internal.utils.CameraHelper;

/**
 * Sandrios Camera Builder Class
 * Created by Arpit Gandhi on 7/6/16.
 */
public class SandriosCamera {

    public static int RESULT_CODE = 956;
    public static String MEDIA = "media";
    private static SandriosCamera mInstance = null;
    private int mediaAction = CameraConfiguration.MEDIA_ACTION_BOTH;
    private boolean showPicker = true;
    private boolean autoRecord = false;
    private boolean enableImageCrop = false;
    private long videoSize = -1;

    public static SandriosCamera with() {
        if (mInstance == null) {
            mInstance = new SandriosCamera();
        }
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

    @SuppressWarnings("SameParameterValue")
    public SandriosCamera setVideoFileSize(int fileSize) {
        this.videoSize = fileSize;
        return mInstance;
    }

    /**
     * Only works if Media Action is set to Video
     */
    public SandriosCamera setAutoRecord() {
        autoRecord = true;
        return mInstance;
    }

    public void launchCamera(Activity activity) {
        if (CameraHelper.hasCamera(activity)) {
            Intent cameraIntent;
            if (CameraHelper.hasCamera2(activity)) {
                cameraIntent = new Intent(activity, Camera2Activity.class);
            } else {
                cameraIntent = new Intent(activity, Camera1Activity.class);
            }
            cameraIntent.putExtra(CameraConfiguration.Arguments.SHOW_PICKER, showPicker);
            cameraIntent.putExtra(CameraConfiguration.Arguments.MEDIA_ACTION, mediaAction);
            cameraIntent.putExtra(CameraConfiguration.Arguments.ENABLE_CROP, enableImageCrop);
            cameraIntent.putExtra(CameraConfiguration.Arguments.AUTO_RECORD, autoRecord);

            if (videoSize > 0) {
                cameraIntent.putExtra(CameraConfiguration.Arguments.VIDEO_FILE_SIZE, videoSize * 1024 * 1024);
            }
            activity.startActivityForResult(cameraIntent, RESULT_CODE);
        }
    }

    public class MediaType {
        public static final int PHOTO = 0;
        public static final int VIDEO = 1;
    }
}
