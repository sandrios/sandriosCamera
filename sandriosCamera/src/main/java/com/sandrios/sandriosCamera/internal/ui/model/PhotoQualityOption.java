package com.sandrios.sandriosCamera.internal.ui.model;

import com.sandrios.sandriosCamera.internal.configuration.CameraConfiguration;
import com.sandrios.sandriosCamera.internal.utils.Size;

/**
 * Created by Arpit Gandhi on 12/1/16.
 */

public class PhotoQualityOption implements CharSequence {

    @CameraConfiguration.MediaQuality
    private int mediaQuality;
    private String title;

    public PhotoQualityOption(@CameraConfiguration.MediaQuality int mediaQuality, Size size) {
        this.mediaQuality = mediaQuality;

        title = String.valueOf(size.getWidth()) + " x " + String.valueOf(size.getHeight());
    }

    @CameraConfiguration.MediaQuality
    public int getMediaQuality() {
        return mediaQuality;
    }

    @Override
    public int length() {
        return title.length();
    }

    @Override
    public char charAt(int index) {
        return title.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return title.subSequence(start, end);
    }

    @Override
    public String toString() {
        return title;
    }
}
