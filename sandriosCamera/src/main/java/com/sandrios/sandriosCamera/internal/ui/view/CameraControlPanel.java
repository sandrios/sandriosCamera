package com.sandrios.sandriosCamera.internal.ui.view;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sandrios.sandriosCamera.R;
import com.sandrios.sandriosCamera.internal.configuration.CameraConfiguration;
import com.sandrios.sandriosCamera.internal.ui.model.Media;
import com.sandrios.sandriosCamera.internal.utils.DateTimeUtils;
import com.sandrios.sandriosCamera.internal.utils.RecyclerItemClickListener;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Created by Arpit Gandhi
 */
public class CameraControlPanel extends RelativeLayout
        implements RecordButton.RecordButtonListener,
        MediaActionSwitchView.OnMediaActionStateChangeListener {

    @MediaActionSwitchView.MediaActionState
    int mediaActionState;
    private Context context;
    private CameraSwitchView cameraSwitchView;
    private RecordButton recordButton;
    private MediaActionSwitchView mediaActionSwitchView;
    private FlashSwitchView flashSwitchView;
    private TextView recordDurationText;
    private TextView recordSizeText;
    private ImageButton settingsButton;
    private RecyclerView slidingGalleryList;
    private RecordButton.RecordButtonListener recordButtonListener;
    private MediaActionSwitchView.OnMediaActionStateChangeListener onMediaActionStateChangeListener;
    private CameraSwitchView.OnCameraTypeChangeListener onCameraTypeChangeListener;
    private FlashSwitchView.FlashModeSwitchListener flashModeSwitchListener;
    private SettingsClickListener settingsClickListener;
    private RecyclerItemClickListener.OnClickListener pickerItemClickListener;
    private TimerTaskBase countDownTimer;
    private long maxVideoFileSize = 0;
    private String mediaFilePath;
    private boolean hasFlash = false;
    private int mediaAction;
    private FileObserver fileObserver;

    public CameraControlPanel(Context context) {
        this(context, null);
    }

    public CameraControlPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    private void init() {
        hasFlash = context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);

        LayoutInflater.from(context).inflate(R.layout.camera_control_panel_layout, this);
        setBackgroundColor(Color.TRANSPARENT);
        settingsButton = findViewById(R.id.settings_view);
        cameraSwitchView = findViewById(R.id.front_back_camera_switcher);
        mediaActionSwitchView = findViewById(R.id.photo_video_camera_switcher);
        recordButton = findViewById(R.id.record_button);
        flashSwitchView = findViewById(R.id.flash_switch_view);
        recordDurationText = findViewById(R.id.record_duration_text);
        recordSizeText = findViewById(R.id.record_size_mb_text);
        slidingGalleryList = findViewById(R.id.horizontal_gallery_list);

        slidingGalleryList.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        cameraSwitchView.setOnCameraTypeChangeListener(onCameraTypeChangeListener);
        mediaActionSwitchView.setOnMediaActionStateChangeListener(this);

        setOnCameraTypeChangeListener(onCameraTypeChangeListener);
        setOnMediaActionStateChangeListener(onMediaActionStateChangeListener);
        setFlashModeSwitchListener(flashModeSwitchListener);
        setRecordButtonListener(recordButtonListener);

        settingsButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_settings_white_24dp));
        settingsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (settingsClickListener != null) settingsClickListener.onSettingsClick();
            }
        });

        if (hasFlash)
            flashSwitchView.setVisibility(VISIBLE);
        else flashSwitchView.setVisibility(GONE);

        countDownTimer = new TimerTask(recordDurationText);
    }

    public void setMediaList(List<Media> mediaList) {
        slidingGalleryList.setAdapter(new GalleryAdapter(context, GalleryAdapter.SMALL, mediaList));
        slidingGalleryList.addOnItemTouchListener(new RecyclerItemClickListener(context, pickerItemClickListener));
    }

    public void lockControls() {
        toggleControls(false);
    }

    public void unLockControls() {
        toggleControls(true);
    }

    private void toggleControls(boolean isVisible) {
        cameraSwitchView.setEnabled(isVisible);
        recordButton.setEnabled(isVisible);
        settingsButton.setEnabled(isVisible);
        flashSwitchView.setEnabled(isVisible);
    }

    public void setup(int mediaAction) {
        this.mediaAction = mediaAction;
        if (CameraConfiguration.MEDIA_ACTION_VIDEO == mediaAction) {
            recordButton.setup(mediaAction, this);
            flashSwitchView.setVisibility(GONE);
        } else {
            recordButton.setup(CameraConfiguration.MEDIA_ACTION_PHOTO, this);
        }

        if (CameraConfiguration.MEDIA_ACTION_BOTH != mediaAction) {
            mediaActionSwitchView.setVisibility(GONE);
        } else mediaActionSwitchView.setVisibility(VISIBLE);
    }

    public void setMediaFilePath(final File mediaFile) {
        this.mediaFilePath = mediaFile.toString();
    }

    public void setMaxVideoFileSize(long maxVideoFileSize) {
        this.maxVideoFileSize = maxVideoFileSize;
    }

    public void setMaxVideoDuration(int maxVideoDurationInMillis) {
        if (maxVideoDurationInMillis > 0)
            countDownTimer = new CountdownTask(recordDurationText, maxVideoDurationInMillis);
        else countDownTimer = new TimerTask(recordDurationText);
    }

    public void setFlasMode(@FlashSwitchView.FlashMode int flashMode) {
        flashSwitchView.setFlashMode(flashMode);
    }

    public void setMediaActionState(@MediaActionSwitchView.MediaActionState int actionState) {
        if (mediaActionState == actionState) return;
        if (MediaActionSwitchView.ACTION_PHOTO == actionState) {
            recordButton.setMediaAction(CameraConfiguration.MEDIA_ACTION_PHOTO);
            if (hasFlash)
                flashSwitchView.setVisibility(VISIBLE);
        } else {
            recordButton.setMediaAction(CameraConfiguration.MEDIA_ACTION_VIDEO);
            flashSwitchView.setVisibility(GONE);
        }
        mediaActionState = actionState;
        mediaActionSwitchView.setMediaActionState(actionState);
    }

    public void setRecordButtonListener(RecordButton.RecordButtonListener recordButtonListener) {
        this.recordButtonListener = recordButtonListener;
    }

    public void rotateControls(int rotation) {
        cameraSwitchView.setRotation(rotation);
        mediaActionSwitchView.setRotation(rotation);
        flashSwitchView.setRotation(rotation);
        recordDurationText.setRotation(rotation);
        recordSizeText.setRotation(rotation);
    }

    public void setOnMediaActionStateChangeListener(MediaActionSwitchView.OnMediaActionStateChangeListener onMediaActionStateChangeListener) {
        this.onMediaActionStateChangeListener = onMediaActionStateChangeListener;
    }

    public void setOnCameraTypeChangeListener(CameraSwitchView.OnCameraTypeChangeListener onCameraTypeChangeListener) {
        this.onCameraTypeChangeListener = onCameraTypeChangeListener;
        if (cameraSwitchView != null)
            cameraSwitchView.setOnCameraTypeChangeListener(this.onCameraTypeChangeListener);
    }

    public void setFlashModeSwitchListener(FlashSwitchView.FlashModeSwitchListener flashModeSwitchListener) {
        this.flashModeSwitchListener = flashModeSwitchListener;
        if (flashSwitchView != null)
            flashSwitchView.setFlashSwitchListener(this.flashModeSwitchListener);
    }

    public void setSettingsClickListener(SettingsClickListener settingsClickListener) {
        this.settingsClickListener = settingsClickListener;
    }

    public void setPickerItemClickListener(RecyclerItemClickListener.OnClickListener pickerItemClickListener) {
        this.pickerItemClickListener = pickerItemClickListener;
    }

    @Override
    public void onTakePhotoButtonPressed() {
        if (recordButtonListener != null)
            recordButtonListener.onTakePhotoButtonPressed();
    }

    public void onStartVideoRecord(final File mediaFile) {
        setMediaFilePath(mediaFile);
        if (maxVideoFileSize > 0) {
            recordSizeText.setText("1Mb" + " / " + maxVideoFileSize / (1024 * 1024) + "Mb");
            recordSizeText.setVisibility(VISIBLE);
            try {
                fileObserver = new FileObserver(this.mediaFilePath) {
                    private long lastUpdateSize = 0;

                    @Override
                    public void onEvent(int event, String path) {
                        final long fileSize = mediaFile.length() / (1024 * 1024);
                        if ((fileSize - lastUpdateSize) >= 1) {
                            lastUpdateSize = fileSize;
                            recordSizeText.post(new Runnable() {
                                @Override
                                public void run() {
                                    recordSizeText.setText(fileSize + "Mb" + " / " + maxVideoFileSize / (1024 * 1024) + "Mb");
                                }
                            });
                        }
                    }
                };
                fileObserver.startWatching();
            } catch (Exception e) {
                Log.e("FileObserver", "setMediaFilePath: ", e);
            }
        }
        countDownTimer.start();
    }

    public void allowRecord(boolean isAllowed) {
        recordButton.setEnabled(isAllowed);
    }

    public void showPicker(boolean isShown) {
        if (isShown) {

        }
        slidingGalleryList.setVisibility(isShown ? VISIBLE : GONE);
    }

    public void allowCameraSwitching(boolean isAllowed) {
        cameraSwitchView.setVisibility(isAllowed ? VISIBLE : GONE);
    }

    public void onStopVideoRecord() {
        if (fileObserver != null)
            fileObserver.stopWatching();
        countDownTimer.stop();
        slidingGalleryList.setVisibility(VISIBLE);
        recordSizeText.setVisibility(GONE);
        cameraSwitchView.setVisibility(View.VISIBLE);
        settingsButton.setVisibility(VISIBLE);

        if (CameraConfiguration.MEDIA_ACTION_BOTH != mediaAction) {
            mediaActionSwitchView.setVisibility(GONE);
        } else mediaActionSwitchView.setVisibility(VISIBLE);
        recordButton.setRecordState(RecordButton.READY_FOR_RECORD_STATE);
    }

    @Override
    public void onStartRecordingButtonPressed() {
        cameraSwitchView.setVisibility(View.GONE);
        mediaActionSwitchView.setVisibility(GONE);
        settingsButton.setVisibility(GONE);
        slidingGalleryList.setVisibility(GONE);

        if (recordButtonListener != null)
            recordButtonListener.onStartRecordingButtonPressed();
    }

    @Override
    public void onStopRecordingButtonPressed() {
        onStopVideoRecord();
        if (recordButtonListener != null)
            recordButtonListener.onStopRecordingButtonPressed();
    }

    @Override
    public void onMediaActionChanged(int mediaActionState) {
        setMediaActionState(mediaActionState);
        if (onMediaActionStateChangeListener != null)
            onMediaActionStateChangeListener.onMediaActionChanged(this.mediaActionState);
    }

    public void startRecording() {
        recordButton.performClick();
    }

    public interface SettingsClickListener {
        void onSettingsClick();
    }

    abstract class TimerTaskBase {
        Handler handler = new Handler(Looper.getMainLooper());
        TextView timerView;
        boolean alive = false;
        long recordingTimeSeconds = 0;
        long recordingTimeMinutes = 0;

        TimerTaskBase(TextView timerView) {
            this.timerView = timerView;
        }

        abstract void stop();

        abstract void start();
    }

    private class CountdownTask extends TimerTaskBase implements Runnable {

        private int maxDurationMilliseconds = 0;

        CountdownTask(TextView timerView, int maxDurationMilliseconds) {
            super(timerView);
            this.maxDurationMilliseconds = maxDurationMilliseconds;
        }

        @Override
        public void run() {

            recordingTimeSeconds--;

            int millis = (int) recordingTimeSeconds * 1000;

            timerView.setText(
                    String.format(Locale.ENGLISH, "%02d:%02d",
                            TimeUnit.MILLISECONDS.toMinutes(millis),
                            TimeUnit.MILLISECONDS.toSeconds(millis) -
                                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
                    ));

            if (recordingTimeSeconds < 10) {
                timerView.setTextColor(Color.RED);
            }

            if (alive && recordingTimeSeconds > 0) handler.postDelayed(this, DateTimeUtils.SECOND);
        }

        @Override
        void stop() {
            timerView.setVisibility(View.INVISIBLE);
            alive = false;
        }

        @Override
        void start() {
            alive = true;
            recordingTimeSeconds = maxDurationMilliseconds / 1000;
            timerView.setTextColor(Color.WHITE);
            timerView.setText(
                    String.format(Locale.ENGLISH, "%02d:%02d",
                            TimeUnit.MILLISECONDS.toMinutes(maxDurationMilliseconds),
                            TimeUnit.MILLISECONDS.toSeconds(maxDurationMilliseconds) -
                                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(maxDurationMilliseconds))
                    ));
            timerView.setVisibility(View.VISIBLE);
            handler.postDelayed(this, DateTimeUtils.SECOND);
        }
    }

    private class TimerTask extends TimerTaskBase implements Runnable {

        TimerTask(TextView timerView) {
            super(timerView);
        }

        @Override
        public void run() {
            recordingTimeSeconds++;

            if (recordingTimeSeconds == 60) {
                recordingTimeSeconds = 0;
                recordingTimeMinutes++;
            }
            timerView.setText(
                    String.format(Locale.ENGLISH, "%02d:%02d", recordingTimeMinutes, recordingTimeSeconds));
            if (alive) handler.postDelayed(this, DateTimeUtils.SECOND);
        }

        public void start() {
            alive = true;
            recordingTimeMinutes = 0;
            recordingTimeSeconds = 0;
            timerView.setText(
                    String.format(Locale.ENGLISH, "%02d:%02d", recordingTimeMinutes, recordingTimeSeconds));
            timerView.setVisibility(View.VISIBLE);
            handler.postDelayed(this, DateTimeUtils.SECOND);
        }

        public void stop() {
            timerView.setVisibility(View.INVISIBLE);
            alive = false;
        }
    }
}
