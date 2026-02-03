package com.pratikpatil.stickerrr.createpack;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.pratikpatil.stickerrr.stickerapi.Sticker;
import com.pratikpatil.stickerrr.stickerapi.StickerContentProvider;
import com.pratikpatil.stickerrr.stickerapi.StickerPack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Saves and loads sticker packs to/from app files dir: getFilesDir()/sticker_packs/&lt;id&gt;/.
 */
public final class PackStorage {

    private static final String DIR_PACKS = "sticker_packs";
    private static final String TRAY_PREFIX = "tray_";
    private static final String TRAY_EXT = ".png";
    private static final String STICKER_PREFIX = "sticker_";
    private static final String STICKER_EXT = ".webp";

    private final Context context;

    public PackStorage(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    @NonNull
    public File getPacksDir() {
        return new File(context.getFilesDir(), DIR_PACKS);
    }

    @NonNull
    public File getPackDir(@NonNull String packIdentifier) {
        return new File(getPacksDir(), packIdentifier);
    }

    /**
     * Create a new pack directory and return its identifier (safe directory name).
     */
    @NonNull
    public String createNewPackIdentifier() {
        String id = "pack_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        File dir = getPackDir(id);
        if (dir.exists()) return createNewPackIdentifier();
        dir.mkdirs();
        return id;
    }

    /**
     * Save pack metadata and ensure tray + sticker files exist in packDir.
     * Tray and sticker files must already be written to packDir by the caller (e.g. via ImageHelper).
     */
    public void savePack(@NonNull StickerPack pack) throws IOException {
        File packDir = getPackDir(pack.identifier);
        if (!packDir.exists()) packDir.mkdirs();
        ContentsJsonWriter.write(packDir, pack, "", "");
        StickerContentProvider.notifyPacksChanged(context);
    }

    /**
     * Delete a sticker pack and all its files. Notifies the ContentProvider to refresh.
     */
    public boolean deletePack(@NonNull String packIdentifier) {
        File packDir = getPackDir(packIdentifier);
        if (!packDir.exists()) return false;
        deleteRecursive(packDir);
        StickerContentProvider.notifyPacksChanged(context);
        return true;
    }

    private static void deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) deleteRecursive(child);
            }
        }
        file.delete();
    }

    /**
     * Add a sticker image from URI to the pack directory and return the Sticker model (filename, emojis, accessibility).
     * The file is saved as sticker_&lt;index&gt;.webp.
     */
    @NonNull
    public Sticker addStickerImageToPack(@NonNull String packIdentifier, int index, @NonNull Uri imageUri, @NonNull List<String> emojis, @NonNull String accessibilityText) throws IOException {
        File packDir = getPackDir(packIdentifier);
        if (!packDir.exists()) packDir.mkdirs();
        String fileName = STICKER_PREFIX + index + STICKER_EXT;
        File outFile = new File(packDir, fileName);
        ImageHelper.saveAsStickerImage(context, imageUri, outFile);
        return new Sticker(fileName, emojis, accessibilityText);
    }

    /**
     * Delete a single sticker file from the pack (e.g. when removing a sticker in edit mode).
     */
    public boolean deleteStickerFile(@NonNull String packIdentifier, @NonNull String fileName) {
        File file = new File(getPackDir(packIdentifier), fileName);
        if (file.isFile()) {
            boolean deleted = file.delete();
            if (deleted) StickerContentProvider.notifyPacksChanged(context);
            return deleted;
        }
        return false;
    }

    /**
     * Save tray icon from URI to pack directory. Filename will be tray_&lt;packId&gt;.png.
     */
    public String saveTrayIcon(@NonNull String packIdentifier, @NonNull Uri imageUri) throws IOException {
        File packDir = getPackDir(packIdentifier);
        if (!packDir.exists()) packDir.mkdirs();
        String fileName = TRAY_PREFIX + packIdentifier + TRAY_EXT;
        File outFile = new File(packDir, fileName);
        ImageHelper.saveAsTrayIcon(context, imageUri, outFile);
        return fileName;
    }

    /**
     * Copy a sticker pack from assets (e.g. sample_1) to files dir so the ContentProvider can serve it.
     * Asset path e.g. "sample_1" means assets/sample_1/contents.json and assets/sample_1/*.webp, tray.png.
     */
    public void copyPackFromAssets(@NonNull String assetPackPath) throws IOException {
        android.content.res.AssetManager am = context.getAssets();
        String[] list = am.list(assetPackPath);
        if (list == null) throw new IOException("Asset path not found: " + assetPackPath);
        File packDir = new File(getPacksDir(), assetPackPath);
        if (packDir.exists()) return;
        packDir.mkdirs();
        for (String name : list) {
            String assetPath = assetPackPath + "/" + name;
            java.io.InputStream is = am.open(assetPath);
            File out = new File(packDir, name);
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(out)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) != -1) fos.write(buf, 0, n);
            }
            is.close();
        }
        StickerContentProvider.notifyPacksChanged(context);
    }
}
