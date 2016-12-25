package com.sandrios.sandriosCamera.internal.manager.listener;

/**
 * Created by Arpit Gandhi on 8/14/16.
 */
public interface CameraCloseListener<CameraId> {
    void onCameraClosed(CameraId closedCameraId);
}
