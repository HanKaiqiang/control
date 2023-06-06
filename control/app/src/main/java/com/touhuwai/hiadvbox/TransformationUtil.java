package com.touhuwai.hiadvbox;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.TransformationUtils;

import java.util.concurrent.locks.ReentrantLock;

public class TransformationUtil {


    private static final ReentrantLock BITMAP_DRAWABLE_LOCK = new ReentrantLock();

    private static final Paint DEFAULT_PAINT = new Paint(TransformationUtils.PAINT_FLAGS);


     public static Bitmap centerScale(BitmapPool pool, Bitmap toTransform, int outWidth, int outHeight) {
        if (toTransform.getWidth() == outWidth && toTransform.getHeight() == outHeight) {
            return toTransform;
        }

         Bitmap.Config config = getNonNullConfig(toTransform);
         Bitmap toReuse = pool.get(outWidth, outHeight, config);
         int width = toTransform.getWidth();
         int height = toTransform.getHeight();
         float scaleWidth = outWidth * 1.0f / width;
         float scaleHeight = outHeight * 1.0f / height;
         Matrix matrix = new Matrix();
         float dx = 0f;
         float dy = 0f;

        if (scaleWidth > scaleHeight) {
            dx = (outWidth - toTransform.getWidth() * scaleHeight) * 0.5f;
            dy = 0f;
            matrix.postScale(scaleHeight, scaleHeight);
        } else {
            dx = 0f;
            dy = (outHeight - toTransform.getHeight() * scaleWidth) * 0.5f;
            matrix.postScale(scaleWidth, scaleWidth);
        }
        matrix.postTranslate(dx, dy);
        applyMatrix(toTransform, toReuse, matrix);
        return toReuse;
    }

    private static Bitmap.Config getNonNullConfig(Bitmap bitmap) {
        if (bitmap.getConfig() == null) {
            return Bitmap.Config.ARGB_8888;
        }
        return  bitmap.getConfig();
    }

    private static void applyMatrix(Bitmap inBitmap, Bitmap targetBitmap, Matrix matrix) {
        BITMAP_DRAWABLE_LOCK.lock();
        try {
            Canvas canvas = new Canvas(targetBitmap);
            canvas.drawBitmap(inBitmap, matrix, DEFAULT_PAINT);
            clear(canvas);
        } finally {
            BITMAP_DRAWABLE_LOCK.unlock();
        }
    }

    private static void clear(Canvas canvas) {
        canvas.setBitmap(null);
    }
}
