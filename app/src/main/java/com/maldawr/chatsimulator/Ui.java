package com.maldawr.chatsimulator;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
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

    public static int bg(Context c) { return isDark(c) ? Color.rgb(17, 27, 33) : Color.rgb(255, 255, 255); }
    public static int card(Context c) { return isDark(c) ? Color.rgb(31, 44, 51) : Color.WHITE; }
    public static int text(Context c) { return isDark(c) ? Color.rgb(233, 237, 239) : Color.rgb(17, 27, 33); }
    public static int sub(Context c) { return isDark(c) ? Color.rgb(134, 150, 160) : Color.rgb(102, 119, 129); }
    public static int divider(Context c) { return isDark(c) ? Color.rgb(42, 57, 66) : Color.rgb(236, 239, 241); }
    public static int chatBg(Context c) { return isDark(c) ? Color.rgb(11, 20, 26) : Color.rgb(239, 234, 226); }
    public static int brand() { return Color.rgb(11, 107, 93); }
    public static int brandDark() { return Color.rgb(8, 83, 74); }
    public static int brandBright() { return Color.rgb(33, 161, 121); }
    public static int red() { return Color.rgb(211, 60, 60); }

    public static GradientDrawable rounded(int color, float radiusDp, Context c) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(c, Math.round(radiusDp)));
        return d;
    }

    public static GradientDrawable circle(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setShape(GradientDrawable.OVAL);
        return d;
    }

    public static TextView label(Context c, String value, float sp, boolean bold) {
        TextView v = new TextView(c);
        v.setText(value);
        v.setTextSize(sp);
        v.setTextColor(text(c));
        v.setIncludeFontPadding(false);
        v.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        return v;
    }

    public static TextView iconButton(Context c, String glyph, int sizeDp, float sp, int bgColor, int textColor) {
        TextView v = new TextView(c);
        v.setText(glyph);
        v.setTextSize(sp);
        v.setTextColor(textColor);
        v.setGravity(Gravity.CENTER);
        v.setIncludeFontPadding(false);
        if (bgColor != Color.TRANSPARENT) v.setBackground(circle(bgColor));
        v.setLayoutParams(new ViewGroup.LayoutParams(dp(c, sizeDp), dp(c, sizeDp)));
        v.setClickable(true);
        v.setFocusable(true);
        return v;
    }

    public static TextView oneLine(Context c, String value, float sp, int color) {
        TextView v = label(c, value, sp, false);
        v.setTextColor(color);
        v.setSingleLine(true);
        v.setEllipsize(TextUtils.TruncateAt.END);
        return v;
    }

    public static View divider(Context c, int startDp) {
        View v = new View(c);
        v.setBackgroundColor(divider(c));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(c, 1));
        lp.setMargins(dp(c, startDp), 0, 0, 0);
        v.setLayoutParams(lp);
        return v;
    }

    public static TextView safetyBanner(Context c) {
        TextView banner = new TextView(c);
        banner.setText("محاكاة شخصية • بيانات خيالية");
        banner.setTextSize(10);
        banner.setTextColor(sub(c));
        banner.setGravity(Gravity.CENTER);
        banner.setIncludeFontPadding(false);
        banner.setPadding(dp(c, 8), dp(c, 4), dp(c, 8), dp(c, 4));
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
                image.setBackground(circle(Color.rgb(95, 118, 126)));
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
        initials.setIncludeFontPadding(false);
        initials.setBackground(circle(Color.rgb(79, 118, 126)));
        frame.addView(initials, new FrameLayout.LayoutParams(px, px));
        return frame;
    }

    public static LinearLayout cardRow(Context c) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(c, 14), dp(c, 11), dp(c, 14), dp(c, 11));
        row.setBackground(rounded(card(c), 12, c));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(dp(c, 10), dp(c, 5), dp(c, 10), dp(c, 5));
        row.setLayoutParams(p);
        return row;
    }
}
