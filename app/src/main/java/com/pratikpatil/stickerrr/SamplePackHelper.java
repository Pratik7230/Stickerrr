package com.pratikpatil.stickerrr;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import androidx.annotation.NonNull;

import com.pratikpatil.stickerrr.createpack.ContentsJsonWriter;
import com.pratikpatil.stickerrr.createpack.PackStorage;
import com.pratikpatil.stickerrr.stickerapi.Sticker;
import com.pratikpatil.stickerrr.stickerapi.StickerContentProvider;
import com.pratikpatil.stickerrr.stickerapi.StickerPack;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Creates a minimal sample sticker pack on first launch so the app has at least one pack.
 */
public final class SamplePackHelper {

    private static final String PREF_NAME = "stickerrr_prefs";
    private static final String KEY_SAMPLE_CREATED = "sample_pack_created";
    private static final String SAMPLE_PACK_ID = "sample_1";
    private static final String SAMPLE_PACK_NAME = "Sample Pack";
    private static final String SAMPLE_PUBLISHER = "Stickerrr";
    private static final String TRAY_FILE = "tray_sample_1.png";
    private static final int STICKER_SIZE = 512;
    private static final int TRAY_SIZE = 96;

    private static final String SAMPLE_PACK_ID_2 = "sample_2";
    private static final String SAMPLE_PACK_NAME_2 = "Fun Emojis";
    private static final String TRAY_FILE_2 = "tray_sample_2.png";

    public static void ensureSamplePackExists(@NonNull Context context) {
        if (context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getBoolean(KEY_SAMPLE_CREATED, false)) {
            return;
        }
        PackStorage storage = new PackStorage(context);
        try {
            createSamplePack(context, storage, SAMPLE_PACK_ID, SAMPLE_PACK_NAME, SAMPLE_PUBLISHER, TRAY_FILE, new int[]{Color.parseColor("#F44336"), Color.parseColor("#2196F3"), Color.parseColor("#FFEB3B")});
            createSamplePack(context, storage, SAMPLE_PACK_ID_2, SAMPLE_PACK_NAME_2, SAMPLE_PUBLISHER, TRAY_FILE_2, new int[]{Color.parseColor("#9C27B0"), Color.parseColor("#00BCD4"), Color.parseColor("#8BC34A")});
            StickerContentProvider.notifyPacksChanged(context);
            markSampleCreated(context);
        } catch (IOException e) {
            // ignore
        }
    }

    private static void createSamplePack(Context context, PackStorage storage, String packId, String packName, String publisher, String trayFile, int[] colors) throws IOException {
        File packDir = storage.getPackDir(packId);
        if (packDir.exists()) return;
        packDir.mkdirs();
        createTrayImage(packDir, trayFile, TRAY_SIZE);
        createStickerImages(packDir, colors);
        StickerPack pack = buildSamplePack(packId, packName, publisher, trayFile, colors.length);
        ContentsJsonWriter.write(packDir, pack, "", "");
    }

    private static void markSampleCreated(Context context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_SAMPLE_CREATED, true).apply();
    }

    private static void createTrayImage(File packDir, String trayFileName, int size) throws IOException {
        Bitmap b = createColorBitmap(size, Color.parseColor("#4CAF50"));
        File out = new File(packDir, trayFileName);
        try (FileOutputStream fos = new FileOutputStream(out)) {
            b.compress(Bitmap.CompressFormat.PNG, 90, fos);
        }
        b.recycle();
    }

    private static void createStickerImages(File packDir, int[] colors) throws IOException {
        for (int i = 0; i < colors.length; i++) {
            Bitmap b = createColorBitmap(STICKER_SIZE, colors[i]);
            File out = new File(packDir, "sticker_" + (i + 1) + ".webp");
            try (FileOutputStream fos = new FileOutputStream(out)) {
                b.compress(Bitmap.CompressFormat.WEBP, 80, fos);
            }
            b.recycle();
        }
    }

    private static Bitmap createColorBitmap(int size, int color) {
        Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        c.drawRect(0, 0, size, size, p);
        return b;
    }

    private static StickerPack buildSamplePack(String packId, String packName, String publisher, String trayFile, int stickerCount) {
        List<Sticker> stickers = new ArrayList<>();
        String[] emojiSets = {"😀", "👍", "❤️", "😊", "👋", "✨", "🎉", "🔥", "⭐"};
        for (int i = 0; i < stickerCount; i++) {
            int j = i % 3;
            stickers.add(new Sticker("sticker_" + (i + 1) + ".webp", Arrays.asList(emojiSets[j], emojiSets[j + 1], emojiSets[j + 2]), "Sticker " + (i + 1)));
        }
        StickerPack pack = new StickerPack(packId, packName, publisher, trayFile,
                "", "", "", "", "1", false, false);
        pack.setStickers(stickers);
        pack.setAndroidPlayStoreLink("");
        pack.setIosAppStoreLink("");
        return pack;
    }
}
