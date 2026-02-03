package com.pratikpatil.stickerrr.stickerapi;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses contents.json (WhatsApp sticker pack schema) from InputStream or File.
 */
public final class ContentFileParser {

    private static final String FIELD_STICKER_IMAGE_FILE = "image_file";
    private static final String FIELD_STICKER_EMOJIS = "emojis";
    private static final String FIELD_STICKER_ACCESSIBILITY_TEXT = "accessibility_text";

    private static final String KEY_ANDROID_PLAY_STORE_LINK = "android_play_store_link";
    private static final String KEY_IOS_APP_STORE_LINK = "ios_app_store_link";
    private static final String KEY_STICKER_PACKS = "sticker_packs";

    private static final String KEY_IDENTIFIER = "identifier";
    private static final String KEY_NAME = "name";
    private static final String KEY_PUBLISHER = "publisher";
    private static final String KEY_TRAY_IMAGE_FILE = "tray_image_file";
    private static final String KEY_PUBLISHER_EMAIL = "publisher_email";
    private static final String KEY_PUBLISHER_WEBSITE = "publisher_website";
    private static final String KEY_PRIVACY_POLICY_WEBSITE = "privacy_policy_website";
    private static final String KEY_LICENSE_AGREEMENT_WEBSITE = "license_agreement_website";
    private static final String KEY_IMAGE_DATA_VERSION = "image_data_version";
    private static final String KEY_AVOID_CACHE = "avoid_cache";
    private static final String KEY_ANIMATED_STICKER_PACK = "animated_sticker_pack";
    private static final String KEY_STICKERS = "stickers";

    @NonNull
    public static List<StickerPack> parseStickerPacks(@NonNull InputStream contentsInputStream) throws IOException, IllegalStateException {
        try {
            return readStickerPacks(contentsInputStream);
        } finally {
            try {
                contentsInputStream.close();
            } catch (IOException ignored) {
            }
        }
    }

    @NonNull
    public static List<StickerPack> parseStickerPacks(@NonNull File contentsFile) throws IOException, IllegalStateException {
        if (!contentsFile.exists() || !contentsFile.isFile()) {
            throw new IllegalStateException("contents.json file does not exist: " + contentsFile.getAbsolutePath());
        }
        try (FileInputStream fis = new FileInputStream(contentsFile)) {
            return readStickerPacks(fis);
        }
    }

    @NonNull
    private static List<StickerPack> readStickerPacks(@NonNull InputStream contentsInputStream) throws IOException, IllegalStateException {
        String json = readStreamToString(contentsInputStream);
        try {
            JSONObject root = new JSONObject(json);
            String androidPlayStoreLink = root.optString(KEY_ANDROID_PLAY_STORE_LINK, "");
            String iosAppStoreLink = root.optString(KEY_IOS_APP_STORE_LINK, "");

            if (!root.has(KEY_STICKER_PACKS)) {
                throw new IllegalStateException("unknown field in json: missing sticker_packs");
            }
            JSONArray packsArray = root.getJSONArray(KEY_STICKER_PACKS);
            List<StickerPack> stickerPackList = new ArrayList<>();
            for (int i = 0; i < packsArray.length(); i++) {
                StickerPack pack = readStickerPack(packsArray.getJSONObject(i));
                pack.setAndroidPlayStoreLink(androidPlayStoreLink);
                pack.setIosAppStoreLink(iosAppStoreLink);
                stickerPackList.add(pack);
            }
            if (stickerPackList.isEmpty()) {
                throw new IllegalStateException("sticker pack list cannot be empty");
            }
            return stickerPackList;
        } catch (JSONException e) {
            throw new IllegalStateException("Invalid contents.json: " + e.getMessage(), e);
        }
    }

    private static String readStreamToString(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (InputStreamReader reader = new InputStreamReader(is, "UTF-8")) {
            char[] buf = new char[4096];
            int n;
            while ((n = reader.read(buf)) != -1) {
                sb.append(buf, 0, n);
            }
        }
        return sb.toString();
    }

    @NonNull
    private static StickerPack readStickerPack(JSONObject obj) throws JSONException, IllegalStateException {
        String identifier = obj.optString(KEY_IDENTIFIER, null);
        String name = obj.optString(KEY_NAME, null);
        String publisher = obj.optString(KEY_PUBLISHER, null);
        String trayImageFile = obj.optString(KEY_TRAY_IMAGE_FILE, null);
        String publisherEmail = obj.optString(KEY_PUBLISHER_EMAIL, "");
        String publisherWebsite = obj.optString(KEY_PUBLISHER_WEBSITE, "");
        String privacyPolicyWebsite = obj.optString(KEY_PRIVACY_POLICY_WEBSITE, "");
        String licenseAgreementWebsite = obj.optString(KEY_LICENSE_AGREEMENT_WEBSITE, "");
        String imageDataVersion = obj.optString(KEY_IMAGE_DATA_VERSION, "1");
        boolean avoidCache = obj.optBoolean(KEY_AVOID_CACHE, false);
        boolean animatedStickerPack = obj.optBoolean(KEY_ANIMATED_STICKER_PACK, false);

        if (TextUtils.isEmpty(identifier)) {
            throw new IllegalStateException("identifier cannot be empty");
        }
        if (identifier.contains("..") || identifier.contains("/")) {
            throw new IllegalStateException("identifier should not contain .. or / to prevent directory traversal");
        }
        if (TextUtils.isEmpty(name)) {
            throw new IllegalStateException("name cannot be empty");
        }
        if (TextUtils.isEmpty(publisher)) {
            throw new IllegalStateException("publisher cannot be empty");
        }
        if (TextUtils.isEmpty(trayImageFile)) {
            throw new IllegalStateException("tray_image_file cannot be empty");
        }
        if (TextUtils.isEmpty(imageDataVersion)) {
            throw new IllegalStateException("image_data_version should not be empty");
        }

        JSONArray stickersArray = obj.getJSONArray(KEY_STICKERS);
        List<Sticker> stickerList = readStickers(stickersArray);
        if (stickerList == null || stickerList.isEmpty()) {
            throw new IllegalStateException("sticker list is empty");
        }

        StickerPack pack = new StickerPack(identifier, name, publisher, trayImageFile,
                publisherEmail, publisherWebsite, privacyPolicyWebsite, licenseAgreementWebsite,
                imageDataVersion, avoidCache, animatedStickerPack);
        pack.setStickers(stickerList);
        return pack;
    }

    @NonNull
    private static List<Sticker> readStickers(JSONArray arr) throws JSONException, IllegalStateException {
        List<Sticker> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            String imageFile = o.optString(FIELD_STICKER_IMAGE_FILE, null);
            String accessibilityText = o.optString(FIELD_STICKER_ACCESSIBILITY_TEXT, null);
            List<String> emojis = new ArrayList<>();
            if (o.has(FIELD_STICKER_EMOJIS)) {
                JSONArray emojiArr = o.getJSONArray(FIELD_STICKER_EMOJIS);
                for (int j = 0; j < emojiArr.length() && j < StickerPackValidator.EMOJI_MAX_LIMIT; j++) {
                    String e = emojiArr.optString(j, null);
                    if (!TextUtils.isEmpty(e)) {
                        emojis.add(e);
                    }
                }
            }
            if (imageFile == null || TextUtils.isEmpty(imageFile)) {
                throw new IllegalStateException("sticker image_file cannot be empty");
            }
            if (!imageFile.endsWith(".webp")) {
                throw new IllegalStateException("image file for stickers should be webp files, image file is: " + imageFile);
            }
            if (imageFile.contains("..") || imageFile.contains("/")) {
                throw new IllegalStateException("the file name should not contain .. or / to prevent directory traversal, image file is:" + imageFile);
            }
            list.add(new Sticker(imageFile, emojis, accessibilityText));
        }
        return list;
    }

    private ContentFileParser() {
    }
}
