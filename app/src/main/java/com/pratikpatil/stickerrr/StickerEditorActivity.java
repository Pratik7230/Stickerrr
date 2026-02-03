package com.pratikpatil.stickerrr;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.pratikpatil.stickerrr.createpack.ImageHelper;
import com.pratikpatil.stickerrr.view.ImageWithTextView;

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

    private final ActivityResultLauncher<Intent> cropActivity = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri == null) uri = result.getData().getParcelableExtra(CropActivity.EXTRA_RESULT_URI);
                    if (uri != null) {
                        currentUri = uri;
                        loadPreview();
                    }
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
        Intent i = new Intent(this, CropActivity.class);
        i.putExtra(CropActivity.EXTRA_IMAGE_URI, currentUri);
        cropActivity.launch(i);
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
                        showAddTextOverlay(text);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showAddTextOverlay(String text) {
        try {
            Bitmap bitmap = ImageHelper.loadBitmap(this, currentUri);
            if (bitmap == null) {
                Toast.makeText(this, "Could not load image", Toast.LENGTH_SHORT).show();
                return;
            }
            View root = LayoutInflater.from(this).inflate(R.layout.dialog_add_text, null);
            ImageWithTextView imageWithText = root.findViewById(R.id.imageWithText);
            SeekBar seekTextSize = root.findViewById(R.id.seekTextSize);
            SeekBar seekRotation = root.findViewById(R.id.seekRotation);
            LinearLayout colorSwatches = root.findViewById(R.id.colorSwatches);

            imageWithText.setBitmap(bitmap);
            imageWithText.setText(text);
            int[] colors = {
                    Color.WHITE, Color.BLACK, Color.RED, Color.BLUE, Color.GREEN,
                    Color.YELLOW, Color.MAGENTA, Color.CYAN, Color.rgb(255, 165, 0), // Orange
                    Color.rgb(128, 0, 128), // Purple
                    Color.rgb(255, 192, 203), // Pink
                    Color.rgb(165, 42, 42)  // Brown
            };
            int selectedColor = Color.WHITE;
            imageWithText.setTextColor(selectedColor);
            final int[] finalSelectedColor = {selectedColor};
            setupColorSwatches(colorSwatches, colors, selectedColor, (color) -> {
                finalSelectedColor[0] = color;
                imageWithText.setTextColor(color);
            });
            float minSize = 20f;
            float maxSize = Math.min(bitmap.getWidth(), bitmap.getHeight()) * 0.35f;
            float initialSize = Math.min(bitmap.getWidth(), bitmap.getHeight()) * 0.15f;
            imageWithText.setTextSizePx(initialSize);
            int progress = maxSize > minSize ? (int) ((initialSize - minSize) / (maxSize - minSize) * 100f) : 50;
            seekTextSize.setProgress(Math.max(0, Math.min(100, progress)));
            seekTextSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        float size = minSize + (maxSize - minSize) * progress / 100f;
                        imageWithText.setTextSizePx(size);
                    }
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
            seekRotation.setProgress(0);
            seekRotation.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        imageWithText.setTextRotation(progress);
                    }
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            AlertDialog overlay = new AlertDialog.Builder(this)
                    .setView(root)
                    .setCancelable(true)
                    .create();
            root.findViewById(R.id.btnCancelText).setOnClickListener(v -> overlay.dismiss());
            root.findViewById(R.id.btnCustomColor).setOnClickListener(v -> {
                showColorPickerDialog(finalSelectedColor[0], (pickedColor) -> {
                    finalSelectedColor[0] = pickedColor;
                    imageWithText.setTextColor(pickedColor);
                });
            });
            root.findViewById(R.id.btnApplyText).setOnClickListener(v -> {
                float textX = imageWithText.getTextX();
                float textY = imageWithText.getTextY();
                float textSizePx = imageWithText.getTextSizePx();
                float rotation = imageWithText.getTextRotation();
                int color = finalSelectedColor[0];
                overlay.dismiss();
                applyAddTextAt(bitmap, text, textX, textY, textSizePx, rotation, color);
            });
            overlay.show();
        } catch (IOException e) {
            Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void applyAddTextAt(Bitmap bitmap, String text, float textX, float textY, float textSizePx, float rotationDegrees, int textColor) {
        try {
            Bitmap withText = ImageHelper.drawTextOnBitmapAt(bitmap, text, textColor, textSizePx, textX, textY, rotationDegrees);
            if (bitmap != withText) bitmap.recycle();
            Uri newUri = ImageHelper.saveBitmapToCacheUri(this, withText);
            withText.recycle();
            currentUri = newUri;
            loadPreview();
        } catch (IOException e) {
            Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupColorSwatches(LinearLayout container, int[] colors, int selectedColor, ColorSelectedListener listener) {
        int size = (int) (48 * getResources().getDisplayMetrics().density);
        int margin = (int) (8 * getResources().getDisplayMetrics().density);
        int strokeWidth = (int) (3 * getResources().getDisplayMetrics().density);

        for (int i = 0; i < colors.length; i++) {
            int color = colors[i];
            View swatch = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(0, 0, margin, 0);
            swatch.setLayoutParams(params);
            if (color == selectedColor) {
                swatch.setBackground(createColorSwatchDrawable(color, strokeWidth, true));
            } else {
                swatch.setBackground(createColorSwatchDrawable(color, strokeWidth, false));
            }
            swatch.setOnClickListener(v -> {
                for (int j = 0; j < container.getChildCount(); j++) {
                    View child = container.getChildAt(j);
                    int childColor = colors[j];
                    child.setBackground(createColorSwatchDrawable(childColor, strokeWidth, childColor == color));
                }
                listener.onColorSelected(color);
            });
            container.addView(swatch);
        }
    }

    private android.graphics.drawable.Drawable createColorSwatchDrawable(int color, int strokeWidth, boolean selected) {
        android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
        drawable.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        drawable.setColor(color);
        if (selected) {
            drawable.setStroke(strokeWidth, Color.BLACK);
        } else {
            drawable.setStroke(strokeWidth, Color.GRAY);
        }
        return drawable;
    }

    private void showColorPickerDialog(int initialColor, ColorSelectedListener listener) {
        View dialogRoot = LayoutInflater.from(this).inflate(R.layout.dialog_color_picker, null);
        View colorPreview = dialogRoot.findViewById(R.id.colorPreview);
        com.pratikpatil.stickerrr.view.ColorPickerView colorPicker = dialogRoot.findViewById(R.id.colorPicker);

        colorPicker.setColor(initialColor);
        colorPreview.setBackgroundColor(initialColor);
        colorPicker.setOnColorChangedListener((color) -> colorPreview.setBackgroundColor(color));

        AlertDialog colorDialog = new AlertDialog.Builder(this)
                .setView(dialogRoot)
                .setCancelable(true)
                .create();

        dialogRoot.findViewById(R.id.btnCancelColor).setOnClickListener(v -> colorDialog.dismiss());
        dialogRoot.findViewById(R.id.btnSelectColor).setOnClickListener(v -> {
            listener.onColorSelected(colorPicker.getColor());
            colorDialog.dismiss();
        });

        colorDialog.show();
    }

    private interface ColorSelectedListener {
        void onColorSelected(int color);
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
