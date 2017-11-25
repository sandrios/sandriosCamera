package com.sandrios.sandriosCamera.internal.manager.listener;

import android.support.annotation.RestrictTo;

import com.sandrios.sandriosCamera.internal.utils.Size;

/**
 * Created by Arpit Gandhi on 8/14/16.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface CameraOpenListener<CameraId, SurfaceListener> {
    void onCameraOpened(CameraId openedCameraId, Size previewSize, SurfaceListener surfaceListener);

    void onCameraOpenError();
}
