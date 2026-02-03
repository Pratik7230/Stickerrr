package com.pratikpatil.stickerrr.stickerapi;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validates sticker packs against WhatsApp requirements.
 * Simplified for static stickers (no Facebook WebPImage dependency).
 */
public final class StickerPackValidator {

    public static final int EMOJI_MAX_LIMIT = 3;
    private static final int MAX_STATIC_STICKER_A11Y_TEXT_CHAR_LIMIT = 125;
    private static final int MAX_ANIMATED_STICKER_A11Y_TEXT_CHAR_LIMIT = 255;
    private static final int STATIC_STICKER_FILE_LIMIT_KB = 100;
    private static final int ANIMATED_STICKER_FILE_LIMIT_KB = 500;
    private static final int EMOJI_MIN_LIMIT = 1;
    private static final int IMAGE_HEIGHT = 512;
    private static final int IMAGE_WIDTH = 512;
    private static final int STICKER_SIZE_MIN = 3;
    private static final int STICKER_SIZE_MAX = 30;
    private static final int CHAR_COUNT_MAX = 128;
    private static final long KB_IN_BYTES = 1024;
    private static final int TRAY_IMAGE_FILE_SIZE_MAX_KB = 50;
    private static final int TRAY_IMAGE_DIMENSION_MIN = 24;
    private static final int TRAY_IMAGE_DIMENSION_MAX = 512;
    private static final Pattern STRING_VALID_PATTERN = Pattern.compile("[\\w-.,'\\s]+");

    /**
     * Verify sticker pack validity (static stickers only in this implementation).
     */
    public static void verifyStickerPackValidity(@NonNull Context context, @NonNull StickerPack stickerPack) throws IllegalStateException {
        if (TextUtils.isEmpty(stickerPack.identifier)) {
            throw new IllegalStateException("sticker pack identifier is empty");
        }
        if (stickerPack.identifier.length() > CHAR_COUNT_MAX) {
            throw new IllegalStateException("sticker pack identifier cannot exceed " + CHAR_COUNT_MAX + " characters");
        }
        checkStringValidity(stickerPack.identifier);
        if (TextUtils.isEmpty(stickerPack.publisher)) {
            throw new IllegalStateException("sticker pack publisher is empty, sticker pack identifier: " + stickerPack.identifier);
        }
        if (stickerPack.publisher.length() > CHAR_COUNT_MAX) {
            throw new IllegalStateException("sticker pack publisher cannot exceed " + CHAR_COUNT_MAX + " characters, sticker pack identifier: " + stickerPack.identifier);
        }
        if (TextUtils.isEmpty(stickerPack.name)) {
            throw new IllegalStateException("sticker pack name is empty, sticker pack identifier: " + stickerPack.identifier);
        }
        if (stickerPack.name.length() > CHAR_COUNT_MAX) {
            throw new IllegalStateException("sticker pack name cannot exceed " + CHAR_COUNT_MAX + " characters, sticker pack identifier: " + stickerPack.identifier);
        }
        if (TextUtils.isEmpty(stickerPack.trayImageFile)) {
            throw new IllegalStateException("sticker pack tray id is empty, sticker pack identifier:" + stickerPack.identifier);
        }

        try {
            byte[] trayBytes = StickerPackLoader.fetchStickerAsset(stickerPack.identifier, stickerPack.trayImageFile, context.getContentResolver());
            if (trayBytes.length > TRAY_IMAGE_FILE_SIZE_MAX_KB * KB_IN_BYTES) {
                throw new IllegalStateException("tray image should be less than " + TRAY_IMAGE_FILE_SIZE_MAX_KB + " KB, tray image file: " + stickerPack.trayImageFile);
            }
            Bitmap bitmap = BitmapFactory.decodeByteArray(trayBytes, 0, trayBytes.length);
            if (bitmap != null) {
                if (bitmap.getHeight() > TRAY_IMAGE_DIMENSION_MAX || bitmap.getHeight() < TRAY_IMAGE_DIMENSION_MIN) {
                    throw new IllegalStateException("tray image height should be between " + TRAY_IMAGE_DIMENSION_MIN + " and " + TRAY_IMAGE_DIMENSION_MAX + " pixels, current: " + bitmap.getHeight());
                }
                if (bitmap.getWidth() > TRAY_IMAGE_DIMENSION_MAX || bitmap.getWidth() < TRAY_IMAGE_DIMENSION_MIN) {
                    throw new IllegalStateException("tray image width should be between " + TRAY_IMAGE_DIMENSION_MIN + " and " + TRAY_IMAGE_DIMENSION_MAX + " pixels, current: " + bitmap.getWidth());
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot open tray image, " + stickerPack.trayImageFile, e);
        }

        List<Sticker> stickers = stickerPack.getStickers();
        if (stickers == null || stickers.size() < STICKER_SIZE_MIN || stickers.size() > STICKER_SIZE_MAX) {
            throw new IllegalStateException("sticker pack sticker count should be between " + STICKER_SIZE_MIN + " and " + STICKER_SIZE_MAX + " inclusive, it currently has " + (stickers != null ? stickers.size() : 0) + ", sticker pack identifier: " + stickerPack.identifier);
        }
        for (Sticker sticker : stickers) {
            validateSticker(context, stickerPack.identifier, sticker, stickerPack.animatedStickerPack);
        }
    }

    private static void validateSticker(@NonNull Context context, @NonNull String identifier, @NonNull Sticker sticker, boolean animatedStickerPack) throws IllegalStateException {
        if (sticker.emojis.size() > EMOJI_MAX_LIMIT) {
            throw new IllegalStateException("emoji count exceed limit, sticker pack identifier: " + identifier + ", filename: " + sticker.imageFileName);
        }
        if (sticker.emojis.size() < EMOJI_MIN_LIMIT) {
            throw new IllegalStateException("To provide best user experience, please associate at least 1 emoji to this sticker, sticker pack identifier: " + identifier + ", filename: " + sticker.imageFileName);
        }
        if (TextUtils.isEmpty(sticker.imageFileName)) {
            throw new IllegalStateException("no file path for sticker, sticker pack identifier:" + identifier);
        }
        String accessibilityText = sticker.accessibilityText;
        if (isInvalidAccessibilityText(accessibilityText, animatedStickerPack)) {
            throw new IllegalStateException("accessibility text length exceed limit, sticker pack identifier: " + identifier + ", filename: " + sticker.imageFileName);
        }
        validateStickerFile(context, identifier, sticker.imageFileName, animatedStickerPack);
    }

    private static boolean isInvalidAccessibilityText(@Nullable String accessibilityText, boolean isAnimatedStickerPack) {
        if (accessibilityText == null) return false;
        int length = accessibilityText.length();
        return isAnimatedStickerPack && length > MAX_ANIMATED_STICKER_A11Y_TEXT_CHAR_LIMIT
                || !isAnimatedStickerPack && length > MAX_STATIC_STICKER_A11Y_TEXT_CHAR_LIMIT;
    }

    private static void validateStickerFile(@NonNull Context context, @NonNull String identifier, @NonNull String fileName, boolean animatedStickerPack) throws IllegalStateException {
        try {
            byte[] stickerBytes = StickerPackLoader.fetchStickerAsset(identifier, fileName, context.getContentResolver());
            if (!animatedStickerPack && stickerBytes.length > STATIC_STICKER_FILE_LIMIT_KB * KB_IN_BYTES) {
                throw new IllegalStateException("static sticker should be less than " + STATIC_STICKER_FILE_LIMIT_KB + "KB, current file is " + (stickerBytes.length / KB_IN_BYTES) + " KB, sticker pack identifier: " + identifier + ", filename: " + fileName);
            }
            if (animatedStickerPack && stickerBytes.length > ANIMATED_STICKER_FILE_LIMIT_KB * KB_IN_BYTES) {
                throw new IllegalStateException("animated sticker should be less than " + ANIMATED_STICKER_FILE_LIMIT_KB + "KB, current file is " + (stickerBytes.length / KB_IN_BYTES) + " KB, sticker pack identifier: " + identifier + ", filename: " + fileName);
            }
            Bitmap bitmap = BitmapFactory.decodeByteArray(stickerBytes, 0, stickerBytes.length);
            if (bitmap != null) {
                if (bitmap.getHeight() != IMAGE_HEIGHT) {
                    throw new IllegalStateException("sticker height should be " + IMAGE_HEIGHT + ", current height is " + bitmap.getHeight() + ", sticker pack identifier: " + identifier + ", filename: " + fileName);
                }
                if (bitmap.getWidth() != IMAGE_WIDTH) {
                    throw new IllegalStateException("sticker width should be " + IMAGE_WIDTH + ", current width is " + bitmap.getWidth() + ", sticker pack identifier: " + identifier + ", filename: " + fileName);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("cannot open sticker file: sticker pack identifier: " + identifier + ", filename: " + fileName, e);
        }
    }

    private static void checkStringValidity(@NonNull String string) {
        if (!STRING_VALID_PATTERN.matcher(string).matches()) {
            throw new IllegalStateException(string + " contains invalid characters, allowed characters are a to z, A to Z, _ , ' - . and space character");
        }
        if (string.contains("..")) {
            throw new IllegalStateException(string + " cannot contain ..");
        }
    }

    private StickerPackValidator() {
    }
}
