package com.sandrios.sandriosCamera.internal.configuration;

/**
 * Created by Arpit Gandhi on 7/6/16.
 */
public interface ConfigurationProvider {

    int getRequestCode();

    @CameraConfiguration.MediaAction
    int getMediaAction();

    @CameraConfiguration.MediaQuality
    int getMediaQuality();

    int getVideoDuration();

    long getVideoFileSize();

    @CameraConfiguration.SensorPosition
    int getSensorPosition();

    int getDegrees();

    int getMinimumVideoDuration();

    @CameraConfiguration.FlashMode
    int getFlashMode();

}
