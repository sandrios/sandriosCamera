package com.sandrios.sandriosCamera.internal.manager.impl;

import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import androidx.annotation.RestrictTo;
import androidx.exifinterface.media.ExifInterface;

import com.sandrios.sandriosCamera.internal.configuration.CameraConfiguration;
import com.sandrios.sandriosCamera.internal.configuration.ConfigurationProvider;
import com.sandrios.sandriosCamera.internal.manager.listener.CameraCloseListener;
import com.sandrios.sandriosCamera.internal.manager.listener.CameraOpenListener;
import com.sandrios.sandriosCamera.internal.manager.listener.CameraPhotoListener;
import com.sandrios.sandriosCamera.internal.manager.listener.CameraVideoListener;
import com.sandrios.sandriosCamera.internal.utils.CameraHelper;
import com.sandrios.sandriosCamera.internal.utils.Size;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Created by Arpit Gandhi on 8/14/16.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class Camera1Manager extends BaseCameraManager<Integer, SurfaceHolder.Callback>
        implements SurfaceHolder.Callback, Camera.PictureCallback {

    private static final String TAG = "Camera1Manager";
    private Camera camera;
    private Surface surface;
    private int orientation;
    private int displayRotation = 0;
    private boolean safeToTakePicture = false;

    private File outputPath;
    private CameraVideoListener videoListener;
    private CameraPhotoListener photoListener;

    public Camera1Manager() {

    }

    @Override
    public void openCamera(final Integer cameraId,
                           final CameraOpenListener<Integer, SurfaceHolder.Callback> cameraOpenListener) {
        this.currentCameraId = cameraId;
        backgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    camera = Camera.open(cameraId);
                    prepareCameraOutputs();
                    if (cameraOpenListener != null) {
                        uiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                cameraOpenListener.onCameraOpened(cameraId, previewSize, Camera1Manager.this);
                            }
                        });
                    }
                } catch (Exception error) {
                    Log.d(TAG, "Can't open camera: " + error.getMessage());
                    if (cameraOpenListener != null) {
                        uiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                cameraOpenListener.onCameraOpenError();
                            }
                        });
                    }
                }
            }
        });
    }

    @Override
    public void closeCamera(final CameraCloseListener<Integer> cameraCloseListener) {
        backgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                if (camera != null) {
                    camera.release();
                    camera = null;
                    if (cameraCloseListener != null) {
                        uiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                cameraCloseListener.onCameraClosed(currentCameraId);
                            }
                        });
                    }
                }
            }
        });
    }

    @Override
    public void takePhoto(File photoFile, CameraPhotoListener cameraPhotoListener) {
        this.outputPath = photoFile;
        this.photoListener = cameraPhotoListener;
        backgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                if (safeToTakePicture) {
                    setCameraPhotoQuality(camera);
                    camera.takePicture(null, null, Camera1Manager.this);
                    safeToTakePicture = false;
                }
            }
        });
    }

    @Override
    public void startVideoRecord(final File videoFile, CameraVideoListener cameraVideoListener) {
        if (isVideoRecording) return;

        this.outputPath = videoFile;
        this.videoListener = cameraVideoListener;

        if (videoListener != null)
            backgroundHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (prepareVideoRecorder()) {
                        videoRecorder.start();
                        isVideoRecording = true;
                        uiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                videoListener.onVideoRecordStarted(videoSize);
                            }
                        });
                    }
                }
            });
    }

    @Override
    public void stopVideoRecord() {
        if (isVideoRecording)
            backgroundHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (videoRecorder != null) videoRecorder.stop();
                    } catch (Exception ignore) {
                        // ignore illegal state.
                        // appear in case time or file size reach limit and stop already called.
                    }

                    isVideoRecording = false;
                    releaseVideoRecorder();

                    if (videoListener != null) {
                        uiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                videoListener.onVideoRecordStopped(outputPath);
                            }
                        });
                    }
                }
            });
    }

    @Override
    public void releaseCameraManager() {
        super.releaseCameraManager();
    }

    @Override
    public void initializeCameraManager(ConfigurationProvider configurationProvider, Context context) {
        super.initializeCameraManager(configurationProvider, context);

        numberOfCameras = Camera.getNumberOfCameras();

        for (int i = 0; i < numberOfCameras; ++i) {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();

            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                faceBackCameraId = i;
                faceBackCameraOrientation = cameraInfo.orientation;
            } else if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                faceFrontCameraId = i;
                faceFrontCameraOrientation = cameraInfo.orientation;
            }
        }
    }

    @Override
    public Size getPhotoSizeForQuality(@CameraConfiguration.MediaQuality int mediaQuality) {
        return CameraHelper.getPictureSize(Size.fromList(camera.getParameters().getSupportedPictureSizes()), mediaQuality);
    }

    @Override
    public void setFlashMode(@CameraConfiguration.FlashMode int flashMode) {
        setFlashMode(camera, camera.getParameters(), flashMode);
    }

    @Override
    protected void prepareCameraOutputs() {
        try {
            if (configurationProvider.getMediaQuality() == CameraConfiguration.MEDIA_QUALITY_AUTO) {
                camcorderProfile = CameraHelper.getCamcorderProfile(currentCameraId, configurationProvider.getVideoFileSize(), configurationProvider.getMinimumVideoDuration());
            } else
                camcorderProfile = CameraHelper.getCamcorderProfile(configurationProvider.getMediaQuality(), currentCameraId);

            List<Size> previewSizes = Size.fromList(camera.getParameters().getSupportedPreviewSizes());
            List<Size> pictureSizes = Size.fromList(camera.getParameters().getSupportedPictureSizes());
            List<Size> videoSizes;
            videoSizes = Size.fromList(camera.getParameters().getSupportedVideoSizes());

            videoSize = CameraHelper.getSizeWithClosestRatio(
                    (videoSizes == null || videoSizes.isEmpty()) ? previewSizes : videoSizes,
                    camcorderProfile.videoFrameWidth, camcorderProfile.videoFrameHeight);

            photoSize = CameraHelper.getPictureSize(
                    (pictureSizes == null || pictureSizes.isEmpty()) ? previewSizes : pictureSizes,
                    configurationProvider.getMediaQuality() == CameraConfiguration.MEDIA_QUALITY_AUTO
                            ? CameraConfiguration.MEDIA_QUALITY_HIGHEST : configurationProvider.getMediaQuality());

            if (configurationProvider.getMediaAction() == CameraConfiguration.MEDIA_ACTION_PHOTO
                    || configurationProvider.getMediaAction() == CameraConfiguration.MEDIA_ACTION_BOTH) {
                previewSize = CameraHelper.getSizeWithClosestRatio(previewSizes, photoSize.getWidth(), photoSize.getHeight());
            } else {
                previewSize = CameraHelper.getSizeWithClosestRatio(previewSizes, videoSize.getWidth(), videoSize.getHeight());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while setup camera sizes.");
        }
    }

    @Override
    protected boolean prepareVideoRecorder() {
        videoRecorder = new MediaRecorder();
        try {
            camera.lock();
            camera.unlock();
            videoRecorder.setCamera(camera);

            videoRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
            videoRecorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);

            videoRecorder.setOutputFormat(camcorderProfile.fileFormat);
            videoRecorder.setVideoFrameRate(camcorderProfile.videoFrameRate);
            videoRecorder.setVideoSize(videoSize.getWidth(), videoSize.getHeight());
            videoRecorder.setVideoEncodingBitRate(camcorderProfile.videoBitRate);
            videoRecorder.setVideoEncoder(camcorderProfile.videoCodec);

            videoRecorder.setAudioEncodingBitRate(camcorderProfile.audioBitRate);
            videoRecorder.setAudioChannels(camcorderProfile.audioChannels);
            videoRecorder.setAudioSamplingRate(camcorderProfile.audioSampleRate);
            videoRecorder.setAudioEncoder(camcorderProfile.audioCodec);

            videoRecorder.setOutputFile(outputPath.toString());

            if (configurationProvider.getVideoFileSize() > 0) {
                videoRecorder.setMaxFileSize(configurationProvider.getVideoFileSize());

                videoRecorder.setOnInfoListener(this);
            }
            if (configurationProvider.getVideoDuration() > 0) {
                videoRecorder.setMaxDuration(configurationProvider.getVideoDuration());

                videoRecorder.setOnInfoListener(this);
            }

            videoRecorder.setOrientationHint(getVideoOrientation(configurationProvider.getSensorPosition()));
            videoRecorder.setPreviewDisplay(surface);

            videoRecorder.prepare();

            return true;
        } catch (IllegalStateException error) {
            Log.e(TAG, "IllegalStateException preparing MediaRecorder: " + error.getMessage());
        } catch (IOException error) {
            Log.e(TAG, "IOException preparing MediaRecorder: " + error.getMessage());
        } catch (Throwable error) {
            Log.e(TAG, "Error during preparing MediaRecorder: " + error.getMessage());
        }

        releaseVideoRecorder();
        return false;
    }

    @Override
    protected void onMaxDurationReached() {
        stopVideoRecord();
    }

    @Override
    protected void onMaxFileSizeReached() {
        stopVideoRecord();
    }

    @Override
    protected void releaseVideoRecorder() {
        super.releaseVideoRecorder();

        try {
            camera.lock(); // lock camera for later use
        } catch (Exception ignore) {
        }
    }

    //------------------------Implementation------------------

    private void startPreview(SurfaceHolder surfaceHolder) {
        try {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(currentCameraId, cameraInfo);
            int cameraRotationOffset = cameraInfo.orientation;

            Camera.Parameters parameters = camera.getParameters();
            setAutoFocus(camera, parameters);
            setFlashMode(configurationProvider.getFlashMode());

            if (configurationProvider.getMediaAction() == CameraConfiguration.MEDIA_ACTION_PHOTO
                    || configurationProvider.getMediaAction() == CameraConfiguration.MEDIA_ACTION_BOTH)
                turnPhotoCameraFeaturesOn(camera, parameters);
            else if (configurationProvider.getMediaAction() == CameraConfiguration.MEDIA_ACTION_PHOTO
                    || configurationProvider.getMediaAction() == CameraConfiguration.MEDIA_ACTION_BOTH)
                turnVideoCameraFeaturesOn(camera, parameters);

            int rotation = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
            int degrees = 0;
            switch (rotation) {
                case Surface.ROTATION_0:
                    degrees = 0;
                    break; // Natural orientation
                case Surface.ROTATION_90:
                    degrees = 90;
                    break; // Landscape left
                case Surface.ROTATION_180:
                    degrees = 180;
                    break;// Upside down
                case Surface.ROTATION_270:
                    degrees = 270;
                    break;// Landscape right
            }

            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                displayRotation = (cameraRotationOffset + degrees) % 360;
                displayRotation = (360 - displayRotation) % 360; // compensate
            } else {
                displayRotation = (cameraRotationOffset - degrees + 360) % 360;
            }

            this.camera.setDisplayOrientation(displayRotation);

            if (Build.VERSION.SDK_INT > 14
                    && parameters.isVideoStabilizationSupported()
                    && (configurationProvider.getMediaAction() == CameraConfiguration.MEDIA_ACTION_VIDEO
                    || configurationProvider.getMediaAction() == CameraConfiguration.MEDIA_ACTION_BOTH)) {
                parameters.setVideoStabilization(true);
            }

            parameters.setPreviewSize(previewSize.getWidth(), previewSize.getHeight());
            parameters.setPictureSize(photoSize.getWidth(), photoSize.getHeight());

            camera.setParameters(parameters);
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
            safeToTakePicture = true;

        } catch (IOException error) {
            Log.d(TAG, "Error setting camera preview: " + error.getMessage());
        } catch (Exception ignore) {
            Log.d(TAG, "Error starting camera preview: " + ignore.getMessage());
        }
    }

    private void turnPhotoCameraFeaturesOn(Camera camera, Camera.Parameters parameters) {
        if (parameters.getSupportedFocusModes().contains(
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }
        camera.setParameters(parameters);
    }

    private void turnVideoCameraFeaturesOn(Camera camera, Camera.Parameters parameters) {
        if (parameters.getSupportedFocusModes().contains(
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }
        camera.setParameters(parameters);
    }

    private void setAutoFocus(Camera camera, Camera.Parameters parameters) {
        try {
            if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                camera.setParameters(parameters);
            }
        } catch (Exception ignore) {
        }
    }

    private void setFlashMode(Camera camera, Camera.Parameters parameters, @CameraConfiguration.FlashMode int flashMode) {
        try {
            switch (flashMode) {
                case CameraConfiguration.FLASH_MODE_AUTO:
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                    break;
                case CameraConfiguration.FLASH_MODE_ON:
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                    break;
                case CameraConfiguration.FLASH_MODE_OFF:
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    break;
                default:
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                    break;
            }
            camera.setParameters(parameters);
        } catch (Exception ignore) {
        }
    }


    private void setCameraPhotoQuality(Camera camera) {
        Camera.Parameters parameters = camera.getParameters();

        parameters.setPictureFormat(PixelFormat.JPEG);

        if (configurationProvider.getMediaQuality() == CameraConfiguration.MEDIA_QUALITY_LOW) {
            parameters.setJpegQuality(50);
        } else if (configurationProvider.getMediaQuality() == CameraConfiguration.MEDIA_QUALITY_MEDIUM) {
            parameters.setJpegQuality(75);
        } else if (configurationProvider.getMediaQuality() == CameraConfiguration.MEDIA_QUALITY_HIGH) {
            parameters.setJpegQuality(100);
        } else if (configurationProvider.getMediaQuality() == CameraConfiguration.MEDIA_QUALITY_HIGHEST) {
            parameters.setJpegQuality(100);
        }
        parameters.setPictureSize(photoSize.getWidth(), photoSize.getHeight());

        camera.setParameters(parameters);
    }

    @Override
    protected int getPhotoOrientation(@CameraConfiguration.SensorPosition int sensorPosition) {
        int rotate;
        if (currentCameraId.equals(faceFrontCameraId)) {
            rotate = (360 + faceFrontCameraOrientation + configurationProvider.getDegrees()) % 360;
        } else {
            rotate = (360 + faceBackCameraOrientation - configurationProvider.getDegrees()) % 360;
        }

        if (rotate == 0) {
            orientation = ExifInterface.ORIENTATION_NORMAL;
        } else if (rotate == 90) {
            orientation = ExifInterface.ORIENTATION_ROTATE_90;
        } else if (rotate == 180) {
            orientation = ExifInterface.ORIENTATION_ROTATE_180;
        } else if (rotate == 270) {
            orientation = ExifInterface.ORIENTATION_ROTATE_270;
        }

        return orientation;
    }

    @Override
    protected int getVideoOrientation(@CameraConfiguration.SensorPosition int sensorPosition) {
        int degrees = 0;
        switch (sensorPosition) {
            case CameraConfiguration.SENSOR_POSITION_UP:
                degrees = 0;
                break; // Natural orientation
            case CameraConfiguration.SENSOR_POSITION_LEFT:
                degrees = 90;
                break; // Landscape left
            case CameraConfiguration.SENSOR_POSITION_RIGHT:
                degrees = 270;
                break;// Landscape right
            case CameraConfiguration.SENSOR_POSITION_UNSPECIFIED:
                degrees = 0;
                break; // Natural orientation
        }

        int rotate;
        if (currentCameraId.equals(faceFrontCameraId)) {
            rotate = (360 + faceFrontCameraOrientation + degrees) % 360;
        } else {
            rotate = (360 + faceBackCameraOrientation - degrees) % 360;
        }
        return rotate;
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        if (surfaceHolder.getSurface() == null) {
            return;
        }

        surface = surfaceHolder.getSurface();

        try {
            camera.stopPreview();
        } catch (Exception ignore) {
        }

        startPreview(surfaceHolder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        if (surfaceHolder.getSurface() == null) {
            return;
        }

        surface = surfaceHolder.getSurface();

        try {
            camera.stopPreview();
        } catch (Exception ignore) {
        }

        startPreview(surfaceHolder);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    @Override
    public void onPictureTaken(byte[] bytes, Camera camera) {
        File pictureFile = outputPath;
        if (pictureFile == null) {
            Log.d(TAG, "Error creating media file, check storage permissions.");
            return;
        }

        try {
            FileOutputStream fileOutputStream = new FileOutputStream(pictureFile);
            fileOutputStream.write(bytes);
            fileOutputStream.close();
        } catch (FileNotFoundException error) {
            Log.e(TAG, "File not found: " + error.getMessage());
        } catch (IOException error) {
            Log.e(TAG, "Error accessing file: " + error.getMessage());
        } catch (Throwable error) {
            Log.e(TAG, "Error saving file: " + error.getMessage());
        }

        try {
            ExifInterface exif = new ExifInterface(pictureFile.getAbsolutePath());
            exif.setAttribute(ExifInterface.TAG_ORIENTATION, "" + getPhotoOrientation(configurationProvider.getSensorPosition()));
            exif.saveAttributes();

            if (photoListener != null) {
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        photoListener.onPhotoTaken(outputPath);
                    }
                });
            }
        } catch (Throwable error) {
            Log.e(TAG, "Can't save exif info: " + error.getMessage());
        }
    }
}
