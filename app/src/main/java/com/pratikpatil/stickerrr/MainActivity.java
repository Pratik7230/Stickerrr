package com.pratikpatil.stickerrr;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pratikpatil.stickerrr.BuildConfig;
import com.pratikpatil.stickerrr.createpack.PackStorage;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.pratikpatil.stickerrr.stickerapi.StickerPack;
import com.pratikpatil.stickerrr.stickerapi.StickerPackLoader;
import com.pratikpatil.stickerrr.stickerpacklist.PackListAdapter;

import java.util.List;

public class MainActivity extends AppCompatActivity implements PackListAdapter.AddToWhatsAppListener {

    private RecyclerView recyclerPacks;
    private View emptyText;
    private PackListAdapter adapter;

    private final ActivityResultLauncher<Intent> addToWhatsAppLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() != RESULT_OK && result.getData() != null) {
                    String validationError = result.getData().getStringExtra("validation_error");
                    if (validationError != null && !validationError.isEmpty()) {
                        Toast.makeText(this, "WhatsApp: " + validationError, Toast.LENGTH_LONG).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SamplePackHelper.ensureSamplePackExists(this);

        setSupportActionBar(findViewById(R.id.toolbar));

        recyclerPacks = findViewById(R.id.recyclerPacks);
        emptyText = findViewById(R.id.emptyText);
        com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton fabCreate = findViewById(R.id.fabCreate);

        recyclerPacks.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PackListAdapter(getContentResolver(), this);
        recyclerPacks.setAdapter(adapter);

        fabCreate.setOnClickListener(v -> {
            startActivity(new Intent(this, CreatePackActivity.class));
        });

        loadPacks();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPacks();
    }

    @Override
    public void onAddToWhatsApp(@NonNull StickerPack pack) {
        if (!isWhatsAppInstalled()) {
            Toast.makeText(this, R.string.whatsapp_not_installed, Toast.LENGTH_LONG).show();
            return;
        }
        Intent intent = new Intent();
        intent.setAction("com.whatsapp.intent.action.ENABLE_STICKER_PACK");
        intent.putExtra("sticker_pack_id", pack.identifier);
        intent.putExtra("sticker_pack_authority", BuildConfig.CONTENT_PROVIDER_AUTHORITY);
        intent.putExtra("sticker_pack_name", pack.name);
        Intent chooser = Intent.createChooser(intent, getString(R.string.add_to_whatsapp));
        try {
            addToWhatsAppLauncher.launch(chooser);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.whatsapp_not_installed, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onPreviewPack(@NonNull StickerPack pack) {
        startActivity(new Intent(this, StickerPackDetailActivity.class)
                .putExtra(StickerPackDetailActivity.EXTRA_PACK_ID, pack.identifier));
    }

    @Override
    public void onEditPack(@NonNull StickerPack pack) {
        startActivity(new Intent(this, EditPackActivity.class)
                .putExtra(EditPackActivity.EXTRA_PACK_ID, pack.identifier));
    }

    @Override
    public void onDeletePack(@NonNull StickerPack pack) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.delete_pack_title)
                .setMessage(R.string.delete_pack_message)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    if (new PackStorage(this).deletePack(pack.identifier)) {
                        Toast.makeText(this, R.string.pack_deleted, Toast.LENGTH_SHORT).show();
                        loadPacks();
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

    private void loadPacks() {
        try {
            List<StickerPack> packs = StickerPackLoader.fetchStickerPacksWithoutValidation(this);
            adapter.setPacks(packs);
            emptyText.setVisibility(packs.isEmpty() ? View.VISIBLE : View.GONE);
        } catch (Exception e) {
            adapter.setPacks(List.of());
            emptyText.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Could not load packs", Toast.LENGTH_SHORT).show();
        }
    }
}
