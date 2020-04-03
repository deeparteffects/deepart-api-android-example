package com.deeparteffects.examples.android;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;

import java.io.InputStream;

/**
 * Created by larrat on 22.02.17.
 */

class ImageHelper {
    static Bitmap loadSizeLimitedBitmapFromUri(
            Uri imageUri,
            ContentResolver contentResolver,
            int imageMaxSideLength) {
        try {

            InputStream imageInputStream = contentResolver.openInputStream(imageUri);

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            Rect outPadding = new Rect();
            BitmapFactory.decodeStream(imageInputStream, outPadding, options);

            int maxSideLength = Math.max(options.outWidth, options.outHeight);
            options.inSampleSize = 1;
            options.inSampleSize = calculateSampleSize(maxSideLength, imageMaxSideLength);
            options.inJustDecodeBounds = false;
            imageInputStream.close();

            imageInputStream = contentResolver.openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(imageInputStream, outPadding, options);
            maxSideLength = Math.max(bitmap.getWidth(), bitmap.getHeight());
            double ratio = imageMaxSideLength / (double) maxSideLength;
            if (ratio < 1) {
                bitmap = Bitmap.createScaledBitmap(
                        bitmap,
                        (int) (bitmap.getWidth() * ratio),
                        (int) (bitmap.getHeight() * ratio),
                        false);
            }

            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }

    private static int calculateSampleSize(int maxSideLength, int expectedMaxImageSideLength) {
        int inSampleSize = 1;

        while (maxSideLength > 2 * expectedMaxImageSideLength) {
            maxSideLength /= 2;
            inSampleSize *= 2;
        }

        return inSampleSize;
    }
}
