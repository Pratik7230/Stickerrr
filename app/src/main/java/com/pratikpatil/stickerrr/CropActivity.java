package com.pratikpatil.stickerrr;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.canhub.cropper.CropImageView;
import com.pratikpatil.stickerrr.createpack.ImageHelper;

import java.io.IOException;
import java.util.concurrent.Executors;

/**
 * In-app crop screen with explicit Done and Cancel buttons.
 * Receives EXTRA_IMAGE_URI; returns EXTRA_RESULT_URI on Done.
 */
public class CropActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URI = "image_uri";
    public static final String EXTRA_RESULT_URI = "result_uri";

    private CropImageView cropImageView;
    private Uri inputUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop);

        inputUri = getIntent().getParcelableExtra(EXTRA_IMAGE_URI);
        if (inputUri == null) {
            Toast.makeText(this, "No image", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        cropImageView = findViewById(R.id.cropImageView);
        cropImageView.setGuidelines(CropImageView.Guidelines.ON);
        cropImageView.setAspectRatio(1, 1);
        cropImageView.setFixedAspectRatio(true);
        cropImageView.setImageUriAsync(inputUri);

        findViewById(R.id.btnCancel).setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        findViewById(R.id.btnDone).setOnClickListener(v -> onCropDone());
    }

    @Override
    public boolean onSupportNavigateUp() {
        setResult(RESULT_CANCELED);
        finish();
        return true;
    }

    private void onCropDone() {
        View doneBtn = findViewById(R.id.btnDone);
        doneBtn.setEnabled(false);
        Executors.newSingleThreadExecutor().execute(() -> {
            Bitmap bitmap = cropImageView.getCroppedImage();
            runOnUiThread(() -> {
                doneBtn.setEnabled(true);
                if (bitmap == null) {
                    Toast.makeText(this, "Crop failed", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    Uri savedUri = ImageHelper.saveBitmapToCacheUri(this, bitmap);
                    bitmap.recycle();
                    Intent data = new Intent();
                    data.setData(savedUri);
                    data.putExtra(EXTRA_RESULT_URI, savedUri);
                    setResult(RESULT_OK, data);
                    finish();
                } catch (IOException e) {
                    Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
