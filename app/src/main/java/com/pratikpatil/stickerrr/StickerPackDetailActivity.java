package com.pratikpatil.stickerrr;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pratikpatil.stickerrr.BuildConfig;
import com.pratikpatil.stickerrr.createpack.PackStorage;
import com.pratikpatil.stickerrr.stickerapi.StickerPack;
import com.pratikpatil.stickerrr.stickerapi.StickerPackLoader;
import com.pratikpatil.stickerrr.stickerpacklist.StickerPreviewAdapter;

import java.io.InputStream;
import java.util.List;

public class StickerPackDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PACK_ID = "pack_id";

    private StickerPack pack;
    private ImageView imgTray;
    private TextView txtPackName;
    private TextView txtPublisher;
    private RecyclerView recyclerStickers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sticker_pack_detail);

        String packId = getIntent().getStringExtra(EXTRA_PACK_ID);
        if (packId == null) {
            finish();
            return;
        }

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        imgTray = findViewById(R.id.imgTray);
        txtPackName = findViewById(R.id.txtPackName);
        txtPublisher = findViewById(R.id.txtPublisher);
        recyclerStickers = findViewById(R.id.recyclerStickers);
        Button btnAddToWhatsApp = findViewById(R.id.btnAddToWhatsApp);
        Button btnEdit = findViewById(R.id.btnEdit);
        Button btnDelete = findViewById(R.id.btnDelete);

        loadPack(packId);
        if (pack == null) {
            finish();
            return;
        }

        recyclerStickers.setLayoutManager(new GridLayoutManager(this, 4));
        recyclerStickers.setAdapter(new StickerPreviewAdapter(
                pack.identifier,
                pack.getStickers(),
                getContentResolver()));

        btnAddToWhatsApp.setOnClickListener(v -> launchAddToWhatsApp());
        btnEdit.setOnClickListener(v -> {
            startActivity(new Intent(this, EditPackActivity.class).putExtra(EditPackActivity.EXTRA_PACK_ID, pack.identifier));
            finish();
        });
        btnDelete.setOnClickListener(v -> confirmDelete());
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void loadPack(String packId) {
        try {
            List<StickerPack> packs = StickerPackLoader.fetchStickerPacksWithoutValidation(this);
            for (StickerPack p : packs) {
                if (packId.equals(p.identifier)) {
                    pack = p;
                    break;
                }
            }
        } catch (Exception ignored) { }
        if (pack == null) return;
        txtPackName.setText(pack.name);
        txtPublisher.setText(pack.publisher);
        try (InputStream is = getContentResolver().openInputStream(
                StickerPackLoader.getStickerAssetUri(pack.identifier, pack.trayImageFile))) {
            if (is != null) {
                imgTray.setImageBitmap(android.graphics.BitmapFactory.decodeStream(is));
            }
        } catch (Exception ignored) { }
    }

    private void launchAddToWhatsApp() {
        if (!isWhatsAppInstalled()) {
            Toast.makeText(this, R.string.whatsapp_not_installed, Toast.LENGTH_LONG).show();
            return;
        }
        try {
            Intent intent = new Intent();
            intent.setAction("com.whatsapp.intent.action.ENABLE_STICKER_PACK");
            intent.putExtra("sticker_pack_id", pack.identifier);
            intent.putExtra("sticker_pack_authority", BuildConfig.CONTENT_PROVIDER_AUTHORITY);
            intent.putExtra("sticker_pack_name", pack.name);
            startActivity(Intent.createChooser(intent, getString(R.string.add_to_whatsapp)));
        } catch (Exception e) {
            Toast.makeText(this, R.string.whatsapp_not_installed, Toast.LENGTH_LONG).show();
        }
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_pack_title)
                .setMessage(R.string.delete_pack_message)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    if (new PackStorage(this).deletePack(pack.identifier)) {
                        Toast.makeText(this, R.string.pack_deleted, Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private boolean isWhatsAppInstalled() {
        try {
            getPackageManager().getPackageInfo("com.whatsapp", 0);
            return true;
        } catch (Exception ignored) { }
        try {
            getPackageManager().getPackageInfo("com.whatsapp.w4b", 0);
            return true;
        } catch (Exception ignored) { }
        return false;
    }
}
