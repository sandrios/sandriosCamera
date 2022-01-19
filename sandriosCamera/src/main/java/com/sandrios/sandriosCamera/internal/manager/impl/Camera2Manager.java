package com.sandrios.sandriosCamera.internal.manager.impl;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.sandrios.sandriosCamera.internal.configuration.CameraConfiguration;
import com.sandrios.sandriosCamera.internal.configuration.ConfigurationProvider;
import com.sandrios.sandriosCamera.internal.manager.listener.CameraCloseListener;
import com.sandrios.sandriosCamera.internal.manager.listener.CameraOpenListener;
import com.sandrios.sandriosCamera.internal.manager.listener.CameraPhotoListener;
import com.sandrios.sandriosCamera.internal.manager.listener.CameraVideoListener;
import com.sandrios.sandriosCamera.internal.utils.CameraHelper;
import com.sandrios.sandriosCamera.internal.utils.ImageSaver;
import com.sandrios.sandriosCamera.internal.utils.Size;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Created by Arpit Gandhi on 8/9/16.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public final class Camera2Manager extends BaseCameraManager<String, TextureView.SurfaceTextureListener>
        implements ImageReader.OnImageAvailableListener, TextureView.SurfaceTextureListener {

    private final static String TAG = "Camera2Manager";
    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAITING_LOCK = 1;
    private static final int STATE_WAITING_PRE_CAPTURE = 2;
    private static final int STATE_WAITING_NON_PRE_CAPTURE = 3;
    private static final int STATE_PICTURE_TAKEN = 4;
    private CameraOpenListener<String, TextureView.SurfaceTextureListener> cameraOpenListener;
    private CameraPhotoListener cameraPhotoListener;
    private CameraVideoListener cameraVideoListener;
    private File outputPath;
    @CameraPreviewState
    private int previewState = STATE_PREVIEW;
    private CameraManager manager;
    private CameraDevice cameraDevice;
    private CaptureRequest previewRequest;
    private CaptureRequest.Builder previewRequestBuilder;
    private CameraCaptureSession captureSession;
    private CameraCharacteristics frontCameraCharacteristics;
    private CameraCharacteristics backCameraCharacteristics;
    private StreamConfigurationMap frontCameraStreamConfigurationMap;
    private StreamConfigurationMap backCameraStreamConfigurationMap;
    private SurfaceTexture texture;
    private Surface workingSurface;
    private ImageReader imageReader;
    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            Camera2Manager.this.cameraDevice = cameraDevice;
            if (cameraOpenListener != null) {
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!TextUtils.isEmpty(currentCameraId) && previewSize != null)
                            cameraOpenListener.onCameraOpened(currentCameraId, previewSize, Camera2Manager.this);
                    }
                });
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            cameraDevice.close();
            Camera2Manager.this.cameraDevice = null;

            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    cameraOpenListener.onCameraOpenError();
                }
            });
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            cameraDevice.close();
            Camera2Manager.this.cameraDevice = null;

            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    cameraOpenListener.onCameraOpenError();
                }
            });
        }
    };
    private CameraCaptureSession.CaptureCallback captureCallback
            = new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            processCaptureResult(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            processCaptureResult(result);
        }

    };

    public Camera2Manager() {

    }

    @Override
    public void initializeCameraManager(ConfigurationProvider configurationProvider, Context context) {
        super.initializeCameraManager(configurationProvider, context);

        this.manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);

        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        windowSize = new Size(size.x, size.y);

        try {
            String[] ids = manager.getCameraIdList();
            numberOfCameras = ids.length;
            for (String id : ids) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(id);

                int orientation = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (orientation == CameraCharacteristics.LENS_FACING_FRONT) {
                    faceFrontCameraId = id;
                    faceFrontCameraOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                    frontCameraCharacteristics = characteristics;
                } else {
                    faceBackCameraId = id;
                    faceBackCameraOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                    backCameraCharacteristics = characteristics;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during camera init");
        }
    }

    @Override
    public void openCamera(String cameraId, final CameraOpenListener<String, TextureView.SurfaceTextureListener> cameraOpenListener) {
        this.currentCameraId = cameraId;
        this.cameraOpenListener = cameraOpenListener;
        backgroundHandler.post(new Runnable() {
            @SuppressLint("MissingPermission")
            @Override
            public void run() {
                if (context == null || configurationProvider == null) {
                    if (cameraOpenListener != null) {
                        uiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                cameraOpenListener.onCameraOpenError();
                            }
                        });
                    }
                    return;
                }
                prepareCameraOutputs();
                try {
                    manager.openCamera(currentCameraId, stateCallback, backgroundHandler);
                } catch (Exception e) {
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
    public void closeCamera(final CameraCloseListener<String> cameraCloseListener) {
        backgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                closeCamera();
                if (cameraCloseListener != null) {
                    uiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            cameraCloseListener.onCameraClosed(currentCameraId);
                        }
                    });
                }
            }
        });
    }

    @Override
    public void setFlashMode(@CameraConfiguration.FlashMode int flashMode) {
        setFlashModeAndBuildPreviewRequest(flashMode);
    }

    @Override
    public void takePhoto(File photoFile, CameraPhotoListener cameraPhotoListener) {
        this.outputPath = photoFile;
        this.cameraPhotoListener = cameraPhotoListener;

        backgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                lockFocus();
            }
        });

    }

    @Override
    public Size getPhotoSizeForQuality(@CameraConfiguration.MediaQuality int mediaQuality) {
        StreamConfigurationMap map = currentCameraId.equals(faceBackCameraId) ? backCameraStreamConfigurationMap : frontCameraStreamConfigurationMap;
        return CameraHelper.getPictureSize(Size.fromArray2(map.getOutputSizes(ImageFormat.JPEG)), mediaQuality);
    }

    @Override
    public void startVideoRecord(File videoFile, final CameraVideoListener cameraVideoListener) {
        if (isVideoRecording || texture == null) return;

        this.outputPath = videoFile;
        this.cameraVideoListener = cameraVideoListener;

        if (cameraVideoListener != null)
            backgroundHandler.post(new Runnable() {
                @Override
                public void run() {
                    closePreviewSession();
                    if (prepareVideoRecorder()) {

                        SurfaceTexture texture = Camera2Manager.this.texture;
                        texture.setDefaultBufferSize(videoSize.getWidth(), videoSize.getHeight());

                        try {
                            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
                            List<Surface> surfaces = new ArrayList<>();

                            Surface previewSurface = workingSurface;
                            surfaces.add(previewSurface);
                            previewRequestBuilder.addTarget(previewSurface);

                            workingSurface = videoRecorder.getSurface();
                            surfaces.add(workingSurface);
                            previewRequestBuilder.addTarget(workingSurface);

                            cameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                                @Override
                                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                                    captureSession = cameraCaptureSession;

                                    previewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                                    try {
                                        captureSession.setRepeatingRequest(previewRequestBuilder.build(), null, backgroundHandler);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }

                                    try {
                                        videoRecorder.start();
                                    } catch (Exception ignore) {
                                        Log.e(TAG, "videoRecorder.start(): ", ignore);
                                    }

                                    isVideoRecording = true;

                                    uiHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            cameraVideoListener.onVideoRecordStarted(videoSize);
                                        }
                                    });
                                }

                                @Override
                                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                                    Log.d(TAG, "onConfigureFailed");
                                }
                            }, backgroundHandler);
                        } catch (Exception e) {
                            Log.e(TAG, "startVideoRecord: ", e);
                        }
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
                    closePreviewSession();

                    if (videoRecorder != null) {
                        try {
                            videoRecorder.stop();
                        } catch (Exception ignore) {
                        }
                    }
                    isVideoRecording = false;
                    releaseVideoRecorder();

                    if (cameraVideoListener != null) {
                        uiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                cameraVideoListener.onVideoRecordStopped(outputPath);
                            }
                        });
                    }
                }
            });
    }

    private void startPreview(SurfaceTexture texture) {
        try {
            if (texture == null) return;

            this.texture = texture;

            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());

            workingSurface = new Surface(texture);

            previewRequestBuilder
                    = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(workingSurface);

            cameraDevice.createCaptureSession(Arrays.asList(workingSurface, imageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            updatePreview(cameraCaptureSession);
                        }

                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            Log.d(TAG, "Fail while starting preview: ");
                        }
                    }, null);
        } catch (Exception e) {
            Log.e(TAG, "Error while preparing surface for preview: ", e);
        }
    }

    //--------------------Internal methods------------------

    @Override
    protected void onMaxDurationReached() {
        stopVideoRecord();
    }

    @Override
    protected void onMaxFileSizeReached() {
        stopVideoRecord();
    }

    @Override
    protected int getPhotoOrientation(@CameraConfiguration.SensorPosition int sensorPosition) {
        return getVideoOrientation(sensorPosition);
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
                break;
        }

        int rotate;
        if (Objects.equals(currentCameraId, faceFrontCameraId)) {
            rotate = (360 + faceFrontCameraOrientation + degrees) % 360;
        } else {
            rotate = (360 + faceBackCameraOrientation - degrees) % 360;
        }
        return rotate;
    }

    private void closeCamera() {
        closePreviewSession();
        releaseTexture();
        closeCameraDevice();
        closeImageReader();
        releaseVideoRecorder();
    }

    private void releaseTexture() {
        if (null != texture) {
            texture.release();
            texture = null;
        }
    }

    private void closeImageReader() {
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
    }

    private void closeCameraDevice() {
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    @Override
    protected void prepareCameraOutputs() {
        try {
            CameraCharacteristics characteristics = currentCameraId.equals(faceBackCameraId) ? backCameraCharacteristics : frontCameraCharacteristics;

            if (currentCameraId.equals(faceFrontCameraId) && frontCameraStreamConfigurationMap == null)
                frontCameraStreamConfigurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            else if (currentCameraId.equals(faceBackCameraId) && backCameraStreamConfigurationMap == null)
                backCameraStreamConfigurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            StreamConfigurationMap map = currentCameraId.equals(faceBackCameraId) ? backCameraStreamConfigurationMap : frontCameraStreamConfigurationMap;
            if (configurationProvider.getMediaQuality() == CameraConfiguration.MEDIA_QUALITY_AUTO) {
                camcorderProfile = CameraHelper.getCamcorderProfile(currentCameraId, configurationProvider.getVideoFileSize(), configurationProvider.getMinimumVideoDuration());
            } else
                camcorderProfile = CameraHelper.getCamcorderProfile(configurationProvider.getMediaQuality(), currentCameraId);

            videoSize = CameraHelper.chooseOptimalSize(Size.fromArray2(map.getOutputSizes(MediaRecorder.class)),
                    windowSize.getWidth(), windowSize.getHeight(), new Size(camcorderProfile.videoFrameWidth, camcorderProfile.videoFrameHeight));

            if (videoSize == null || videoSize.getWidth() > camcorderProfile.videoFrameWidth
                    || videoSize.getHeight() > camcorderProfile.videoFrameHeight)
                videoSize = CameraHelper.getSizeWithClosestRatio(Size.fromArray2(map.getOutputSizes(MediaRecorder.class)), camcorderProfile.videoFrameWidth, camcorderProfile.videoFrameHeight);
            else if (videoSize == null || videoSize.getWidth() > camcorderProfile.videoFrameWidth
                    || videoSize.getHeight() > camcorderProfile.videoFrameHeight)
                videoSize = CameraHelper.getSizeWithClosestRatio(Size.fromArray2(map.getOutputSizes(MediaRecorder.class)), camcorderProfile.videoFrameWidth, camcorderProfile.videoFrameHeight);

            photoSize = CameraHelper.getPictureSize(Size.fromArray2(map.getOutputSizes(ImageFormat.JPEG)),
                    configurationProvider.getMediaQuality() == CameraConfiguration.MEDIA_QUALITY_AUTO
                            ? CameraConfiguration.MEDIA_QUALITY_HIGHEST : configurationProvider.getMediaQuality());

            imageReader = ImageReader.newInstance(photoSize.getWidth(), photoSize.getHeight(),
                    ImageFormat.JPEG, 2);
            imageReader.setOnImageAvailableListener(this, backgroundHandler);

            if (configurationProvider.getMediaAction() == CameraConfiguration.MEDIA_ACTION_PHOTO
                    || configurationProvider.getMediaAction() == CameraConfiguration.MEDIA_ACTION_BOTH) {

                if (windowSize.getHeight() * windowSize.getWidth() > photoSize.getWidth() * photoSize.getHeight()) {
                    previewSize = CameraHelper.getOptimalPreviewSize(Size.fromArray2(map.getOutputSizes(SurfaceTexture.class)), photoSize.getWidth(), photoSize.getHeight());
                } else {
                    previewSize = CameraHelper.getOptimalPreviewSize(Size.fromArray2(map.getOutputSizes(SurfaceTexture.class)), windowSize.getWidth(), windowSize.getHeight());
                }

                if (previewSize == null)
                    previewSize = CameraHelper.chooseOptimalSize(Size.fromArray2(map.getOutputSizes(SurfaceTexture.class)), windowSize.getWidth(), windowSize.getHeight(), photoSize);

            } else {
                if (windowSize.getHeight() * windowSize.getWidth() > videoSize.getWidth() * videoSize.getHeight()) {
                    previewSize = CameraHelper.getOptimalPreviewSize(Size.fromArray2(map.getOutputSizes(SurfaceTexture.class)), videoSize.getWidth(), videoSize.getHeight());
                } else {
                    previewSize = CameraHelper.getOptimalPreviewSize(Size.fromArray2(map.getOutputSizes(SurfaceTexture.class)), windowSize.getWidth(), windowSize.getHeight());
                }

                if (previewSize == null)
                    previewSize = CameraHelper.getSizeWithClosestRatio(Size.fromArray2(map.getOutputSizes(SurfaceTexture.class)), videoSize.getWidth(), videoSize.getHeight());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while setup camera sizes.", e);
        }
    }

    @Override
    protected boolean prepareVideoRecorder() {
        videoRecorder = new MediaRecorder();
        try {
            videoRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            videoRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);

            videoRecorder.setOutputFormat(camcorderProfile.fileFormat);
            videoRecorder.setVideoFrameRate(camcorderProfile.videoFrameRate);
            videoRecorder.setVideoSize(videoSize.getWidth(), videoSize.getHeight());
            videoRecorder.setVideoEncodingBitRate(camcorderProfile.videoBitRate);
            videoRecorder.setVideoEncoder(camcorderProfile.videoCodec);

            videoRecorder.setAudioEncodingBitRate(camcorderProfile.audioBitRate);
            videoRecorder.setAudioChannels(camcorderProfile.audioChannels);
            videoRecorder.setAudioSamplingRate(camcorderProfile.audioSampleRate);
            videoRecorder.setAudioEncoder(camcorderProfile.audioCodec);

            File outputFile = outputPath;
            String outputFilePath = outputFile.toString();
            videoRecorder.setOutputFile(outputFilePath);

            if (configurationProvider.getVideoFileSize() > 0) {
                videoRecorder.setMaxFileSize(configurationProvider.getVideoFileSize());
                videoRecorder.setOnInfoListener(this);
            }
            if (configurationProvider.getVideoDuration() > 0) {
                videoRecorder.setMaxDuration(configurationProvider.getVideoDuration());
                videoRecorder.setOnInfoListener(this);
            }
            videoRecorder.setOrientationHint(getVideoOrientation(configurationProvider.getSensorPosition()));

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

    private void updatePreview(CameraCaptureSession cameraCaptureSession) {
        if (null == cameraDevice) {
            return;
        }
        captureSession = cameraCaptureSession;
        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        previewRequest = previewRequestBuilder.build();

        try {
            captureSession.setRepeatingRequest(previewRequest, captureCallback, backgroundHandler);
        } catch (Exception e) {
            Log.e(TAG, "Error updating preview: ", e);
        }
        setFlashModeAndBuildPreviewRequest(configurationProvider.getFlashMode());
    }

    private void closePreviewSession() {
        if (captureSession != null) {
            captureSession.close();
            try {
                captureSession.abortCaptures();
            } catch (Exception ignore) {
            } finally {
                captureSession = null;
            }
        }
    }

    private void lockFocus() {
        try {
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);

            previewState = STATE_WAITING_LOCK;
            captureSession.capture(previewRequestBuilder.build(), captureCallback, backgroundHandler);
        } catch (Exception ignore) {
        }
    }

    private void processCaptureResult(CaptureResult result) {
        switch (previewState) {
            case STATE_PREVIEW: {
                break;
            }
            case STATE_WAITING_LOCK: {
                Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                if (afState == null) {
                    captureStillPicture();
                } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState
                        || CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState
                        || CaptureResult.CONTROL_AF_STATE_INACTIVE == afState
                        || CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN == afState) {
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                        previewState = STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    } else {
                        runPreCaptureSequence();
                    }
                }
                break;
            }
            case STATE_WAITING_PRE_CAPTURE: {
                Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                if (aeState == null ||
                        aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                        aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                    previewState = STATE_WAITING_NON_PRE_CAPTURE;
                }
                break;
            }
            case STATE_WAITING_NON_PRE_CAPTURE: {
                Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                    previewState = STATE_PICTURE_TAKEN;
                    captureStillPicture();
                }
                break;
            }
            case STATE_PICTURE_TAKEN:
                break;
        }
    }

    private void runPreCaptureSequence() {
        try {
            previewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            previewState = STATE_WAITING_PRE_CAPTURE;
            captureSession.capture(previewRequestBuilder.build(), captureCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setFlashModeAndBuildPreviewRequest(@CameraConfiguration.FlashMode int flashMode) {
        try {

            switch (flashMode) {
                case CameraConfiguration.FLASH_MODE_AUTO:
                    previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                    previewRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_SINGLE);
                    break;
                case CameraConfiguration.FLASH_MODE_ON:
                    previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                    previewRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_SINGLE);
                    break;
                case CameraConfiguration.FLASH_MODE_OFF:
                    previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                    previewRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
                    break;
                default:
                    previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                    previewRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_SINGLE);
                    break;
            }

            previewRequest = previewRequestBuilder.build();

            try {
                captureSession.setRepeatingRequest(previewRequest, captureCallback, backgroundHandler);
            } catch (Exception e) {
                Log.e(TAG, "Error updating preview: ", e);
            }
        } catch (Exception ignore) {
            Log.e(TAG, "Error setting flash: ", ignore);
        }
    }

    private void captureStillPicture() {
        try {
            if (null == cameraDevice) {
                return;
            }
            final CaptureRequest.Builder captureBuilder =
                    cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(imageReader.getSurface());

            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getPhotoOrientation(configurationProvider.getSensorPosition()));

            CameraCaptureSession.CaptureCallback CaptureCallback = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    Log.d(TAG, "onCaptureCompleted: ");
                }
            };

            captureSession.stopRepeating();
            captureSession.capture(captureBuilder.build(), CaptureCallback, null);

        } catch (CameraAccessException e) {
            Log.e(TAG, "Error during capturing picture");
        }
    }

    private void unlockFocus() {
        try {
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            captureSession.capture(previewRequestBuilder.build(), captureCallback, backgroundHandler);
            previewState = STATE_PREVIEW;
            captureSession.setRepeatingRequest(previewRequest, captureCallback, backgroundHandler);
        } catch (Exception e) {
            Log.e(TAG, "Error during focus unlocking");
        }
    }

    @Override
    public void onImageAvailable(ImageReader imageReader) {
        File outputFile = outputPath;
        backgroundHandler.post(new ImageSaver(imageReader.acquireNextImage(), outputFile, new ImageSaver.ImageSaverCallback() {
            @Override
            public void onSuccessFinish() {
                Log.d(TAG, "onPhotoSuccessFinish: ");
                if (cameraPhotoListener != null) {
                    uiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            cameraPhotoListener.onPhotoTaken(outputPath);
                        }
                    });
                }
                unlockFocus();
            }

            @Override
            public void onError() {
                Log.d(TAG, "onPhotoError: ");
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        cameraPhotoListener.onPhotoTakeError();
                    }
                });
            }
        }));
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        if (surfaceTexture != null) startPreview(surfaceTexture);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
        if (surfaceTexture != null) startPreview(surfaceTexture);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
    }

    @IntDef({STATE_PREVIEW, STATE_WAITING_LOCK, STATE_WAITING_PRE_CAPTURE, STATE_WAITING_NON_PRE_CAPTURE, STATE_PICTURE_TAKEN})
    @Retention(RetentionPolicy.SOURCE)
    @interface CameraPreviewState {
    }

}
