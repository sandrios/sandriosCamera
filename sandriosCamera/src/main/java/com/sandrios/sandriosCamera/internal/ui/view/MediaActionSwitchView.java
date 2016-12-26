package com.sandrios.sandriosCamera.internal.ui.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.IntDef;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;

import com.sandrios.sandriosCamera.R;
import com.sandrios.sandriosCamera.internal.utils.Utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Arpit Gandhi on 6/24/16.
 */
public class MediaActionSwitchView extends ImageButton {

    public final static int ACTION_PHOTO = 0;
    public final static int ACTION_VIDEO = 1;
    private int currentMediaActionState = ACTION_PHOTO;
    private OnMediaActionStateChangeListener onMediaActionStateChangeListener;
    private Context context;
    private Drawable photoDrawable;
    private Drawable videoDrawable;
    private int padding = 5;

    public MediaActionSwitchView(Context context) {
        this(context, null);
    }

    public MediaActionSwitchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        initializeView();
    }

    public MediaActionSwitchView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs);
    }

    private void initializeView() {
        photoDrawable = ContextCompat.getDrawable(context, R.drawable.ic_photo_camera_white_24dp);
        photoDrawable = DrawableCompat.wrap(photoDrawable);
        DrawableCompat.setTintList(photoDrawable.mutate(), ContextCompat.getColorStateList(context, R.drawable.switch_camera_mode_selector));

        videoDrawable = ContextCompat.getDrawable(context, R.drawable.ic_videocam_white_24dp);
        videoDrawable = DrawableCompat.wrap(videoDrawable);
        DrawableCompat.setTintList(videoDrawable.mutate(), ContextCompat.getColorStateList(context, R.drawable.switch_camera_mode_selector));

        setBackgroundResource(R.drawable.circle_frame_background_dark);
//        setBackgroundResource(R.drawable.circle_frame_background);

        setOnClickListener(new MediaActionClickListener());
        setIcons();
        padding = Utils.convertDipToPixels(context, padding);
        setPadding(padding, padding, padding, padding);
    }

    private void setIcons() {
        if (currentMediaActionState == ACTION_PHOTO) {
            setImageDrawable(videoDrawable);
        } else setImageDrawable(photoDrawable);
    }

    public void setMediaActionState(@MediaActionState int currentMediaActionState) {
        this.currentMediaActionState = currentMediaActionState;
        setIcons();
    }

    public void setOnMediaActionStateChangeListener(OnMediaActionStateChangeListener onMediaActionStateChangeListener) {
        this.onMediaActionStateChangeListener = onMediaActionStateChangeListener;
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

    @IntDef({ACTION_PHOTO, ACTION_VIDEO})
    @Retention(RetentionPolicy.SOURCE)
    public @interface MediaActionState {
    }

    public interface OnMediaActionStateChangeListener {
        void onMediaActionChanged(int mediaActionState);
    }

    private class MediaActionClickListener implements OnClickListener {

        @Override
        public void onClick(View view) {
            if (currentMediaActionState == ACTION_PHOTO) {
                currentMediaActionState = ACTION_VIDEO;
            } else currentMediaActionState = ACTION_PHOTO;

            setIcons();

            if (onMediaActionStateChangeListener != null)
                onMediaActionStateChangeListener.onMediaActionChanged(currentMediaActionState);
        }
    }

}
