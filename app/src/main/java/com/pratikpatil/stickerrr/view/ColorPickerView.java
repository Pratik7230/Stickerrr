package com.pratikpatil.stickerrr.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Simple HSB color picker: square (saturation vs brightness) + horizontal hue strip.
 * Draggable circular selectors on both.
 */
public class ColorPickerView extends View {

    private float hue = 0f;       // 0-360
    private float saturation = 1f; // 0-1
    private float value = 1f;     // 0-1 (brightness)

    private RectF satValRect;
    private RectF hueRect;
    private final PointF satValSelector = new PointF();
    private float hueSelectorX;

    private Bitmap satValBitmap;
    private Bitmap hueBitmap;
    private final Paint selectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final float SELECTOR_RADIUS_DP = 12f;
    private static final float HUE_BAR_HEIGHT_DP = 24f;
    private static final float PADDING_DP = 8f;

    private float selectorRadiusPx;
    private float hueBarHeightPx;
    private float paddingPx;

    private boolean draggingSatVal;
    private boolean draggingHue;

    public interface OnColorChangedListener {
        void onColorChanged(int color);
    }

    private OnColorChangedListener listener;

    public ColorPickerView(Context context) {
        super(context);
        init();
    }

    public ColorPickerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ColorPickerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        float density = getResources().getDisplayMetrics().density;
        selectorRadiusPx = SELECTOR_RADIUS_DP * density;
        hueBarHeightPx = HUE_BAR_HEIGHT_DP * density;
        paddingPx = PADDING_DP * density;

        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2f * density);
        borderPaint.setColor(Color.WHITE);
        selectorPaint.setStyle(Paint.Style.STROKE);
        selectorPaint.setStrokeWidth(2f * density);
        selectorPaint.setColor(Color.WHITE);
    }

    public void setColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hue = hsv[0];
        saturation = hsv[1];
        value = hsv[2];
        updateSelectorPositions();
        invalidate();
    }

    public int getColor() {
        return Color.HSVToColor(new float[]{hue, saturation, value});
    }

    public void setOnColorChangedListener(OnColorChangedListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float gap = paddingPx * 2;
        float availableHeight = h - hueBarHeightPx - gap - paddingPx * 2;
        float size = Math.min(w - paddingPx * 2, availableHeight);
        satValRect = new RectF(paddingPx, paddingPx, paddingPx + size, paddingPx + size);
        hueRect = new RectF(paddingPx, satValRect.bottom + gap, w - paddingPx, satValRect.bottom + gap + hueBarHeightPx);
        recycleBitmaps();
        updateSelectorPositions();
    }

    private void updateSelectorPositions() {
        if (satValRect == null || hueRect == null) return;
        satValSelector.x = satValRect.left + saturation * satValRect.width();
        satValSelector.y = satValRect.top + (1f - value) * satValRect.height();
        hueSelectorX = hueRect.left + (hue / 360f) * hueRect.width();
    }

    private void recycleBitmaps() {
        if (satValBitmap != null) {
            satValBitmap.recycle();
            satValBitmap = null;
        }
        if (hueBitmap != null) {
            hueBitmap.recycle();
            hueBitmap = null;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (satValRect == null || hueRect == null) return;

        if (satValBitmap == null || satValBitmap.getWidth() != (int) satValRect.width()) {
            satValBitmap = createSatValBitmap((int) satValRect.width(), (int) satValRect.height());
        }
        if (hueBitmap == null || hueBitmap.getWidth() != (int) hueRect.width()) {
            hueBitmap = createHueBitmap((int) hueRect.width(), (int) hueRect.height());
        }

        if (satValBitmap != null) {
            canvas.drawBitmap(satValBitmap, satValRect.left, satValRect.top, null);
        }
        if (hueBitmap != null) {
            canvas.drawBitmap(hueBitmap, hueRect.left, hueRect.top, null);
        }

        // Selector on sat/val square (white circle with border)
        selectorPaint.setColor(Color.WHITE);
        canvas.drawCircle(satValSelector.x, satValSelector.y, selectorRadiusPx, selectorPaint);
        borderPaint.setColor(Color.BLACK);
        borderPaint.setStrokeWidth(1f);
        canvas.drawCircle(satValSelector.x, satValSelector.y, selectorRadiusPx, borderPaint);

        // Selector on hue bar
        float hueCy = hueRect.centerY();
        selectorPaint.setColor(Color.WHITE);
        canvas.drawCircle(hueSelectorX, hueCy, selectorRadiusPx, selectorPaint);
        borderPaint.setColor(Color.BLACK);
        canvas.drawCircle(hueSelectorX, hueCy, selectorRadiusPx, borderPaint);
    }

    private Bitmap createSatValBitmap(int w, int h) {
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        int[] pixels = new int[w * h];
        float[] hsv = new float[3];
        hsv[0] = hue;
        for (int y = 0; y < h; y++) {
            float v = 1f - (float) y / h;
            for (int x = 0; x < w; x++) {
                float s = (float) x / w;
                hsv[1] = s;
                hsv[2] = v;
                pixels[y * w + x] = Color.HSVToColor(hsv);
            }
        }
        bmp.setPixels(pixels, 0, w, 0, 0, w, h);
        return bmp;
    }

    private Bitmap createHueBitmap(int w, int h) {
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        int[] pixels = new int[w * h];
        float[] hsv = new float[3];
        hsv[1] = 1f;
        hsv[2] = 1f;
        for (int x = 0; x < w; x++) {
            hsv[0] = 360f * x / w;
            int c = Color.HSVToColor(hsv);
            for (int y = 0; y < h; y++) {
                pixels[y * w + x] = c;
            }
        }
        bmp.setPixels(pixels, 0, w, 0, 0, w, h);
        return bmp;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (satValRect == null || hueRect == null) return false;

        float x = event.getX();
        float y = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (hueRect.contains(x, y)) {
                    draggingHue = true;
                    updateHueFromX(x);
                } else if (satValRect.contains(x, y)) {
                    draggingSatVal = true;
                    updateSatValFromPoint(x, y);
                }
                return draggingHue || draggingSatVal;
            case MotionEvent.ACTION_MOVE:
                if (draggingHue) {
                    updateHueFromX(x);
                    return true;
                }
                if (draggingSatVal) {
                    updateSatValFromPoint(x, y);
                    return true;
                }
                return false;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                draggingHue = false;
                draggingSatVal = false;
                return true;
            default:
                return false;
        }
    }

    private void updateHueFromX(float x) {
        hue = (x - hueRect.left) / hueRect.width() * 360f;
        hue = Math.max(0, Math.min(360, hue));
        hueSelectorX = x;
        recycleBitmaps();
        invalidate();
        notifyColorChanged();
    }

    private void updateSatValFromPoint(float x, float y) {
        saturation = (x - satValRect.left) / satValRect.width();
        saturation = Math.max(0, Math.min(1, saturation));
        value = 1f - (y - satValRect.top) / satValRect.height();
        value = Math.max(0, Math.min(1, value));
        satValSelector.x = x;
        satValSelector.y = y;
        invalidate();
        notifyColorChanged();
    }

    private void notifyColorChanged() {
        if (listener != null) {
            listener.onColorChanged(getColor());
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        recycleBitmaps();
    }
}
