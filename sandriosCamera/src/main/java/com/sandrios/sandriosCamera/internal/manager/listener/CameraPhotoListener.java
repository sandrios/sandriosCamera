package com.sandrios.sandriosCamera.internal.manager.listener;

import androidx.annotation.RestrictTo;

import java.io.File;

/**
 * Created by Arpit Gandhi on 8/14/16.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface CameraPhotoListener {
    void onPhotoTaken(File photoFile);

    void onPhotoTakeError();
}
