package com.pratikpatil.stickerrr.createpack;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
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

    /**
     * Load a bitmap from URI without scaling (for editor).
     */
    @Nullable
    public static Bitmap loadBitmap(@NonNull Context context, @NonNull Uri uri) throws IOException {
        InputStream is = context.getContentResolver().openInputStream(uri);
        if (is == null) return null;
        Bitmap bitmap = BitmapFactory.decodeStream(is);
        is.close();
        return bitmap;
    }

    /**
     * Save bitmap to a temporary file in cache and return its file URI.
     * Caller should use the returned URI and can delete the file when done.
     */
    @NonNull
    public static Uri saveBitmapToCacheUri(@NonNull Context context, @NonNull Bitmap bitmap) throws IOException {
        File cacheDir = context.getCacheDir();
        File out = File.createTempFile("sticker_edit_", ".png", cacheDir);
        try (FileOutputStream fos = new FileOutputStream(out)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        }
        return Uri.fromFile(out);
    }

    /**
     * Make white and near-white pixels transparent (simple background removal).
     * Threshold: pixels with red, green, blue all >= 240 become transparent.
     */
    @NonNull
    public static Bitmap removeBackground(@NonNull Bitmap source) {
        int w = source.getWidth();
        int h = source.getHeight();
        Bitmap result = source.copy(source.getConfig() != null ? source.getConfig() : Bitmap.Config.ARGB_8888, true);
        int[] pixels = new int[w * h];
        result.getPixels(pixels, 0, w, 0, 0, w, h);
        final int threshold = 240;
        for (int i = 0; i < pixels.length; i++) {
            int p = pixels[i];
            int r = Color.red(p);
            int g = Color.green(p);
            int b = Color.blue(p);
            int a = Color.alpha(p);
            if (r >= threshold && g >= threshold && b >= threshold) {
                pixels[i] = Color.TRANSPARENT;
            }
        }
        result.setPixels(pixels, 0, w, 0, 0, w, h);
        return result;
    }

    /**
     * Draw text on the bitmap (centered). Does not modify the original; returns a new bitmap.
     */
    @NonNull
    public static Bitmap drawTextOnBitmap(@NonNull Bitmap source, @NonNull String text, int textColor, float textSizePx) {
        int w = source.getWidth();
        int h = source.getHeight();
        Bitmap result = source.copy(source.getConfig() != null ? source.getConfig() : Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(textColor);
        paint.setTextSize(textSizePx);
        paint.setTextAlign(Paint.Align.CENTER);
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        float x = w * 0.5f;
        float y = h * 0.5f - (bounds.top + bounds.bottom) / 2f;
        canvas.drawText(text, x, y, paint);
        return result;
    }
}
