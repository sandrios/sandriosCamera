package com.sandrios.sandriosCamera.internal;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.support.annotation.IntRange;
import android.support.annotation.RequiresPermission;

import com.sandrios.sandriosCamera.internal.configuration.SandriosCameraConfiguration;
import com.sandrios.sandriosCamera.internal.ui.camera.Camera1Activity;
import com.sandrios.sandriosCamera.internal.ui.camera2.Camera2Activity;
import com.sandrios.sandriosCamera.internal.utils.CameraHelper;

/**
 * Created by Arpit Gandhi on 7/6/16.
 */
public class SandriosCamera {

    private SandriosCameraConfiguration sandriosCameraConfiguration;

    /***
     * Creates SandriosCamera instance with default configuration set to photo with medium quality.
     *
     * @param activity    - fromList which request was invoked
     * @param requestCode - request code which will return in onActivityForResult
     */
    public SandriosCamera(Activity activity, @IntRange(from = 0) int requestCode) {
        SandriosCameraConfiguration.Builder builder = new SandriosCameraConfiguration.Builder(activity, requestCode);
        sandriosCameraConfiguration = builder.build();
    }

    /***
     * Creates SandriosCamera instance with custom camera configuration.
     *
     * @param cameraConfiguration
     */
    public SandriosCamera(SandriosCameraConfiguration cameraConfiguration) {
        this.sandriosCameraConfiguration = cameraConfiguration;
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    public void launchCamera() {
        if (sandriosCameraConfiguration == null || sandriosCameraConfiguration.getActivity() == null)
            return;

        Intent cameraIntent;

        if (CameraHelper.hasCamera2(sandriosCameraConfiguration.getActivity())) {
            cameraIntent = new Intent(this.sandriosCameraConfiguration.getActivity(), Camera2Activity.class);
        } else {
            cameraIntent = new Intent(this.sandriosCameraConfiguration.getActivity(), Camera1Activity.class);
        }

        cameraIntent.putExtra(SandriosCameraConfiguration.Arguments.REQUEST_CODE, sandriosCameraConfiguration.getRequestCode());

        if (sandriosCameraConfiguration.getMediaAction() > 0)
            cameraIntent.putExtra(SandriosCameraConfiguration.Arguments.MEDIA_ACTION, sandriosCameraConfiguration.getMediaAction());

        if (sandriosCameraConfiguration.getMediaQuality() > 0)
            cameraIntent.putExtra(SandriosCameraConfiguration.Arguments.MEDIA_QUALITY, sandriosCameraConfiguration.getMediaQuality());

        if (sandriosCameraConfiguration.getVideoDuration() > 0)
            cameraIntent.putExtra(SandriosCameraConfiguration.Arguments.VIDEO_DURATION, sandriosCameraConfiguration.getVideoDuration());

        if (sandriosCameraConfiguration.getVideoFileSize() > 0)
            cameraIntent.putExtra(SandriosCameraConfiguration.Arguments.VIDEO_FILE_SIZE, sandriosCameraConfiguration.getVideoFileSize());

        if (sandriosCameraConfiguration.getMinimumVideoDuration() > 0)
            cameraIntent.putExtra(SandriosCameraConfiguration.Arguments.MINIMUM_VIDEO_DURATION, sandriosCameraConfiguration.getMinimumVideoDuration());

        cameraIntent.putExtra(SandriosCameraConfiguration.Arguments.SHOW_PICKER, sandriosCameraConfiguration.shouldShowPicker());
        sandriosCameraConfiguration.getActivity().startActivityForResult(cameraIntent, sandriosCameraConfiguration.getRequestCode());

    }
}
