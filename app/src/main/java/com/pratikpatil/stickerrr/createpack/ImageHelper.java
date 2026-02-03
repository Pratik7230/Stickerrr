package com.pratikpatil.stickerrr.createpack;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Resizes and encodes images for WhatsApp stickers: 512x512 WebP (max 100KB), tray 96x96 PNG (max 50KB).
 */
public final class ImageHelper {

    private static final int STICKER_SIZE = 512;
    private static final int TRAY_SIZE = 96;
    private static final int MAX_STICKER_BYTES = 100 * 1024;
    private static final int MAX_TRAY_BYTES = 50 * 1024;

    private ImageHelper() {
    }

    /**
     * Load bitmap from URI (content or file), scale to 512x512, encode as WebP to outFile.
     * Tries to keep under 100KB by reducing quality.
     */
    public static boolean saveAsStickerImage(@NonNull Context context, @NonNull Uri sourceUri, @NonNull File outFile) throws IOException {
        Bitmap bitmap = loadAndScaleToSquare(context, sourceUri, STICKER_SIZE);
        if (bitmap == null) return false;
        try {
            int quality = 90;
            Bitmap.CompressFormat format = Bitmap.CompressFormat.WEBP;
            while (quality >= 10) {
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    bitmap.compress(format, quality, fos);
                }
                if (outFile.length() <= MAX_STICKER_BYTES) break;
                quality -= 15;
                if (quality < 10) {
                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        bitmap.compress(format, 80, fos);
                    }
                }
            }
            return true;
        } finally {
            bitmap.recycle();
        }
    }

    /**
     * Load bitmap from URI, scale to 96x96, encode as PNG to outFile (tray icon).
     */
    public static boolean saveAsTrayIcon(@NonNull Context context, @NonNull Uri sourceUri, @NonNull File outFile) throws IOException {
        Bitmap bitmap = loadAndScaleToSquare(context, sourceUri, TRAY_SIZE);
        if (bitmap == null) return false;
        try {
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
            }
            if (outFile.length() > MAX_TRAY_BYTES) {
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 70, fos);
                }
            }
            return true;
        } finally {
            bitmap.recycle();
        }
    }

    /**
     * Scale bitmap to exactly size x size (square), center-crop if needed.
     */
    @Nullable
    public static Bitmap loadAndScaleToSquare(@NonNull Context context, @NonNull Uri uri, int size) throws IOException {
        InputStream is = context.getContentResolver().openInputStream(uri);
        if (is == null) return null;
        Bitmap source = BitmapFactory.decodeStream(is);
        is.close();
        if (source == null) return null;
        Bitmap scaled = scaleToSquare(source, size);
        if (scaled != source) source.recycle();
        return scaled;
    }

    @NonNull
    public static Bitmap scaleToSquare(@NonNull Bitmap source, int size) {
        int w = source.getWidth();
        int h = source.getHeight();
        if (w == size && h == size) return source;
        float scale = Math.max((float) size / w, (float) size / h);
        int newW = Math.round(w * scale);
        int newH = Math.round(h * scale);
        Bitmap scaled = Bitmap.createScaledBitmap(source, newW, newH, true);
        if (scaled.getWidth() == size && scaled.getHeight() == size) return scaled;
        int x = (scaled.getWidth() - size) / 2;
        int y = (scaled.getHeight() - size) / 2;
        Bitmap result = Bitmap.createBitmap(size, size, scaled.getConfig() != null ? scaled.getConfig() : Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(scaled, -x, -y, null);
        if (scaled != source) scaled.recycle();
        return result;
    }
}
