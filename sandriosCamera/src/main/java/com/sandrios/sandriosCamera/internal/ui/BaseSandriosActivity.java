package com.sandrios.sandriosCamera.internal.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;

import androidx.annotation.RestrictTo;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.sandrios.sandriosCamera.R;
import com.sandrios.sandriosCamera.internal.SandriosCamera;
import com.sandrios.sandriosCamera.internal.configuration.CameraConfiguration;
import com.sandrios.sandriosCamera.internal.ui.model.Media;
import com.sandrios.sandriosCamera.internal.ui.model.PhotoQualityOption;
import com.sandrios.sandriosCamera.internal.ui.model.VideoQualityOption;
import com.sandrios.sandriosCamera.internal.ui.preview.PreviewActivity;
import com.sandrios.sandriosCamera.internal.ui.view.CameraControlPanel;
import com.sandrios.sandriosCamera.internal.ui.view.CameraSwitchView;
import com.sandrios.sandriosCamera.internal.ui.view.FlashSwitchView;
import com.sandrios.sandriosCamera.internal.ui.view.MediaActionSwitchView;
import com.sandrios.sandriosCamera.internal.ui.view.RecordButton;
import com.sandrios.sandriosCamera.internal.utils.RecyclerItemClickListener;
import com.sandrios.sandriosCamera.internal.utils.Size;
import com.sandrios.sandriosCamera.internal.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Arpit Gandhi on 12/1/16.
 */

@RestrictTo(RestrictTo.Scope.LIBRARY)
public abstract class BaseSandriosActivity<CameraId> extends SandriosCameraActivity<CameraId>
        implements
        RecordButton.RecordButtonListener,
        FlashSwitchView.FlashModeSwitchListener,
        MediaActionSwitchView.OnMediaActionStateChangeListener,
        CameraSwitchView.OnCameraTypeChangeListener,
        CameraControlPanel.SettingsClickListener,
        RecyclerItemClickListener.OnClickListener {

    public static final int ACTION_CONFIRM = 900;
    public static final int ACTION_RETAKE = 901;
    public static final int ACTION_CANCEL = 902;
    protected static final int REQUEST_PREVIEW_CODE = 1001;
    @CameraConfiguration.MediaAction
    protected int mediaAction = CameraConfiguration.MEDIA_ACTION_BOTH;
    @CameraConfiguration.MediaQuality
    protected int mediaQuality = CameraConfiguration.MEDIA_QUALITY_HIGHEST;
    @CameraConfiguration.MediaQuality
    protected int passedMediaQuality = CameraConfiguration.MEDIA_QUALITY_HIGHEST;
    protected CharSequence[] videoQualities;
    protected CharSequence[] photoQualities;
    protected boolean enableImageCrop = false;
    protected int videoDuration = -1;
    protected long videoFileSize = -1;
    protected boolean autoRecord = false;
    protected int minimumVideoDuration = -1;
    protected boolean showPicker = true;
    @MediaActionSwitchView.MediaActionState
    protected int currentMediaActionState;
    @CameraSwitchView.CameraType
    protected int currentCameraType = CameraSwitchView.CAMERA_TYPE_REAR;
    @CameraConfiguration.MediaQuality
    protected int newQuality = -1;
    @CameraConfiguration.FlashMode
    protected int flashMode = CameraConfiguration.FLASH_MODE_AUTO;
    private List<Media> mediaList = new ArrayList<>();
    private CameraControlPanel cameraControlPanel;
    private AlertDialog settingsDialog;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ArrayList<String> permissions = new ArrayList<>();

        permissions.add(Manifest.permission.CAMERA);
        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (mediaAction != CameraConfiguration.MEDIA_ACTION_PHOTO) {
            permissions.add(Manifest.permission.RECORD_AUDIO);
        }
        Dexter.withActivity(this)
                .withPermissions(permissions)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        fetchMediaList();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {

                    }
                }).check();
    }

    @Override
    protected void onProcessBundle(Bundle savedInstanceState) {
        super.onProcessBundle(savedInstanceState);

        extractConfiguration(getIntent().getExtras());
        currentMediaActionState = mediaAction == CameraConfiguration.MEDIA_ACTION_VIDEO ?
                MediaActionSwitchView.ACTION_VIDEO : MediaActionSwitchView.ACTION_PHOTO;
    }

    @Override
    protected void onCameraControllerReady() {
        super.onCameraControllerReady();

        videoQualities = getVideoQualityOptions();
        photoQualities = getPhotoQualityOptions();
    }

    @Override
    protected void onResume() {
        super.onResume();

        cameraControlPanel.lockControls();
        cameraControlPanel.allowRecord(false);
        cameraControlPanel.showPicker(showPicker);
    }

    @Override
    protected void onPause() {
        super.onPause();

        cameraControlPanel.lockControls();
        cameraControlPanel.allowRecord(false);
    }

    private void extractConfiguration(Bundle bundle) {
        if (bundle != null) {
            if (bundle.containsKey(CameraConfiguration.Arguments.MEDIA_ACTION)) {
                switch (bundle.getInt(CameraConfiguration.Arguments.MEDIA_ACTION)) {
                    case CameraConfiguration.MEDIA_ACTION_PHOTO:
                        mediaAction = CameraConfiguration.MEDIA_ACTION_PHOTO;
                        break;
                    case CameraConfiguration.MEDIA_ACTION_VIDEO:
                        mediaAction = CameraConfiguration.MEDIA_ACTION_VIDEO;
                        break;
                    default:
                        mediaAction = CameraConfiguration.MEDIA_ACTION_BOTH;
                        break;
                }
            }

            if (bundle.containsKey(CameraConfiguration.Arguments.MEDIA_QUALITY)) {
                switch (bundle.getInt(CameraConfiguration.Arguments.MEDIA_QUALITY)) {
                    case CameraConfiguration.MEDIA_QUALITY_AUTO:
                        mediaQuality = CameraConfiguration.MEDIA_QUALITY_AUTO;
                        break;
                    case CameraConfiguration.MEDIA_QUALITY_HIGHEST:
                        mediaQuality = CameraConfiguration.MEDIA_QUALITY_HIGHEST;
                        break;
                    case CameraConfiguration.MEDIA_QUALITY_HIGH:
                        mediaQuality = CameraConfiguration.MEDIA_QUALITY_HIGH;
                        break;
                    case CameraConfiguration.MEDIA_QUALITY_MEDIUM:
                        mediaQuality = CameraConfiguration.MEDIA_QUALITY_MEDIUM;
                        break;
                    case CameraConfiguration.MEDIA_QUALITY_LOW:
                        mediaQuality = CameraConfiguration.MEDIA_QUALITY_LOW;
                        break;
                    case CameraConfiguration.MEDIA_QUALITY_LOWEST:
                        mediaQuality = CameraConfiguration.MEDIA_QUALITY_LOWEST;
                        break;
                    default:
                        mediaQuality = CameraConfiguration.MEDIA_QUALITY_MEDIUM;
                        break;
                }
                passedMediaQuality = mediaQuality;
            }

            if (bundle.containsKey(CameraConfiguration.Arguments.VIDEO_DURATION))
                videoDuration = bundle.getInt(CameraConfiguration.Arguments.VIDEO_DURATION);

            if (bundle.containsKey(CameraConfiguration.Arguments.VIDEO_FILE_SIZE))
                videoFileSize = bundle.getLong(CameraConfiguration.Arguments.VIDEO_FILE_SIZE);

            if (bundle.containsKey(CameraConfiguration.Arguments.MINIMUM_VIDEO_DURATION))
                minimumVideoDuration = bundle.getInt(CameraConfiguration.Arguments.MINIMUM_VIDEO_DURATION);

            if (bundle.containsKey(CameraConfiguration.Arguments.SHOW_PICKER))
                showPicker = bundle.getBoolean(CameraConfiguration.Arguments.SHOW_PICKER);

            if (bundle.containsKey(CameraConfiguration.Arguments.ENABLE_CROP))
                enableImageCrop = bundle.getBoolean(CameraConfiguration.Arguments.ENABLE_CROP);

            if (bundle.containsKey(CameraConfiguration.Arguments.FLASH_MODE))
                switch (bundle.getInt(CameraConfiguration.Arguments.FLASH_MODE)) {
                    case CameraConfiguration.FLASH_MODE_AUTO:
                        flashMode = CameraConfiguration.FLASH_MODE_AUTO;
                        break;
                    case CameraConfiguration.FLASH_MODE_ON:
                        flashMode = CameraConfiguration.FLASH_MODE_ON;
                        break;
                    case CameraConfiguration.FLASH_MODE_OFF:
                        flashMode = CameraConfiguration.FLASH_MODE_OFF;
                        break;
                    default:
                        flashMode = CameraConfiguration.FLASH_MODE_AUTO;
                        break;
                }
            if (bundle.containsKey(CameraConfiguration.Arguments.AUTO_RECORD)) {
                if (mediaAction == CameraConfiguration.MEDIA_ACTION_VIDEO) {
                    autoRecord = bundle.getBoolean(CameraConfiguration.Arguments.AUTO_RECORD);
                }
            }
        }
    }

    @Override
    View getUserContentView(LayoutInflater layoutInflater, ViewGroup parent) {
        cameraControlPanel = (CameraControlPanel) layoutInflater.inflate(R.layout.user_control_layout, parent, false);

        if (cameraControlPanel != null) {
            cameraControlPanel.setup(getMediaAction());

            switch (flashMode) {
                case CameraConfiguration.FLASH_MODE_AUTO:
                    cameraControlPanel.setFlasMode(FlashSwitchView.FLASH_AUTO);
                    break;
                case CameraConfiguration.FLASH_MODE_ON:
                    cameraControlPanel.setFlasMode(FlashSwitchView.FLASH_ON);
                    break;
                case CameraConfiguration.FLASH_MODE_OFF:
                    cameraControlPanel.setFlasMode(FlashSwitchView.FLASH_OFF);
                    break;
            }

            cameraControlPanel.setRecordButtonListener(this);
            cameraControlPanel.setFlashModeSwitchListener(this);
            cameraControlPanel.setOnMediaActionStateChangeListener(this);
            cameraControlPanel.setOnCameraTypeChangeListener(this);
            cameraControlPanel.setMaxVideoDuration(getVideoDuration());
            cameraControlPanel.setMaxVideoFileSize(getVideoFileSize());
            cameraControlPanel.setSettingsClickListener(this);
            cameraControlPanel.setPickerItemClickListener(this);
            cameraControlPanel.shouldShowCrop(enableImageCrop);

            if (autoRecord) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        cameraControlPanel.startRecording();
                    }
                }, 1500);
            }
        }
        return cameraControlPanel;
    }

    @Override
    public void onSettingsClick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        if (currentMediaActionState == MediaActionSwitchView.ACTION_VIDEO) {
            builder.setSingleChoiceItems(videoQualities, getVideoOptionCheckedIndex(), getVideoOptionSelectedListener());
            if (getVideoFileSize() > 0)
                builder.setTitle(String.format(getString(R.string.settings_video_quality_title),
                        "(Max " + getVideoFileSize() / (1024 * 1024) + " MB)"));
            else
                builder.setTitle(String.format(getString(R.string.settings_video_quality_title), ""));
        } else {
            builder.setSingleChoiceItems(photoQualities, getPhotoOptionCheckedIndex(), getPhotoOptionSelectedListener());
            builder.setTitle(R.string.settings_photo_quality_title);
        }

        builder.setPositiveButton(R.string.ok_label, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (newQuality > 0 && newQuality != mediaQuality) {
                    mediaQuality = newQuality;
                    dialogInterface.dismiss();
                    cameraControlPanel.lockControls();
                    getCameraController().switchQuality();
                }
            }
        });
        builder.setNegativeButton(R.string.cancel_label, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        settingsDialog = builder.create();
        settingsDialog.show();
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.copyFrom(settingsDialog.getWindow().getAttributes());
        layoutParams.width = Utils.convertDpToPixel(350);
        layoutParams.height = Utils.convertDpToPixel(350);
        settingsDialog.getWindow().setAttributes(layoutParams);
    }

    @Override
    public void onItemClick(View view, int position) {
        String filePath = mediaList.get(position).getPath();
        int mimeType = getMimeType(filePath);
        Intent resultIntent = new Intent();
        resultIntent.putExtra(SandriosCamera.MEDIA, new Media(mimeType, filePath));
        setResult(RESULT_OK, resultIntent);
        this.finish();
    }

    @Override
    public void onCameraTypeChanged(@CameraSwitchView.CameraType int cameraType) {
        if (currentCameraType == cameraType) return;
        currentCameraType = cameraType;

        cameraControlPanel.lockControls();
        cameraControlPanel.allowRecord(false);

        int cameraFace = cameraType == CameraSwitchView.CAMERA_TYPE_FRONT
                ? CameraConfiguration.CAMERA_FACE_FRONT : CameraConfiguration.CAMERA_FACE_REAR;

        getCameraController().switchCamera(cameraFace);
    }


    @Override
    public void onFlashModeChanged(@FlashSwitchView.FlashMode int mode) {
        switch (mode) {
            case FlashSwitchView.FLASH_AUTO:
                flashMode = CameraConfiguration.FLASH_MODE_AUTO;
                getCameraController().setFlashMode(CameraConfiguration.FLASH_MODE_AUTO);
                break;
            case FlashSwitchView.FLASH_ON:
                flashMode = CameraConfiguration.FLASH_MODE_ON;
                getCameraController().setFlashMode(CameraConfiguration.FLASH_MODE_ON);
                break;
            case FlashSwitchView.FLASH_OFF:
                flashMode = CameraConfiguration.FLASH_MODE_OFF;
                getCameraController().setFlashMode(CameraConfiguration.FLASH_MODE_OFF);
                break;
        }
    }


    @Override
    public void onMediaActionChanged(int mediaActionState) {
        if (currentMediaActionState == mediaActionState) return;
        currentMediaActionState = mediaActionState;
    }

    @Override
    public void onTakePhotoButtonPressed() {
        getCameraController().takePhoto();
    }

    @Override
    public void onStartRecordingButtonPressed() {
        getCameraController().startVideoRecord();
    }

    @Override
    public void onStopRecordingButtonPressed() {
        getCameraController().stopVideoRecord();
    }

    @Override
    protected void onScreenRotation(int degrees) {
        cameraControlPanel.rotateControls(degrees);
        rotateSettingsDialog(degrees);
    }

    @Override
    public int getMediaAction() {
        return mediaAction;
    }

    @Override
    public int getMediaQuality() {
        return mediaQuality;
    }

    @Override
    public int getVideoDuration() {
        return videoDuration;
    }

    @Override
    public long getVideoFileSize() {
        return videoFileSize;
    }

    @Override
    public int getFlashMode() {
        return flashMode;
    }

    @Override
    public int getMinimumVideoDuration() {
        return minimumVideoDuration / 1000;
    }

    @Override
    public Activity getActivity() {
        return this;
    }

    @Override
    public void updateCameraPreview(Size size, View cameraPreview) {
        cameraControlPanel.unLockControls();
        cameraControlPanel.allowRecord(true);

        setCameraPreview(cameraPreview, size);
    }

    @Override
    public void updateUiForMediaAction(@CameraConfiguration.MediaAction int mediaAction) {

    }

    @Override
    public void updateCameraSwitcher(int numberOfCameras) {
        cameraControlPanel.allowCameraSwitching(numberOfCameras > 1);
    }

    @Override
    public void onPhotoTaken() {
        startPreviewActivity();
    }

    @Override
    public void onVideoRecordStart(int width, int height) {
        cameraControlPanel.onStartVideoRecord(getCameraController().getOutputFile());
    }

    @Override
    public void onVideoRecordStop() {
        cameraControlPanel.allowRecord(false);
        cameraControlPanel.onStopVideoRecord();
        startPreviewActivity();
    }

    @Override
    public void releaseCameraPreview() {
        clearCameraPreview();
    }

    private void startPreviewActivity() {
        Intent intent = PreviewActivity.newIntent(this,
                getMediaAction(), getCameraController().getOutputFile().toString(), cameraControlPanel.showCrop());
        startActivityForResult(intent, REQUEST_PREVIEW_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_PREVIEW_CODE) {
                if (PreviewActivity.isResultConfirm(data)) {
                    String filePath = PreviewActivity.getMediaFilePatch(data);
                    int mimeType = getMimeType(filePath);
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra(SandriosCamera.MEDIA, new Media(mimeType, filePath));
                    setResult(RESULT_OK, resultIntent);
                    this.finish();
                } else if (PreviewActivity.isResultCancel(data)) {
                    //ignore, just proceed the camera
//                    this.finish();
                } else if (PreviewActivity.isResultRetake(data)) {
                    //ignore, just proceed the camera
                }
            }
        }
    }

    private int getMimeType(String path) {
        Uri uri = Uri.fromFile(new File(path));
        String extension;
        //Check uri format to avoid null
        if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            //If scheme is a content
            final MimeTypeMap mime = MimeTypeMap.getSingleton();
            extension = mime.getExtensionFromMimeType(getContentResolver().getType(uri));
        } else {
            //If scheme is a File
            //This will replace white spaces with %20 and also other special characters. This will avoid returning null values on file name with spaces and special characters.
            extension = MimeTypeMap.getFileExtensionFromUrl(path);
        }
        String mimeTypeString
                = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        int mimeType = SandriosCamera.MediaType.PHOTO;
        if (mimeTypeString.toLowerCase().contains("video")) {
            mimeType = SandriosCamera.MediaType.VIDEO;
        }
        return mimeType;
    }

    private void rotateSettingsDialog(int degrees) {
        if (settingsDialog != null && settingsDialog.isShowing()) {
            ViewGroup dialogView = (ViewGroup) settingsDialog.getWindow().getDecorView();
            for (int i = 0; i < dialogView.getChildCount(); i++) {
                dialogView.getChildAt(i).setRotation(degrees);
            }
        }
    }

    protected abstract CharSequence[] getVideoQualityOptions();

    protected abstract CharSequence[] getPhotoQualityOptions();

    protected int getVideoOptionCheckedIndex() {
        int checkedIndex = -1;
        if (mediaQuality == CameraConfiguration.MEDIA_QUALITY_AUTO) checkedIndex = 0;
        else if (mediaQuality == CameraConfiguration.MEDIA_QUALITY_HIGH) checkedIndex = 1;
        else if (mediaQuality == CameraConfiguration.MEDIA_QUALITY_MEDIUM) checkedIndex = 2;
        else if (mediaQuality == CameraConfiguration.MEDIA_QUALITY_LOW) checkedIndex = 3;

        if (passedMediaQuality != CameraConfiguration.MEDIA_QUALITY_AUTO) checkedIndex--;

        return checkedIndex;
    }

    protected int getPhotoOptionCheckedIndex() {
        int checkedIndex = -1;
        if (mediaQuality == CameraConfiguration.MEDIA_QUALITY_HIGHEST) checkedIndex = 0;
        else if (mediaQuality == CameraConfiguration.MEDIA_QUALITY_HIGH) checkedIndex = 1;
        else if (mediaQuality == CameraConfiguration.MEDIA_QUALITY_MEDIUM) checkedIndex = 2;
        else if (mediaQuality == CameraConfiguration.MEDIA_QUALITY_LOWEST) checkedIndex = 3;
        return checkedIndex;
    }

    protected DialogInterface.OnClickListener getVideoOptionSelectedListener() {
        return new DialogInterface.OnClickListener() {
            @SuppressLint("WrongConstant")
            @Override
            public void onClick(DialogInterface dialogInterface, int index) {
                newQuality = ((VideoQualityOption) videoQualities[index]).getMediaQuality();
            }
        };
    }

    protected DialogInterface.OnClickListener getPhotoOptionSelectedListener() {
        return new DialogInterface.OnClickListener() {
            @SuppressLint("WrongConstant")
            @Override
            public void onClick(DialogInterface dialogInterface, int index) {
                newQuality = ((PhotoQualityOption) photoQualities[index]).getMediaQuality();
            }
        };
    }

    private void fetchMediaList() {
        switch (mediaAction) {
            case CameraConfiguration.MEDIA_ACTION_PHOTO:
                addPhotosToList();
                break;
            case CameraConfiguration.MEDIA_ACTION_VIDEO:
                addVideosToList();
                break;
            case CameraConfiguration.MEDIA_ACTION_BOTH:
                addPhotosToList();
                addVideosToList();
                break;
        }
        cameraControlPanel.setMediaList(mediaList);
    }

    private void addPhotosToList() {
        Cursor imageCursor;
        String[] columns = {MediaStore.Images.Media.DATA};
        String orderBy = MediaStore.Images.Media.DATE_ADDED + " DESC";
        imageCursor = getApplicationContext().getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, null, null, orderBy);
        addToMediaList(imageCursor, SandriosCamera.MediaType.PHOTO);
    }

    private void addVideosToList() {
        String[] columns = {MediaStore.Video.VideoColumns.DATA};
        String orderBy = MediaStore.Video.Media.DATE_ADDED + " DESC";

        Cursor videoCursor = getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, columns, null, null, orderBy);
        addToMediaList(videoCursor, SandriosCamera.MediaType.VIDEO);
    }

    private void addToMediaList(Cursor cursor, final int type) {
        try {
            while (cursor.moveToNext()) {
                String imageLocation = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                Media media = new Media();
                media.setType(type);
                media.setPath(imageLocation);
                mediaList.add(media);
            }
        } finally {
            cursor.close();
        }
    }
}
