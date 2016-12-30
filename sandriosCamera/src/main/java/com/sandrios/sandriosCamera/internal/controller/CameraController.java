package com.sandrios.sandriosCamera.internal.controller;

import android.os.Bundle;

import com.sandrios.sandriosCamera.internal.configuration.CameraConfiguration;
import com.sandrios.sandriosCamera.internal.manager.CameraManager;

import java.io.File;

/**
 * Created by Arpit Gandhi on 7/6/16.
 */
public interface CameraController<CameraId> {

    void onCreate(Bundle savedInstanceState);

    void onResume();

    void onPause();

    void onDestroy();

    void takePhoto();

    void startVideoRecord();

    void stopVideoRecord();

    boolean isVideoRecording();

    void switchCamera(@CameraConfiguration.CameraFace int cameraFace);

    void switchQuality();

    int getNumberOfCameras();

    @CameraConfiguration.MediaAction
    int getMediaAction();

    CameraId getCurrentCameraId();

    File getOutputFile();

    CameraManager getCameraManager();

    void setFlashMode(@CameraConfiguration.FlashMode int flashMode);

}
