package com.pratikpatil.stickerrr;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;
import com.pratikpatil.stickerrr.createpack.ImageHelper;

import java.io.IOException;

/**
 * Edit a sticker image with options: Crop, Add text, Remove background.
 * Receives EXTRA_IMAGE_URI; returns EXTRA_RESULT_URI on Done.
 */
public class StickerEditorActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URI = "image_uri";
    public static final String EXTRA_RESULT_URI = "result_uri";

    private ImageView imgPreview;
    private Uri currentUri;

    private final ActivityResultLauncher<CropImageContractOptions> cropImage = registerForActivityResult(
            new CropImageContract(),
            result -> {
                if (result.isSuccessful() && result.getUriContent() != null) {
                    currentUri = result.getUriContent();
                    loadPreview();
                } else if (result.getError() != null) {
                    Toast.makeText(this, "Crop failed", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sticker_editor);

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        imgPreview = findViewById(R.id.imgPreview);
        Uri inputUri = getIntent().getParcelableExtra(EXTRA_IMAGE_URI);
        if (inputUri == null) {
            Toast.makeText(this, "No image", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUri = inputUri;
        loadPreview();

        findViewById(R.id.btnCrop).setOnClickListener(v -> launchCrop());
        findViewById(R.id.btnAddText).setOnClickListener(v -> showAddTextDialog());
        findViewById(R.id.btnRemoveBg).setOnClickListener(v -> applyRemoveBackground());
        findViewById(R.id.btnDone).setOnClickListener(v -> done());
    }

    @Override
    public boolean onSupportNavigateUp() {
        setResult(RESULT_CANCELED);
        finish();
        return true;
    }

    private void loadPreview() {
        imgPreview.setImageURI(currentUri);
    }

    private void launchCrop() {
        CropImageOptions options = new CropImageOptions();
        options.guidelines = CropImageView.Guidelines.ON;
        options.aspectRatioX = 1;
        options.aspectRatioY = 1;
        options.outputCompressFormat = Bitmap.CompressFormat.PNG;

        cropImage.launch(new CropImageContractOptions(currentUri, options));
    }

    private void showAddTextDialog() {
        EditText editText = new EditText(this);
        editText.setHint(getString(R.string.text_hint));
        editText.setPadding(48, 32, 48, 32);
        editText.setSingleLine(true);

        new AlertDialog.Builder(this)
                .setTitle(R.string.add_text)
                .setView(editText)
                .setPositiveButton(R.string.add, (dialog, which) -> {
                    String text = editText.getText() != null ? editText.getText().toString().trim() : "";
                    if (!TextUtils.isEmpty(text)) {
                        applyAddText(text);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void applyAddText(String text) {
        try {
            Bitmap bitmap = ImageHelper.loadBitmap(this, currentUri);
            if (bitmap == null) {
                Toast.makeText(this, "Could not load image", Toast.LENGTH_SHORT).show();
                return;
            }
            float textSize = Math.min(bitmap.getWidth(), bitmap.getHeight()) * 0.15f;
            Bitmap withText = ImageHelper.drawTextOnBitmap(bitmap, text, Color.WHITE, textSize);
            if (bitmap != withText) bitmap.recycle();
            Uri newUri = ImageHelper.saveBitmapToCacheUri(this, withText);
            withText.recycle();
            currentUri = newUri;
            loadPreview();
        } catch (IOException e) {
            Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void applyRemoveBackground() {
        try {
            Bitmap bitmap = ImageHelper.loadBitmap(this, currentUri);
            if (bitmap == null) {
                Toast.makeText(this, "Could not load image", Toast.LENGTH_SHORT).show();
                return;
            }
            Bitmap noBg = ImageHelper.removeBackground(bitmap);
            if (bitmap != noBg) bitmap.recycle();
            Uri newUri = ImageHelper.saveBitmapToCacheUri(this, noBg);
            noBg.recycle();
            currentUri = newUri;
            loadPreview();
            Toast.makeText(this, "Background removed (white areas)", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void done() {
        Intent data = new Intent();
        data.setData(currentUri);
        data.putExtra(EXTRA_RESULT_URI, currentUri);
        setResult(RESULT_OK, data);
        finish();
    }
}
