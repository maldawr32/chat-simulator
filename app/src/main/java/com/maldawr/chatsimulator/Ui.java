package com.maldawr.chatsimulator;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class Ui {
    private Ui() {}

    public static int dp(Context c, int value) {
        return Math.round(value * c.getResources().getDisplayMetrics().density);
    }

    public static boolean isDark(Context c) {
        int mode = c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return mode == Configuration.UI_MODE_NIGHT_YES;
    }

    public static int bg(Context c) { return isDark(c) ? Color.rgb(17, 24, 26) : Color.rgb(246, 249, 249); }
    public static int card(Context c) { return isDark(c) ? Color.rgb(31, 42, 45) : Color.WHITE; }
    public static int text(Context c) { return isDark(c) ? Color.rgb(235, 240, 241) : Color.rgb(28, 37, 39); }
    public static int sub(Context c) { return isDark(c) ? Color.rgb(170, 183, 186) : Color.rgb(100, 112, 115); }
    public static int teal() { return Color.rgb(31, 111, 120); }
    public static int red() { return Color.rgb(183, 55, 63); }

    public static GradientDrawable rounded(int color, float radiusDp, Context c) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(c, (int) radiusDp));
        return d;
    }

    public static GradientDrawable circle(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setShape(GradientDrawable.OVAL);
        return d;
    }

    public static TextView label(Context c, String text, float sp, boolean bold) {
        TextView v = new TextView(c);
        v.setText(text);
        v.setTextSize(sp);
        v.setTextColor(text(c));
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        return v;
    }

    public static Button button(Context c, String title) {
        Button b = new Button(c);
        b.setText(title);
        b.setAllCaps(false);
        b.setTextSize(14);
        return b;
    }

    public static TextView safetyBanner(Context c) {
        TextView banner = new TextView(c);
        banner.setText("SIMULATION / FICTIONAL DATA - NOT REAL MESSAGES OR CALLS");
        banner.setTextSize(11);
        banner.setTextColor(Color.WHITE);
        banner.setGravity(Gravity.CENTER);
        banner.setPadding(dp(c, 8), dp(c, 7), dp(c, 8), dp(c, 7));
        banner.setBackgroundColor(Color.rgb(117, 76, 30));
        return banner;
    }

    public static View avatar(Context c, Store.Bot bot, int sizeDp) {
        int px = dp(c, sizeDp);
        FrameLayout frame = new FrameLayout(c);
        frame.setLayoutParams(new ViewGroup.LayoutParams(px, px));

        if (bot != null && bot.avatarUri != null && !bot.avatarUri.isEmpty()) {
            try {
                ImageView image = new ImageView(c);
                image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                image.setImageURI(Uri.parse(bot.avatarUri));
                image.setClipToOutline(true);
                image.setBackground(circle(Color.rgb(73, 112, 119)));
                frame.addView(image, new FrameLayout.LayoutParams(px, px));
                return frame;
            } catch (Exception ignored) {}
        }

        TextView initials = new TextView(c);
        String name = bot == null ? "S" : bot.name;
        String first = name == null || name.trim().isEmpty() ? "B" : name.trim().substring(0, 1).toUpperCase();
        initials.setText(first);
        initials.setTextColor(Color.WHITE);
        initials.setTextSize(sizeDp * 0.34f);
        initials.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        initials.setGravity(Gravity.CENTER);
        initials.setBackground(circle(Color.rgb(52, 112, 121)));
        frame.addView(initials, new FrameLayout.LayoutParams(px, px));
        return frame;
    }

    public static LinearLayout cardRow(Context c) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(c, 12), dp(c, 10), dp(c, 12), dp(c, 10));
        row.setBackground(rounded(card(c), 16, c));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(dp(c, 10), dp(c, 5), dp(c, 10), dp(c, 5));
        row.setLayoutParams(p);
        return row;
    }
}
