package com.pratikpatil.stickerrr.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Displays a bitmap with an overlay text that can be dragged.
 * Position and size are in bitmap coordinates. Use setTextSize() for resize (e.g. from SeekBar).
 */
public class ImageWithTextView extends View {

    private Bitmap bitmap;
    private String text = "";
    private int textColor = 0xFFFFFFFF;
    private float textX;  // left edge, bitmap coords
    private float textY;  // baseline, bitmap coords
    private float textSize = 48f;  // pixels in bitmap space
    private float textRotation = 0f;  // degrees

    private float scale = 1f;
    private float offsetX;
    private float offsetY;
    private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect textBounds = new Rect();

    private boolean dragging;
    private float lastTouchBx;
    private float lastTouchBy;

    public ImageWithTextView(Context context) {
        super(context);
    }

    public ImageWithTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ImageWithTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
        if (bitmap != null && text != null && !text.isEmpty()) {
            centerTextInitially();
        }
        invalidate();
    }

    public void setText(String text) {
        this.text = text != null ? text : "";
        if (bitmap != null && !this.text.isEmpty()) {
            centerTextInitially();
        }
        invalidate();
    }

    public void setTextColor(int textColor) {
        this.textColor = textColor;
        invalidate();
    }

    public int getTextColor() {
        return textColor;
    }

    /** Set text size in bitmap pixels (used for resize). */
    public void setTextSizePx(float sizePx) {
        this.textSize = Math.max(12f, Math.min(200f, sizePx));
        invalidate();
    }

    public float getTextSizePx() {
        return textSize;
    }

    /** Set rotation in degrees (0–360). */
    public void setTextRotation(float degrees) {
        this.textRotation = degrees % 360f;
        if (this.textRotation < 0) this.textRotation += 360f;
        invalidate();
    }

    public float getTextRotation() {
        return textRotation;
    }

    public float getTextX() { return textX; }
    public float getTextY() { return textY; }
    public String getText() { return text; }

    private void centerTextInitially() {
        if (bitmap == null || text.isEmpty()) return;
        textPaint.setTextSize(textSize);
        textPaint.getTextBounds(text, 0, text.length(), textBounds);
        float w = bitmap.getWidth();
        float h = bitmap.getHeight();
        textX = (w - textBounds.width()) / 2f;
        textY = h / 2f - (textBounds.top + textBounds.bottom) / 2f;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateScaleAndOffset();
    }

    private void updateScaleAndOffset() {
        if (bitmap == null || getWidth() <= 0 || getHeight() <= 0) return;
        int vw = getWidth();
        int vh = getHeight();
        int bw = bitmap.getWidth();
        int bh = bitmap.getHeight();
        scale = Math.min((float) vw / bw, (float) vh / bh);
        offsetX = (vw - bw * scale) / 2f;
        offsetY = (vh - bh * scale) / 2f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (bitmap == null) return;
        updateScaleAndOffset();
        RectF dst = new RectF(offsetX, offsetY, offsetX + bitmap.getWidth() * scale, offsetY + bitmap.getHeight() * scale);
        canvas.drawBitmap(bitmap, null, dst, bitmapPaint);

        if (text.isEmpty()) return;
        textPaint.setColor(textColor);
        textPaint.setTextSize(textSize * scale);
        textPaint.getTextBounds(text, 0, text.length(), textBounds);
        float vx = offsetX + textX * scale;
        float vy = offsetY + textY * scale;
        float pivotX = vx + textBounds.width() / 2f;
        float pivotY = vy - (textBounds.top + textBounds.bottom) / 2f;
        canvas.save();
        canvas.rotate(textRotation, pivotX, pivotY);
        canvas.drawText(text, vx, vy, textPaint);
        canvas.restore();
    }

    private void viewToBitmap(float vx, float vy, float[] outBxBy) {
        outBxBy[0] = (vx - offsetX) / scale;
        outBxBy[1] = (vy - offsetY) / scale;
    }

    private boolean isInsideText(float bx, float by) {
        if (text.isEmpty()) return false;
        textPaint.setTextSize(textSize);
        textPaint.getTextBounds(text, 0, text.length(), textBounds);
        float left = textX;
        float right = textX + textBounds.width();
        float top = textY + textBounds.top;
        float bottom = textY + textBounds.bottom;
        return bx >= left - 20 && bx <= right + 20 && by >= top - 20 && by <= bottom + 20;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (bitmap == null || text.isEmpty()) return false;
        float[] b = new float[2];
        viewToBitmap(event.getX(), event.getY(), b);
        float bx = b[0];
        float by = b[1];

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (isInsideText(bx, by)) {
                    dragging = true;
                    lastTouchBx = bx;
                    lastTouchBy = by;
                    return true;
                }
                return false;
            case MotionEvent.ACTION_MOVE:
                if (dragging) {
                    float dx = bx - lastTouchBx;
                    float dy = by - lastTouchBy;
                    textX += dx;
                    textY += dy;
                    textPaint.setTextSize(textSize);
                    textPaint.getTextBounds(text, 0, text.length(), textBounds);
                    float minX = 0;
                    float maxX = bitmap.getWidth() - textBounds.width();
                    float minY = -textBounds.top;
                    float maxY = bitmap.getHeight() - textBounds.bottom;
                    textX = Math.max(minX, Math.min(maxX, textX));
                    textY = Math.max(minY, Math.min(maxY, textY));
                    lastTouchBx = bx;
                    lastTouchBy = by;
                    invalidate();
                    return true;
                }
                return false;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                dragging = false;
                return true;
            default:
                return false;
        }
    }
}
