package com.pratikpatil.stickerrr.createpack;

import androidx.annotation.NonNull;

import com.pratikpatil.stickerrr.stickerapi.Sticker;
import com.pratikpatil.stickerrr.stickerapi.StickerPack;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

/**
 * Writes contents.json (WhatsApp sticker pack schema) for a single pack.
 */
public final class ContentsJsonWriter {

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
    private static final String KEY_IMAGE_FILE = "image_file";
    private static final String KEY_EMOJIS = "emojis";
    private static final String KEY_ACCESSIBILITY_TEXT = "accessibility_text";

    private ContentsJsonWriter() {
    }

    /**
     * Write a single pack to contents.json in the given pack directory.
     * File will be packDir/contents.json with root structure { android_play_store_link, ios_app_store_link, sticker_packs: [ one pack ] }.
     */
    public static void write(@NonNull File packDir, @NonNull StickerPack pack, String androidPlayStoreLink, String iosAppStoreLink) throws IOException {
        if (!packDir.isDirectory()) {
            throw new IOException("Not a directory: " + packDir.getAbsolutePath());
        }
        JSONObject root = new JSONObject();
        try {
            root.put(KEY_ANDROID_PLAY_STORE_LINK, androidPlayStoreLink != null ? androidPlayStoreLink : "");
            root.put(KEY_IOS_APP_STORE_LINK, iosAppStoreLink != null ? iosAppStoreLink : "");
            JSONArray packsArray = new JSONArray();
            packsArray.put(packToJson(pack));
            root.put(KEY_STICKER_PACKS, packsArray);
        } catch (org.json.JSONException e) {
            throw new IOException(e);
        }
        File outFile = new File(packDir, "contents.json");
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8")) {
            writer.write(root.toString(2));
        } catch (org.json.JSONException e) {
            throw new IOException(e);
        }
    }

    private static JSONObject packToJson(StickerPack pack) throws org.json.JSONException {
        JSONObject o = new JSONObject();
        o.put(KEY_IDENTIFIER, pack.identifier);
        o.put(KEY_NAME, pack.name);
        o.put(KEY_PUBLISHER, pack.publisher);
        o.put(KEY_TRAY_IMAGE_FILE, pack.trayImageFile);
        o.put(KEY_PUBLISHER_EMAIL, pack.publisherEmail != null ? pack.publisherEmail : "");
        o.put(KEY_PUBLISHER_WEBSITE, pack.publisherWebsite != null ? pack.publisherWebsite : "");
        o.put(KEY_PRIVACY_POLICY_WEBSITE, pack.privacyPolicyWebsite != null ? pack.privacyPolicyWebsite : "");
        o.put(KEY_LICENSE_AGREEMENT_WEBSITE, pack.licenseAgreementWebsite != null ? pack.licenseAgreementWebsite : "");
        o.put(KEY_IMAGE_DATA_VERSION, pack.imageDataVersion != null ? pack.imageDataVersion : "1");
        o.put(KEY_AVOID_CACHE, pack.avoidCache);
        o.put(KEY_ANIMATED_STICKER_PACK, pack.animatedStickerPack);
        JSONArray stickersArray = new JSONArray();
        List<Sticker> stickers = pack.getStickers();
        if (stickers != null) {
            for (Sticker s : stickers) {
                JSONObject st = new JSONObject();
                st.put(KEY_IMAGE_FILE, s.imageFileName);
                JSONArray emojis = new JSONArray();
                if (s.emojis != null) for (String e : s.emojis) emojis.put(e);
                st.put(KEY_EMOJIS, emojis);
                st.put(KEY_ACCESSIBILITY_TEXT, s.accessibilityText != null ? s.accessibilityText : "");
                stickersArray.put(st);
            }
        }
        o.put(KEY_STICKERS, stickersArray);
        return o;
    }
}
