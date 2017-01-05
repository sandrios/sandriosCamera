package com.sandrios.sandriosCamera.internal.imageCropper;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Arpit Gandhi (arpitgandhi9)
 */
public class BitmapUtils {

    /**
     * Write the given bitmap into the given file. JPEG is used as the compression format with
     * quality set
     * to 100.
     *
     * @param bm   The bitmap.
     * @param file The file to write the bitmap into.
     */
    public static void writeBitmapToFile(Bitmap bm, File file, int quality) throws IOException {

        FileOutputStream fos = new FileOutputStream(file);
        bm.compress(Bitmap.CompressFormat.JPEG, quality, fos);
        fos.flush();
        fos.close();
    }


    public static Bitmap addPadding(Bitmap bmp, int color) {

        if (bmp == null) {
            return null;
        }

        int biggerParam = Math.max(bmp.getWidth(), bmp.getHeight());
        Bitmap bitmap = Bitmap.createBitmap(biggerParam, biggerParam, bmp.getConfig());
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(color);

        int top = bmp.getHeight() > bmp.getWidth() ? 0 : (bmp.getWidth() - bmp.getHeight()) / 2;
        int left = bmp.getWidth() > bmp.getHeight() ? 0 : (bmp.getHeight() - bmp.getWidth()) / 2;

        canvas.drawBitmap(bmp, left, top, null);
        return bitmap;
    }

    public static Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }
}
