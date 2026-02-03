package com.pratikpatil.stickerrr;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pratikpatil.stickerrr.createpack.ContentsJsonWriter;
import com.pratikpatil.stickerrr.createpack.PackStorage;
import com.pratikpatil.stickerrr.stickerapi.ContentFileParser;
import com.pratikpatil.stickerrr.stickerapi.Sticker;
import com.pratikpatil.stickerrr.stickerapi.StickerContentProvider;
import com.pratikpatil.stickerrr.stickerapi.StickerPack;
import com.pratikpatil.stickerrr.stickerapi.StickerPackLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EditPackActivity extends AppCompatActivity
        implements EditStickerListAdapter.OnRemoveStickerListener, EditStickerListAdapter.OnUpdateStickerListener {

    public static final String EXTRA_PACK_ID = "pack_id";

    private static final int MIN_STICKERS = 3;
    private static final int MAX_STICKERS = 30;
    private static final Pattern STICKER_INDEX_PATTERN = Pattern.compile("sticker_(\\d+)\\.webp");

    private String packIdentifier;
    private String trayFileName;
    private Uri trayUri;
    private final List<Sticker> stickers = new ArrayList<>();
    private PackStorage packStorage;
    private int pendingReplacePosition = -1;
    private EditText editPackName;
    private EditText editPublisher;
    private ImageView imgTrayPreview;
    private RecyclerView recyclerStickers;
    private EditStickerListAdapter stickerAdapter;

    private final ActivityResultLauncher<Intent> pickTray = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
                Uri uri = result.getData().getData();
                if (uri != null) {
                    trayUri = uri;
                    imgTrayPreview.setImageURI(uri);
                    try {
                        trayFileName = packStorage.saveTrayIcon(packIdentifier, uri);
                    } catch (Exception e) {
                        Toast.makeText(this, "Failed to save tray", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
                Uri uri = result.getData().getData();
                if (uri == null) return;
                if (pendingReplacePosition >= 0 && pendingReplacePosition < stickers.size()) {
                    Sticker sticker = stickers.get(pendingReplacePosition);
                    try {
                        packStorage.replaceStickerImage(packIdentifier, sticker.imageFileName, uri);
                        stickerAdapter.notifyItemChanged(pendingReplacePosition);
                        Toast.makeText(this, R.string.pack_saved, Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Failed to replace: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    pendingReplacePosition = -1;
                } else {
                    addStickerFromUri(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_pack);

        packIdentifier = getIntent().getStringExtra(EXTRA_PACK_ID);
        if (packIdentifier == null) {
            finish();
            return;
        }

        setTitle(R.string.edit_pack);
        packStorage = new PackStorage(this);

        editPackName = findViewById(R.id.editPackName);
        editPublisher = findViewById(R.id.editPublisher);
        imgTrayPreview = findViewById(R.id.imgTrayPreview);
        recyclerStickers = findViewById(R.id.recyclerStickers);
        Button btnPickTray = findViewById(R.id.btnPickTray);
        Button btnPickImage = findViewById(R.id.btnPickImage);
        Button btnSave = findViewById(R.id.btnSave);

        loadPack();
        trayFileName = "tray_" + packIdentifier + ".png";

        stickerAdapter = new EditStickerListAdapter(stickers, this, this);
        recyclerStickers.setLayoutManager(new LinearLayoutManager(this));
        recyclerStickers.setAdapter(stickerAdapter);

        btnPickTray.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*");
            pickTray.launch(Intent.createChooser(i, getString(R.string.pick_tray_icon)));
        });

        btnPickImage.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*");
            pickImage.launch(Intent.createChooser(i, getString(R.string.pick_image)));
        });

        btnSave.setOnClickListener(v -> savePack());
    }

    private void loadPack() {
        try {
            File packDir = packStorage.getPackDir(packIdentifier);
            File contentsFile = new File(packDir, "contents.json");
            if (!contentsFile.exists()) {
                Toast.makeText(this, "Pack not found", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            List<StickerPack> packs = ContentFileParser.parseStickerPacks(contentsFile);
            if (packs.isEmpty()) {
                finish();
                return;
            }
            StickerPack pack = packs.get(0);
            editPackName.setText(pack.name);
            editPublisher.setText(pack.publisher);
            trayFileName = pack.trayImageFile;
            if (pack.getStickers() != null) {
                stickers.clear();
                stickers.addAll(pack.getStickers());
            }
            Uri trayUri = StickerPackLoader.getStickerAssetUri(pack.identifier, pack.trayImageFile);
            imgTrayPreview.setImageURI(trayUri);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load pack", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private int getNextStickerIndex() {
        int max = 0;
        for (Sticker s : stickers) {
            Matcher m = STICKER_INDEX_PATTERN.matcher(s.imageFileName);
            if (m.find()) {
                int n = Integer.parseInt(m.group(1));
                if (n > max) max = n;
            }
        }
        return max + 1;
    }

    private void addStickerFromUri(Uri uri) {
        if (stickers.size() >= MAX_STICKERS) {
            Toast.makeText(this, "Max " + MAX_STICKERS + " stickers", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            int index = getNextStickerIndex();
            List<String> emojis = Collections.singletonList("😀");
            Sticker s = packStorage.addStickerImageToPack(packIdentifier, index, uri, emojis, "");
            stickers.add(s);
            stickerAdapter.notifyItemInserted(stickers.size() - 1);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to add sticker: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onUpdateSticker(Sticker sticker, int position) {
        pendingReplacePosition = position;
        Intent i = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*");
        pickImage.launch(Intent.createChooser(i, getString(R.string.replace_image)));
    }

    @Override
    public void onRemoveSticker(Sticker sticker, int position) {
        if (stickers.size() <= MIN_STICKERS) {
            Toast.makeText(this, R.string.add_at_least_3, Toast.LENGTH_SHORT).show();
            return;
        }
        packStorage.deleteStickerFile(packIdentifier, sticker.imageFileName);
        stickers.remove(position);
        stickerAdapter.notifyItemRemoved(position);
    }

    private void savePack() {
        if (stickers.size() < MIN_STICKERS) {
            Toast.makeText(this, R.string.add_at_least_3, Toast.LENGTH_SHORT).show();
            return;
        }
        String name = editPackName.getText().toString().trim();
        String publisher = editPublisher.getText().toString().trim();
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(publisher)) {
            Toast.makeText(this, "Enter pack name and publisher", Toast.LENGTH_SHORT).show();
            return;
        }
        File packDir = packStorage.getPackDir(packIdentifier);
        String trayFile = trayFileName != null ? trayFileName : ("tray_" + packIdentifier + ".png");
        StickerPack pack = new StickerPack(packIdentifier, name, publisher, trayFile,
                "", "", "", "", "1", false, false);
        pack.setStickers(new ArrayList<>(stickers));
        pack.setAndroidPlayStoreLink("");
        pack.setIosAppStoreLink("");
        try {
            ContentsJsonWriter.write(packDir, pack, "", "");
            StickerContentProvider.notifyPacksChanged(this);
            Toast.makeText(this, R.string.pack_saved, Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
