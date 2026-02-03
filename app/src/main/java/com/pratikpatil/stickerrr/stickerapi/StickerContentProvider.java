package com.pratikpatil.stickerrr.stickerapi;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pratikpatil.stickerrr.BuildConfig;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * ContentProvider implementing WhatsApp sticker pack contract.
 * Serves sticker packs from app files dir: getFilesDir()/sticker_packs/&lt;pack_id&gt;/contents.json and images.
 */
public class StickerContentProvider extends ContentProvider {

    // Do not change these strings - used by WhatsApp
    public static final String STICKER_PACK_IDENTIFIER_IN_QUERY = "sticker_pack_identifier";
    public static final String STICKER_PACK_NAME_IN_QUERY = "sticker_pack_name";
    public static final String STICKER_PACK_PUBLISHER_IN_QUERY = "sticker_pack_publisher";
    public static final String STICKER_PACK_ICON_IN_QUERY = "sticker_pack_icon";
    public static final String ANDROID_APP_DOWNLOAD_LINK_IN_QUERY = "android_play_store_link";
    public static final String IOS_APP_DOWNLOAD_LINK_IN_QUERY = "ios_app_download_link";
    public static final String PUBLISHER_EMAIL = "sticker_pack_publisher_email";
    public static final String PUBLISHER_WEBSITE = "sticker_pack_publisher_website";
    public static final String PRIVACY_POLICY_WEBSITE = "sticker_pack_privacy_policy_website";
    public static final String LICENSE_AGREEMENT_WEBSITE = "sticker_pack_license_agreement_website";
    public static final String IMAGE_DATA_VERSION = "image_data_version";
    public static final String AVOID_CACHE = "whatsapp_will_not_cache_stickers";
    public static final String ANIMATED_STICKER_PACK = "animated_sticker_pack";
    public static final String STICKER_FILE_NAME_IN_QUERY = "sticker_file_name";
    public static final String STICKER_FILE_EMOJI_IN_QUERY = "sticker_emoji";
    public static final String STICKER_FILE_ACCESSIBILITY_TEXT_IN_QUERY = "sticker_accessibility_text";

    private static final String METADATA = "metadata";
    static final String STICKERS = "stickers";
    static final String STICKERS_ASSET = "stickers_asset";

    private static final int METADATA_CODE = 1;
    private static final int METADATA_CODE_FOR_SINGLE_PACK = 2;
    private static final int STICKERS_CODE = 3;
    private static final int STICKERS_ASSET_CODE = 4;
    private static final int STICKER_PACK_TRAY_ICON_CODE = 5;

    private static UriMatcher matcher;
    private List<StickerPack> stickerPackList;

    public static final Uri AUTHORITY_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(BuildConfig.CONTENT_PROVIDER_AUTHORITY)
            .appendPath(METADATA)
            .build();

    @Override
    public boolean onCreate() {
        Context ctx = getContext();
        if (ctx == null) return false;
        String authority = BuildConfig.CONTENT_PROVIDER_AUTHORITY;
        if (!authority.startsWith(ctx.getPackageName())) {
            throw new IllegalStateException("authority (" + authority + ") must start with package name: " + ctx.getPackageName());
        }
        matcher = new UriMatcher(UriMatcher.NO_MATCH);
        matcher.addURI(authority, METADATA, METADATA_CODE);
        matcher.addURI(authority, METADATA + "/*", METADATA_CODE_FOR_SINGLE_PACK);
        matcher.addURI(authority, STICKERS + "/*", STICKERS_CODE);
        // stickers_asset/<identifier>/<fileName> - match dynamically in openAssetFile/query
        matcher.addURI(authority, STICKERS_ASSET + "/*/*", STICKERS_ASSET_CODE);
        return true;
    }

    private synchronized List<StickerPack> getStickerPackList() {
        Context ctx = Objects.requireNonNull(getContext());
        File packsDir = new File(ctx.getFilesDir(), "sticker_packs");
        List<StickerPack> list = new ArrayList<>();
        if (packsDir.exists() && packsDir.isDirectory()) {
            File[] dirs = packsDir.listFiles(File::isDirectory);
            if (dirs != null) {
                for (File dir : dirs) {
                    File contentsFile = new File(dir, "contents.json");
                    if (contentsFile.exists()) {
                        try {
                            List<StickerPack> parsed = ContentFileParser.parseStickerPacks(contentsFile);
                            list.addAll(parsed);
                        } catch (IOException | IllegalStateException e) {
                            Log.e("StickerContentProvider", "Failed to parse " + contentsFile.getAbsolutePath(), e);
                        }
                    }
                }
            }
        }
        stickerPackList = list;
        return stickerPackList;
    }

    /** Call this after adding/removing packs so UI and WhatsApp can refresh. */
    public static void notifyPacksChanged(Context context) {
        if (context != null) {
            context.getContentResolver().notifyChange(AUTHORITY_URI, null);
        }
    }

    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        int code = matcher.match(uri);
        if (code == METADATA_CODE) {
            return getPackForAllStickerPacks(uri);
        }
        if (code == METADATA_CODE_FOR_SINGLE_PACK) {
            return getCursorForSingleStickerPack(uri);
        }
        if (code == STICKERS_CODE) {
            return getStickersForAStickerPack(uri);
        }
        throw new IllegalArgumentException("Unknown URI: " + uri);
    }

    @Nullable
    @Override
    public android.content.res.AssetFileDescriptor openAssetFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
        int code = matcher.match(uri);
        if (code != STICKERS_ASSET_CODE) return null;
        List<String> pathSegments = uri.getPathSegments();
        if (pathSegments.size() != 3) return null;
        String identifier = pathSegments.get(1);
        String fileName = pathSegments.get(2);
        if (TextUtils.isEmpty(identifier) || TextUtils.isEmpty(fileName)) return null;
        File packDir = new File(Objects.requireNonNull(getContext()).getFilesDir(), "sticker_packs" + File.separator + identifier);
        File file = new File(packDir, fileName);
        if (!file.exists() || !file.isFile()) throw new FileNotFoundException(uri.toString());
        try {
            ParcelFileDescriptor pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            return new android.content.res.AssetFileDescriptor(pfd, 0, file.length());
        } catch (IOException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    @Override
    public String getType(@NonNull Uri uri) {
        int code = matcher.match(uri);
        switch (code) {
            case METADATA_CODE:
                return "vnd.android.cursor.dir/vnd." + BuildConfig.CONTENT_PROVIDER_AUTHORITY + "." + METADATA;
            case METADATA_CODE_FOR_SINGLE_PACK:
                return "vnd.android.cursor.item/vnd." + BuildConfig.CONTENT_PROVIDER_AUTHORITY + "." + METADATA;
            case STICKERS_CODE:
                return "vnd.android.cursor.dir/vnd." + BuildConfig.CONTENT_PROVIDER_AUTHORITY + "." + STICKERS;
            case STICKERS_ASSET_CODE: {
                List<String> segs = uri.getPathSegments();
                if (segs.size() >= 3) {
                    String fileName = segs.get(2);
                    if (fileName != null && fileName.toLowerCase().endsWith(".png")) {
                        return "image/png";
                    }
                }
                return "image/webp";
            }
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    private Cursor getPackForAllStickerPacks(@NonNull Uri uri) {
        return getStickerPackInfo(uri, getStickerPackList());
    }

    private Cursor getCursorForSingleStickerPack(@NonNull Uri uri) {
        String identifier = uri.getLastPathSegment();
        for (StickerPack pack : getStickerPackList()) {
            if (identifier != null && identifier.equals(pack.identifier)) {
                return getStickerPackInfo(uri, Collections.singletonList(pack));
            }
        }
        return getStickerPackInfo(uri, new ArrayList<>());
    }

    @NonNull
    private Cursor getStickerPackInfo(@NonNull Uri uri, @NonNull List<StickerPack> packs) {
        MatrixCursor cursor = new MatrixCursor(new String[]{
                STICKER_PACK_IDENTIFIER_IN_QUERY,
                STICKER_PACK_NAME_IN_QUERY,
                STICKER_PACK_PUBLISHER_IN_QUERY,
                STICKER_PACK_ICON_IN_QUERY,
                ANDROID_APP_DOWNLOAD_LINK_IN_QUERY,
                IOS_APP_DOWNLOAD_LINK_IN_QUERY,
                PUBLISHER_EMAIL,
                PUBLISHER_WEBSITE,
                PRIVACY_POLICY_WEBSITE,
                LICENSE_AGREEMENT_WEBSITE,
                IMAGE_DATA_VERSION,
                AVOID_CACHE,
                ANIMATED_STICKER_PACK,
        });
        for (StickerPack pack : packs) {
            cursor.addRow(new Object[]{
                    pack.identifier,
                    pack.name,
                    pack.publisher,
                    pack.trayImageFile,
                    pack.androidPlayStoreLink != null ? pack.androidPlayStoreLink : "",
                    pack.iosAppStoreLink != null ? pack.iosAppStoreLink : "",
                    pack.publisherEmail != null ? pack.publisherEmail : "",
                    pack.publisherWebsite != null ? pack.publisherWebsite : "",
                    pack.privacyPolicyWebsite != null ? pack.privacyPolicyWebsite : "",
                    pack.licenseAgreementWebsite != null ? pack.licenseAgreementWebsite : "",
                    pack.imageDataVersion != null ? pack.imageDataVersion : "1",
                    pack.avoidCache ? 1 : 0,
                    pack.animatedStickerPack ? 1 : 0,
            });
        }
        cursor.setNotificationUri(Objects.requireNonNull(getContext()).getContentResolver(), uri);
        return cursor;
    }

    @NonNull
    private Cursor getStickersForAStickerPack(@NonNull Uri uri) {
        String identifier = uri.getLastPathSegment();
        MatrixCursor cursor = new MatrixCursor(new String[]{STICKER_FILE_NAME_IN_QUERY, STICKER_FILE_EMOJI_IN_QUERY, STICKER_FILE_ACCESSIBILITY_TEXT_IN_QUERY});
        for (StickerPack pack : getStickerPackList()) {
            if (identifier != null && identifier.equals(pack.identifier) && pack.getStickers() != null) {
                for (Sticker sticker : pack.getStickers()) {
                    cursor.addRow(new Object[]{
                            sticker.imageFileName,
                            TextUtils.join(",", sticker.emojis != null ? sticker.emojis : Collections.<String>emptyList()),
                            sticker.accessibilityText != null ? sticker.accessibilityText : ""
                    });
                }
                break;
            }
        }
        cursor.setNotificationUri(Objects.requireNonNull(getContext()).getContentResolver(), uri);
        return cursor;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        throw new UnsupportedOperationException("Not supported");
    }
}
