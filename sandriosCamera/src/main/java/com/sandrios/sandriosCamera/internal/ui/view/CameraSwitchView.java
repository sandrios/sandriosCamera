package com.sandrios.sandriosCamera.internal.ui.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.IntDef;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.AppCompatImageButton;
import android.util.AttributeSet;
import android.view.View;

import com.sandrios.sandriosCamera.R;
import com.sandrios.sandriosCamera.internal.utils.Utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Arpit Gandhi on 6/24/16.
 */
public class CameraSwitchView extends AppCompatImageButton {

    public static final int CAMERA_TYPE_FRONT = 0;
    public static final int CAMERA_TYPE_REAR = 1;
    private OnCameraTypeChangeListener onCameraTypeChangeListener;
    private Context context;
    private Drawable frontCameraDrawable;
    private Drawable rearCameraDrawable;
    private int padding = 5;
    private
    @CameraType
    int currentCameraType = CAMERA_TYPE_REAR;

    public CameraSwitchView(Context context) {
        this(context, null);
    }

    public CameraSwitchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        initializeView();
    }

    public CameraSwitchView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs);
    }

    private void initializeView() {
        frontCameraDrawable = ContextCompat.getDrawable(context, R.drawable.ic_camera_front_white_24dp);
        frontCameraDrawable = DrawableCompat.wrap(frontCameraDrawable);
        DrawableCompat.setTintList(frontCameraDrawable.mutate(), ContextCompat.getColorStateList(context, R.drawable.switch_camera_mode_selector));

        rearCameraDrawable = ContextCompat.getDrawable(context, R.drawable.ic_camera_rear_white_24dp);
        rearCameraDrawable = DrawableCompat.wrap(rearCameraDrawable);
        DrawableCompat.setTintList(rearCameraDrawable.mutate(), ContextCompat.getColorStateList(context, R.drawable.switch_camera_mode_selector));

        setBackgroundResource(R.drawable.circle_frame_background_dark);
        setOnClickListener(new CameraTypeClickListener());
        setIcons();
        padding = Utils.convertDipToPixels(context, padding);
        setPadding(padding, padding, padding, padding);
    }

    private void setIcons() {
        if (currentCameraType == CAMERA_TYPE_REAR) {
            setImageDrawable(frontCameraDrawable);
        } else setImageDrawable(rearCameraDrawable);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (Build.VERSION.SDK_INT > 10) {
            if (enabled) {
                setAlpha(1f);
            } else {
                setAlpha(0.5f);
            }
        }
    }

    public
    @CameraType
    int getCameraType() {
        return currentCameraType;
    }

    public void setCameraType(@CameraType int cameraType) {
        this.currentCameraType = cameraType;
        setIcons();
    }

    public void setOnCameraTypeChangeListener(OnCameraTypeChangeListener onCameraTypeChangeListener) {
        this.onCameraTypeChangeListener = onCameraTypeChangeListener;
    }

    @IntDef({CAMERA_TYPE_FRONT, CAMERA_TYPE_REAR})
    @Retention(RetentionPolicy.SOURCE)
    public @interface CameraType {
    }

    public interface OnCameraTypeChangeListener {
        void onCameraTypeChanged(@CameraType int cameraType);
    }

    private class CameraTypeClickListener implements OnClickListener {

        @Override
        public void onClick(View view) {
            if (currentCameraType == CAMERA_TYPE_REAR) {
                currentCameraType = CAMERA_TYPE_FRONT;
            } else currentCameraType = CAMERA_TYPE_REAR;

            setIcons();

            if (onCameraTypeChangeListener != null)
                onCameraTypeChangeListener.onCameraTypeChanged(currentCameraType);
        }
    }
}
