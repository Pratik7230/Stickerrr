package com.pratikpatil.stickerrr;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pratikpatil.stickerrr.createpack.ContentsJsonWriter;
import com.pratikpatil.stickerrr.createpack.PackStorage;
import com.pratikpatil.stickerrr.stickerapi.Sticker;
import com.pratikpatil.stickerrr.stickerapi.StickerPack;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CreatePackActivity extends AppCompatActivity {

    private static final int MIN_STICKERS = 3;
    private static final int MAX_STICKERS = 30;

    private EditText editPackName;
    private EditText editPublisher;
    private ImageView imgTrayPreview;
    private LinearLayout step1Layout;
    private LinearLayout step2Layout;
    private RecyclerView recyclerStickers;
    private Button btnNext;
    private Button btnSave;

    private int step = 1;
    private String packIdentifier;
    private Uri trayUri;
    private final List<Sticker> stickers = new ArrayList<>();
    private CreateStickerListAdapter stickerAdapter;
    private PackStorage packStorage;

    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
                Uri uri = result.getData().getData();
                if (uri == null) return;
                if (step == 1) {
                    trayUri = uri;
                    imgTrayPreview.setImageURI(uri);
                } else {
                    addStickerFromUri(uri);
                }
            });

    private final ActivityResultLauncher<Intent> pickTray = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
                Uri uri = result.getData().getData();
                if (uri != null) {
                    trayUri = uri;
                    imgTrayPreview.setImageURI(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_pack);

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        packStorage = new PackStorage(this);

        editPackName = findViewById(R.id.editPackName);
        editPublisher = findViewById(R.id.editPublisher);
        imgTrayPreview = findViewById(R.id.imgTrayPreview);
        step1Layout = findViewById(R.id.step1Layout);
        step2Layout = findViewById(R.id.step2Layout);
        recyclerStickers = findViewById(R.id.recyclerStickers);
        btnNext = findViewById(R.id.btnNext);
        Button btnPickTray = findViewById(R.id.btnPickTray);
        Button btnPickImage = findViewById(R.id.btnPickImage);
        btnSave = findViewById(R.id.btnSave);

        stickerAdapter = new CreateStickerListAdapter(stickers);
        recyclerStickers.setLayoutManager(new LinearLayoutManager(this));
        recyclerStickers.setAdapter(stickerAdapter);

        btnPickTray.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*");
            pickTray.launch(Intent.createChooser(i, getString(R.string.pick_tray_icon)));
        });

        btnNext.setOnClickListener(v -> {
            if (step == 1) {
                String name = editPackName.getText().toString().trim();
                String publisher = editPublisher.getText().toString().trim();
                if (TextUtils.isEmpty(name) || TextUtils.isEmpty(publisher)) {
                    Toast.makeText(this, "Enter pack name and publisher", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (trayUri == null) {
                    Toast.makeText(this, R.string.pick_tray_icon, Toast.LENGTH_SHORT).show();
                    return;
                }
                packIdentifier = packStorage.createNewPackIdentifier();
                try {
                    String trayFile = packStorage.saveTrayIcon(packIdentifier, trayUri);
                    step = 2;
                    step1Layout.setVisibility(View.GONE);
                    step2Layout.setVisibility(View.VISIBLE);
                    btnNext.setVisibility(View.GONE);
                    btnSave.setVisibility(View.VISIBLE);
                } catch (Exception e) {
                    Toast.makeText(this, "Failed to save tray: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnPickImage.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*");
            pickImage.launch(Intent.createChooser(i, getString(R.string.pick_image)));
        });

        btnSave.setOnClickListener(v -> savePack());
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void addStickerFromUri(Uri uri) {
        if (stickers.size() >= MAX_STICKERS) {
            Toast.makeText(this, "Max " + MAX_STICKERS + " stickers", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            int index = stickers.size() + 1;
            List<String> emojis = Collections.singletonList("😀");
            Sticker s = packStorage.addStickerImageToPack(packIdentifier, index, uri, emojis, "");
            stickers.add(s);
            stickerAdapter.notifyItemInserted(stickers.size() - 1);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to add sticker: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void savePack() {
        if (stickers.size() < MIN_STICKERS) {
            Toast.makeText(this, R.string.add_at_least_3, Toast.LENGTH_SHORT).show();
            return;
        }
        String name = editPackName.getText().toString().trim();
        String publisher = editPublisher.getText().toString().trim();
        File packDir = packStorage.getPackDir(packIdentifier);
        String trayFileName = "tray_" + packIdentifier + ".png";
        StickerPack pack = new StickerPack(packIdentifier, name, publisher, trayFileName,
                "", "", "", "", "1", false, false);
        pack.setStickers(stickers);
        pack.setAndroidPlayStoreLink("");
        pack.setIosAppStoreLink("");
        try {
            ContentsJsonWriter.write(packDir, pack, "", "");
            com.pratikpatil.stickerrr.stickerapi.StickerContentProvider.notifyPacksChanged(this);
            Toast.makeText(this, R.string.pack_saved, Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
