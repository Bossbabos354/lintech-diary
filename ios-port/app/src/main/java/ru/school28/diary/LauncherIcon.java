package ru.school28.diary;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.RoundRectShape;

import androidx.core.content.ContextCompat;

public class LauncherIcon extends Drawable {
    private final Paint bgPaint;
    private final Paint letterPaint;
    private final String letter = "Д";

    public LauncherIcon(Context context) {
        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(Color.rgb(63, 81, 181));
        letterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        letterPaint.setColor(Color.WHITE);
        letterPaint.setTextAlign(Paint.Align.CENTER);
        letterPaint.setTextSize(96f);
        letterPaint.setFakeBoldText(true);
    }

    @Override
    public void draw(Canvas canvas) {
        RectF r = new RectF(getBounds());
        canvas.drawRoundRect(r, 48f, 48f, bgPaint);
        float cx = r.centerX();
        float cy = r.centerY() + 4f;
        Paint.FontMetrics fm = letterPaint.getFontMetrics();
        float baseline = cy - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(letter, cx, baseline, letterPaint);
    }

    @Override
    public void setAlpha(int alpha) { }

    @Override
    public void setColorFilter(android.graphics.ColorFilter colorFilter) { }

    @Override
    public int getOpacity() {
        return android.graphics.PixelFormat.OPAQUE;
    }
}