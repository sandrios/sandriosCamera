package com.sandrios.sandriosCamera.internal.configuration;

import com.sandrios.sandriosCamera.internal.ui.view.FlashSwitchView;

/**
 * Created by Arpit Gandhi on 7/6/16.
 */
public interface ConfigurationProvider {

    int getRequestCode();

    @SandriosCameraConfiguration.MediaAction
    int getMediaAction();

    @SandriosCameraConfiguration.MediaQuality
    int getMediaQuality();

    int getVideoDuration();

    long getVideoFileSize();

    @SandriosCameraConfiguration.SensorPosition
    int getSensorPosition();

    int getDegrees();

    int getMinimumVideoDuration();

    @SandriosCameraConfiguration.FlashMode
    int getFlashMode();

}
