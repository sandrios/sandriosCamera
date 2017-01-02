package com.sandrios.sandriosCamera.internal.imageCropper;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import java.io.File;
import java.io.IOException;

/**
 * Created by Arpit Gandhi (arpitgandhi9)
 */
public class CropperView extends FrameLayout {

    private static final String TAG = "CropperView";
    public CropperImageView mImageView;
    private CropperGridView mGridView;
    private Bitmap mBitmap;
    private boolean isSnappedToCenter = false;

    private boolean gestureEnabled = true;

    public CropperView(Context context) {
        super(context);
        init(context, null);
    }

    public CropperView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CropperView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CropperView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    // Make Square
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int orientation = getContext().getResources().getConfiguration().orientation;

        if (orientation == Configuration.ORIENTATION_PORTRAIT ||
                orientation == Configuration.ORIENTATION_UNDEFINED) {

            int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
            setMeasuredDimension(width, height);

        } else {

            int height = MeasureSpec.getSize(heightMeasureSpec);
            int width = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
            setMeasuredDimension(width, height);

        }
    }

    private void init(Context context, AttributeSet attrs) {
        mImageView = new CropperImageView(context, attrs);
        mGridView = new CropperGridView(context, attrs);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                0);

        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            params.width = 0;
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        }

        addView(mImageView, 0, params);
        addView(mGridView, 1, params);

        mImageView.setGestureCallback(new TouchGestureCallback());
    }

    public void release() {
        mImageView.release();
    }

    public void setImageBitmap(Bitmap bm) {
        mImageView.setImageBitmap(bm);
    }

    public void setMaxZoom(float zoom) {
        mImageView.setMaxZoom(zoom);
    }

    public Bitmap getCroppedBitmap() {
        return mImageView.getCroppedBitmap();
    }

    public boolean isPreScaling() {
        return mImageView.isDoPreScaling();
    }

    public void setPreScaling(boolean doPreScaling) {
        mImageView.setDoPreScaling(doPreScaling);
    }

    public float getMaxZoom() {
        return mImageView.getMaxZoom();
    }

    public float getMinZoom() {
        return mImageView.getMinZoom();
    }

    public void setMinZoom(float mMInZoom) {
        mImageView.setMinZoom(mMInZoom);
    }

    public void cropToCenter() {
        mImageView.cropToCenter();
    }

    public void fitToCenter() {
        mImageView.fitToCenter();
    }

    public void setDebug(boolean status) {
        mImageView.setDEBUG(status);
    }

    public int getPaddingColor() {
        return mImageView.getPaddingColor();
    }

    public void setPaddingColor(int paddingColor) {
        mImageView.setPaddingColor(paddingColor);
    }

    public int getCropperWidth() {
        return mImageView != null ? mImageView.getWidth() : 0;
    }

    public boolean isMakeSquare() {
        return mImageView.isMakeSquare();
    }

    public void setMakeSquare(boolean mAddPaddingToMakeSquare) {
        mImageView.setMakeSquare(mAddPaddingToMakeSquare);
    }

    public void replaceBitmap(Bitmap bitmap) {
        mImageView.replaceBitmap(bitmap);
    }

    public boolean isGestureEnabled() {
        return gestureEnabled;
    }

    public void setGestureEnabled(boolean enabled) {
        this.gestureEnabled = enabled;
        mImageView.setGestureEnabled(enabled);
    }

    private class TouchGestureCallback implements CropperImageView.GestureCallback {

        @Override
        public void onGestureStarted() {
            mGridView.setShowGrid(true);
        }

        @Override
        public void onGestureCompleted() {
            mGridView.setShowGrid(false);
        }
    }

    public void loadNewImage(String filePath) {
        Log.i(TAG, "load image: " + filePath);
        mBitmap = BitmapFactory.decodeFile(filePath);
        Log.i(TAG, "bitmap: " + mBitmap.getWidth() + " " + mBitmap.getHeight());

        int maxP = Math.max(mBitmap.getWidth(), mBitmap.getHeight());
        float scale1280 = (float) maxP / 1280;

        if (mImageView.getWidth() != 0) {
            mImageView.setMaxZoom(mImageView.getWidth() * 2 / 1280f);
        } else {

            ViewTreeObserver vto = mImageView.getViewTreeObserver();
            vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    mImageView.getViewTreeObserver().removeOnPreDrawListener(this);
                    mImageView.setMaxZoom(mImageView.getWidth() * 2 / 1280f);
                    return true;
                }
            });

        }

        mBitmap = Bitmap.createScaledBitmap(mBitmap, (int) (mBitmap.getWidth() / scale1280),
                (int) (mBitmap.getHeight() / scale1280), true);
        mImageView.setImageBitmap(mBitmap);
    }

    public void cropImage(String filepath) {
        Bitmap bitmap = mImageView.getCroppedBitmap();
        if (bitmap != null) {
            try {
                BitmapUtils.writeBitmapToFile(bitmap, new File(filepath), 90);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void rotateImage() {
        if (mBitmap == null) {
            Log.e(TAG, "bitmap is not loaded yet");
            return;
        }

        mBitmap = BitmapUtils.rotateBitmap(mBitmap, 90);
        mImageView.setImageBitmap(mBitmap);
    }

    public void snapImage() {
        if (isSnappedToCenter) {
            mImageView.cropToCenter();
        } else {
            mImageView.fitToCenter();
        }

        isSnappedToCenter = !isSnappedToCenter;
    }
}
