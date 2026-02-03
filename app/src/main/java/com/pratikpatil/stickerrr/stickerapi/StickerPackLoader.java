package com.pratikpatil.stickerrr.stickerapi;

import com.pratikpatil.stickerrr.BuildConfig;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Loads sticker packs from the app's StickerContentProvider.
 */
public final class StickerPackLoader {

    private StickerPackLoader() {
    }

    @NonNull
    public static ArrayList<StickerPack> fetchStickerPacks(Context context) throws IllegalStateException {
        Cursor cursor = context.getContentResolver().query(StickerContentProvider.AUTHORITY_URI, null, null, null, null);
        if (cursor == null) {
            throw new IllegalStateException("could not fetch from content provider, " + BuildConfig.CONTENT_PROVIDER_AUTHORITY);
        }
        HashSet<String> identifierSet = new HashSet<>();
        ArrayList<StickerPack> stickerPackList = fetchFromContentProvider(cursor);
        for (StickerPack stickerPack : stickerPackList) {
            if (identifierSet.contains(stickerPack.identifier)) {
                throw new IllegalStateException("sticker pack identifiers should be unique, there are more than one pack with identifier:" + stickerPack.identifier);
            }
            identifierSet.add(stickerPack.identifier);
        }
        if (stickerPackList.isEmpty()) {
            cursor.close();
            return stickerPackList;
        }
        for (StickerPack stickerPack : stickerPackList) {
            List<Sticker> stickers = getStickersForPack(context, stickerPack);
            stickerPack.setStickers(stickers);
        }
        cursor.close();
        for (StickerPack stickerPack : stickerPackList) {
            try {
                StickerPackValidator.verifyStickerPackValidity(context, stickerPack);
            } catch (IllegalStateException e) {
                // Allow listing packs even if validation fails (e.g. for in-progress packs)
                // Re-throw if you want strict validation on load
            }
        }
        return stickerPackList;
    }

    @NonNull
    public static List<StickerPack> fetchStickerPacksWithoutValidation(Context context) {
        Cursor cursor = context.getContentResolver().query(StickerContentProvider.AUTHORITY_URI, null, null, null, null);
        if (cursor == null) return new ArrayList<>();
        ArrayList<StickerPack> list = fetchFromContentProvider(cursor);
        cursor.close();
        for (StickerPack pack : list) {
            List<Sticker> stickers = getStickersForPack(context, pack);
            pack.setStickers(stickers);
        }
        return list;
    }

    @NonNull
    private static List<Sticker> getStickersForPack(Context context, StickerPack stickerPack) {
        List<Sticker> stickers = fetchFromContentProviderForStickers(stickerPack.identifier, context.getContentResolver());
        for (Sticker sticker : stickers) {
            try {
                byte[] bytes = fetchStickerAsset(stickerPack.identifier, sticker.imageFileName, context.getContentResolver());
                if (bytes.length > 0) {
                    sticker.setSize(bytes.length);
                }
            } catch (IOException | IllegalArgumentException ignored) {
            }
        }
        return stickers;
    }

    @NonNull
    private static ArrayList<StickerPack> fetchFromContentProvider(Cursor cursor) {
        ArrayList<StickerPack> stickerPackList = new ArrayList<>();
        if (!cursor.moveToFirst()) return stickerPackList;
        do {
            String identifier = cursor.getString(cursor.getColumnIndexOrThrow(StickerContentProvider.STICKER_PACK_IDENTIFIER_IN_QUERY));
            String name = cursor.getString(cursor.getColumnIndexOrThrow(StickerContentProvider.STICKER_PACK_NAME_IN_QUERY));
            String publisher = cursor.getString(cursor.getColumnIndexOrThrow(StickerContentProvider.STICKER_PACK_PUBLISHER_IN_QUERY));
            String trayImage = cursor.getString(cursor.getColumnIndexOrThrow(StickerContentProvider.STICKER_PACK_ICON_IN_QUERY));
            String androidPlayStoreLink = cursor.getString(cursor.getColumnIndexOrThrow(StickerContentProvider.ANDROID_APP_DOWNLOAD_LINK_IN_QUERY));
            String iosAppLink = cursor.getString(cursor.getColumnIndexOrThrow(StickerContentProvider.IOS_APP_DOWNLOAD_LINK_IN_QUERY));
            String publisherEmail = cursor.getString(cursor.getColumnIndexOrThrow(StickerContentProvider.PUBLISHER_EMAIL));
            String publisherWebsite = cursor.getString(cursor.getColumnIndexOrThrow(StickerContentProvider.PUBLISHER_WEBSITE));
            String privacyPolicyWebsite = cursor.getString(cursor.getColumnIndexOrThrow(StickerContentProvider.PRIVACY_POLICY_WEBSITE));
            String licenseAgreementWebsite = cursor.getString(cursor.getColumnIndexOrThrow(StickerContentProvider.LICENSE_AGREEMENT_WEBSITE));
            String imageDataVersion = cursor.getString(cursor.getColumnIndexOrThrow(StickerContentProvider.IMAGE_DATA_VERSION));
            boolean avoidCache = cursor.getShort(cursor.getColumnIndexOrThrow(StickerContentProvider.AVOID_CACHE)) > 0;
            boolean animatedStickerPack = cursor.getShort(cursor.getColumnIndexOrThrow(StickerContentProvider.ANIMATED_STICKER_PACK)) > 0;
            StickerPack stickerPack = new StickerPack(identifier, name, publisher, trayImage, publisherEmail, publisherWebsite, privacyPolicyWebsite, licenseAgreementWebsite, imageDataVersion, avoidCache, animatedStickerPack);
            stickerPack.setAndroidPlayStoreLink(androidPlayStoreLink != null ? androidPlayStoreLink : "");
            stickerPack.setIosAppStoreLink(iosAppLink != null ? iosAppLink : "");
            stickerPackList.add(stickerPack);
        } while (cursor.moveToNext());
        return stickerPackList;
    }

    @NonNull
    private static List<Sticker> fetchFromContentProviderForStickers(String identifier, ContentResolver contentResolver) {
        Uri uri = getStickerListUri(identifier);
        String[] projection = {StickerContentProvider.STICKER_FILE_NAME_IN_QUERY, StickerContentProvider.STICKER_FILE_EMOJI_IN_QUERY, StickerContentProvider.STICKER_FILE_ACCESSIBILITY_TEXT_IN_QUERY};
        Cursor cursor = contentResolver.query(uri, projection, null, null, null);
        List<Sticker> stickers = new ArrayList<>();
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                String name = cursor.getString(cursor.getColumnIndexOrThrow(StickerContentProvider.STICKER_FILE_NAME_IN_QUERY));
                String emojisConcatenated = cursor.getString(cursor.getColumnIndexOrThrow(StickerContentProvider.STICKER_FILE_EMOJI_IN_QUERY));
                String accessibilityText = cursor.getString(cursor.getColumnIndexOrThrow(StickerContentProvider.STICKER_FILE_ACCESSIBILITY_TEXT_IN_QUERY));
                List<String> emojis = new ArrayList<>(StickerPackValidator.EMOJI_MAX_LIMIT);
                if (!TextUtils.isEmpty(emojisConcatenated)) {
                    emojis = Arrays.asList(emojisConcatenated.split(","));
                }
                stickers.add(new Sticker(name, emojis, accessibilityText));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return stickers;
    }

    public static byte[] fetchStickerAsset(@NonNull String identifier, @NonNull String name, ContentResolver contentResolver) throws IOException {
        try (InputStream inputStream = contentResolver.openInputStream(getStickerAssetUri(identifier, name));
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            if (inputStream == null) {
                throw new IOException("cannot read sticker asset:" + identifier + "/" + name);
            }
            byte[] data = new byte[16384];
            int read;
            while ((read = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, read);
            }
            return buffer.toByteArray();
        }
    }

    public static Uri getStickerListUri(String identifier) {
        return new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(BuildConfig.CONTENT_PROVIDER_AUTHORITY)
                .appendPath(StickerContentProvider.STICKERS)
                .appendPath(identifier)
                .build();
    }

    public static Uri getStickerAssetUri(String identifier, String stickerName) {
        return new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(BuildConfig.CONTENT_PROVIDER_AUTHORITY)
                .appendPath(StickerContentProvider.STICKERS_ASSET)
                .appendPath(identifier)
                .appendPath(stickerName)
                .build();
    }
}
