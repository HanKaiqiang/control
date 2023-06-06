package com.touhuwai.hiadvbox;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class GlideBitmapTransformation extends BitmapTransformation {

    private String ID = "com.touhuwai.hiadvbox.GlideBitmapTransformation";

    private byte[] ID_BYTES = ID.getBytes(StandardCharsets.UTF_8);

    @Override
    protected Bitmap transform(BitmapPool pool, Bitmap toTransform, int outWidth, int outHeight) {
        return TransformationUtil.centerScale(pool, toTransform, outWidth, outHeight);
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
        messageDigest.update(ID_BYTES);
    }
}
