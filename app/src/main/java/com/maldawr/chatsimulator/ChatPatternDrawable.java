package com.maldawr.chatsimulator;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

public class ChatPatternDrawable extends Drawable {
    private final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int step;

    public ChatPatternDrawable(Context c) {
        bg.setColor(Color.rgb(13, 23, 28));
        ink.setColor(Color.rgb(29, 43, 49));
        ink.setStyle(Paint.Style.STROKE);
        ink.setStrokeWidth(Ui.dp(c, 1));
        ink.setAlpha(150);
        step = Ui.dp(c, 58);
    }

    @Override public void draw(Canvas canvas) {
        Rect b = getBounds();
        canvas.drawRect(b, bg);
        int index = 0;
        for (int y = 0; y < b.height() + step; y += step) {
            for (int x = 0; x < b.width() + step; x += step) {
                int ox = x + (((y / step) % 2) * step / 2);
                drawMotif(canvas, ox, y, index++ % 6);
            }
        }
    }

    private void drawMotif(Canvas c, float x, float y, int type) {
        float s = step * 0.26f;
        switch (type) {
            case 0:
                c.drawCircle(x + s, y + s, s * 0.55f, ink);
                c.drawCircle(x + s, y + s, s * 0.18f, ink);
                break;
            case 1:
                c.drawRect(x, y + s * 0.25f, x + s * 1.4f, y + s, ink);
                c.drawLine(x + s * 0.25f, y + s * 0.25f, x + s * 0.25f, y, ink);
                c.drawLine(x + s, y + s * 0.25f, x + s, y, ink);
                break;
            case 2:
                Path p = new Path();
                p.moveTo(x, y + s);
                p.lineTo(x + s * 0.55f, y);
                p.lineTo(x + s * 1.1f, y + s);
                p.close();
                c.drawPath(p, ink);
                break;
            case 3:
                c.drawCircle(x + s * 0.35f, y + s * 0.45f, s * 0.24f, ink);
                c.drawCircle(x + s * 0.85f, y + s * 0.45f, s * 0.24f, ink);
                c.drawLine(x + s * 0.45f, y + s * 0.8f, x + s * 0.75f, y + s * 0.8f, ink);
                break;
            case 4:
                c.drawLine(x, y + s * 0.5f, x + s * 1.3f, y + s * 0.5f, ink);
                c.drawLine(x + s * 0.65f, y, x + s * 0.65f, y + s, ink);
                c.drawCircle(x + s * 0.65f, y + s * 0.5f, s * 0.45f, ink);
                break;
            default:
                c.drawRect(x + s * 0.1f, y + s * 0.1f, x + s, y + s * 0.9f, ink);
                c.drawLine(x + s * 0.1f, y + s * 0.1f, x + s, y + s * 0.9f, ink);
                break;
        }
    }

    @Override public void setAlpha(int alpha) { ink.setAlpha(alpha); }
    @Override public void setColorFilter(android.graphics.ColorFilter colorFilter) { ink.setColorFilter(colorFilter); }
    @Override public int getOpacity() { return android.graphics.PixelFormat.OPAQUE; }
}
