package com.maldawr.chatsimulator;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int CHAT_SOUND = 41, CALL_SOUND = 42;
    private static final int DARK = Color.rgb(11, 20, 26);
    private static final int PANEL = Color.rgb(31, 44, 51);
    private static final int SEARCH = Color.rgb(32, 39, 43);
    private static final int GREEN = Color.rgb(37, 211, 102);
    private static final int MUTED = Color.rgb(134, 150, 160);

    private LinearLayout content;
    private LinearLayout nav;
    private LinearLayout searchHost;
    private EditText searchInput;
    private TextView topTitle;
    private TextView fab;
    private int page = 0;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        getWindow().setStatusBarColor(DARK);
        getWindow().setNavigationBarColor(DARK);
        Store.ensureSeeded(this);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 90);
        }
        build();
    }

    @Override protected void onResume() {
        super.onResume();
        if (content != null) render();
    }

    private void build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        root.setBackgroundColor(DARK);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setBackgroundColor(DARK);
        header.setOnApplyWindowInsetsListener((v, insets) -> {
            int top;
            if (Build.VERSION.SDK_INT >= 30) top = insets.getInsets(WindowInsets.Type.statusBars()).top;
            else top = insets.getSystemWindowInsetTop();
            v.setPadding(0, top, 0, 0);
            return insets;
        });

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.setPadding(Ui.dp(this, 20), Ui.dp(this, 6), Ui.dp(this, 14), 0);

        topTitle = new TextView(this);
        topTitle.setText("Chat Simulator");
        topTitle.setTextColor(Color.WHITE);
        topTitle.setTextSize(29);
        topTitle.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        topTitle.setGravity(Gravity.CENTER_VERTICAL);
        topTitle.setIncludeFontPadding(false);
        titleRow.addView(topTitle, new LinearLayout.LayoutParams(0, Ui.dp(this, 64), 1f));

        TextView sim = Ui.label(this, "SIM", 10, true);
        sim.setTextColor(GREEN);
        sim.setGravity(Gravity.CENTER);
        sim.setBackground(Ui.rounded(Color.rgb(25, 34, 38), 12, this));
        LinearLayout.LayoutParams simp = new LinearLayout.LayoutParams(Ui.dp(this, 40), Ui.dp(this, 28));
        simp.setMargins(0, 0, Ui.dp(this, 8), 0);
        titleRow.addView(sim, simp);

        TextView camera = Ui.iconButton(this, "▣", 46, 25, Color.TRANSPARENT, Color.WHITE);
        camera.setContentDescription("Camera");
        titleRow.addView(camera);
        TextView more = Ui.iconButton(this, "⋮", 42, 28, Color.TRANSPARENT, Color.WHITE);
        more.setContentDescription("More");
        more.setOnClickListener(v -> { page = 3; render(); });
        titleRow.addView(more);
        header.addView(titleRow);

        searchHost = new LinearLayout(this);
        searchHost.setPadding(Ui.dp(this, 20), Ui.dp(this, 7), Ui.dp(this, 20), Ui.dp(this, 15));
        searchInput = new EditText(this);
        searchInput.setSingleLine(true);
        searchInput.setTextSize(16);
        searchInput.setTextColor(Color.WHITE);
        searchInput.setHintTextColor(Color.rgb(145, 150, 154));
        searchInput.setHint("⌕   Search...");
        searchInput.setBackground(Ui.rounded(SEARCH, 30, this));
        searchInput.setPadding(Ui.dp(this, 20), 0, Ui.dp(this, 20), 0);
        searchInput.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        searchHost.addView(searchInput, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 58)));
        header.addView(searchHost);
        root.addView(header);

        FrameLayout center = new FrameLayout(this);
        center.setBackgroundColor(DARK);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(DARK);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        content.setBackgroundColor(DARK);
        scroll.addView(content, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        center.addView(scroll, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        fab = Ui.iconButton(this, "▣+", 58, 20, Color.WHITE, DARK);
        fab.setContentDescription("New simulated chat");
        fab.setBackground(Ui.rounded(Color.WHITE, 17, this));
        fab.setOnClickListener(v -> startActivity(new Intent(this, BotEditorActivity.class)));
        FrameLayout.LayoutParams fp = new FrameLayout.LayoutParams(Ui.dp(this, 58), Ui.dp(this, 58), Gravity.END | Gravity.BOTTOM);
        fp.setMargins(Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 18));
        center.addView(fab, fp);
        root.addView(center, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setBackgroundColor(DARK);
        int baseBottom = Ui.dp(this, 6);
        nav.setPadding(Ui.dp(this, 8), Ui.dp(this, 7), Ui.dp(this, 8), baseBottom);
        nav.setOnApplyWindowInsetsListener((v, insets) -> {
            int bottom;
            if (Build.VERSION.SDK_INT >= 30) bottom = insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
            else bottom = insets.getSystemWindowInsetBottom();
            v.setPadding(Ui.dp(this, 8), Ui.dp(this, 7), Ui.dp(this, 8), baseBottom + bottom);
            return insets;
        });
        root.addView(nav);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { if (page == 0) chats(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        setContentView(root);
        root.requestApplyInsets();
        render();
    }

    private void render() {
        content.removeAllViews();
        searchHost.setVisibility(page == 0 ? View.VISIBLE : View.GONE);
        fab.setVisibility(page == 0 ? View.VISIBLE : View.GONE);
        if (page == 0) {
            topTitle.setText("Chat Simulator");
            chats();
        } else if (page == 1) {
            topTitle.setText("Calls");
            calls();
        } else if (page == 2) {
            topTitle.setText("Updates");
            updates();
        } else {
            topTitle.setText("Tools");
            settingsPage();
        }
        renderNav();
    }

    private void renderNav() {
        nav.removeAllViews();
        addNav("▰", "Chats", 0);
        addNav("⌕", "Calls", 1);
        addNav("◉", "Updates", 2);
        addNav("▣", "Tools", 3);
    }

    private void addNav(String icon, String label, int target) {
        boolean selected = page == target;
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(0, 0, 0, Ui.dp(this, 2));
        box.setOnClickListener(v -> { page = target; render(); });

        TextView iv = Ui.label(this, icon, 24, false);
        iv.setGravity(Gravity.CENTER);
        iv.setTextColor(selected ? GREEN : Color.rgb(230, 234, 236));
        if (selected) iv.setBackground(Ui.rounded(Color.rgb(37, 43, 45), 20, this));
        box.addView(iv, new LinearLayout.LayoutParams(Ui.dp(this, 62), Ui.dp(this, 34)));

        TextView tv = Ui.label(this, label, 12, true);
        tv.setTextColor(Color.rgb(239, 242, 243));
        tv.setGravity(Gravity.CENTER);
        box.addView(tv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 27)));
        nav.addView(box, new LinearLayout.LayoutParams(0, Ui.dp(this, 64), 1f));
    }

    private void chats() {
        if (content == null) return;
        content.removeAllViews();
        String query = searchInput == null ? "" : searchInput.getText().toString().trim().toLowerCase(Locale.ROOT);
        List<Store.Bot> bots = Store.loadBots(this);
        int shown = 0;
        for (Store.Bot bot : bots) {
            String hay = (bot.name + " " + bot.lastMessage + " " + bot.phone).toLowerCase(Locale.ROOT);
            if (!query.isEmpty() && !hay.contains(query)) continue;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(Ui.dp(this, 20), Ui.dp(this, 8), Ui.dp(this, 18), Ui.dp(this, 8));
            row.setBackgroundColor(DARK);
            row.addView(Ui.avatar(this, bot, 58));

            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            info.setPadding(Ui.dp(this, 14), 0, 0, 0);

            LinearLayout line1 = new LinearLayout(this);
            line1.setOrientation(LinearLayout.HORIZONTAL);
            line1.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
            line1.setGravity(Gravity.CENTER_VERTICAL);
            TextView name = Ui.oneLine(this, bot.name, 17, Color.WHITE);
            name.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
            line1.addView(name, new LinearLayout.LayoutParams(0, Ui.dp(this, 29), 1f));
            TextView time = Ui.oneLine(this, Store.formatTime(bot.lastTime), 12, bot.unread > 0 ? GREEN : MUTED);
            time.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            line1.addView(time, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, Ui.dp(this, 29)));
            info.addView(line1);

            LinearLayout line2 = new LinearLayout(this);
            line2.setOrientation(LinearLayout.HORIZONTAL);
            line2.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
            line2.setGravity(Gravity.CENTER_VERTICAL);
            TextView preview = Ui.oneLine(this, bot.lastMessage, 15, MUTED);
            line2.addView(preview, new LinearLayout.LayoutParams(0, Ui.dp(this, 30), 1f));
            if (bot.unread > 0) {
                TextView badge = Ui.label(this, String.valueOf(Math.min(bot.unread, 9)), 11, true);
                badge.setTextColor(DARK);
                badge.setGravity(Gravity.CENTER);
                badge.setBackground(Ui.circle(GREEN));
                LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(Ui.dp(this, 25), Ui.dp(this, 25));
                bp.setMargins(Ui.dp(this, 8), 0, 0, 0);
                line2.addView(badge, bp);
            }
            info.addView(line2);
            row.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            row.setOnClickListener(v -> {
                Intent in = new Intent(this, ChatActivity.class);
                in.putExtra("bot_id", bot.id);
                startActivity(in);
            });
            row.setOnLongClickListener(v -> {
                Intent in = new Intent(this, BotEditorActivity.class);
                in.putExtra("bot_id", bot.id);
                startActivity(in);
                return true;
            });

            content.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 88)));
            shown++;
        }
        if (shown == 0) {
            TextView empty = Ui.label(this, "No simulated chats found", 15, false);
            empty.setTextColor(MUTED);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, Ui.dp(this, 48), 0, Ui.dp(this, 48));
            content.addView(empty);
        }
    }

    private void calls() {
        List<Store.CallItem> calls = Store.loadCalls(this);
        for (Store.CallItem c : calls) {
            Store.Bot bot = Store.getBot(this, c.botId);
            if (bot == null) continue;
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(Ui.dp(this, 20), Ui.dp(this, 10), Ui.dp(this, 18), Ui.dp(this, 10));
            row.setBackgroundColor(DARK);
            row.addView(Ui.avatar(this, bot, 56));
            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            info.setPadding(Ui.dp(this, 14), 0, 0, 0);
            TextView n = Ui.label(this, bot.name, 17, true); n.setTextColor(Color.WHITE); info.addView(n);
            String kind = "missed".equals(c.type) ? "↙ Missed" : "incoming".equals(c.type) ? "↙ Incoming" : "↗ Outgoing";
            TextView sub = Ui.label(this, kind + "  •  " + Store.formatDateTime(c.time), 13, false);
            sub.setTextColor("missed".equals(c.type) ? Color.rgb(239, 83, 80) : MUTED);
            info.addView(sub);
            row.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            TextView call = Ui.iconButton(this, "☎", 46, 20, Color.TRANSPARENT, GREEN); row.addView(call);
            content.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 82)));
        }
        TextView schedule = Ui.label(this, "+  Schedule simulated call", 15, true);
        schedule.setTextColor(GREEN);
        schedule.setGravity(Gravity.CENTER);
        schedule.setPadding(0, Ui.dp(this, 20), 0, Ui.dp(this, 20));
        schedule.setOnClickListener(v -> chooseBot());
        content.addView(schedule);
    }

    private void updates() {
        TextView icon = Ui.label(this, "◉", 54, false);
        icon.setTextColor(MUTED); icon.setGravity(Gravity.CENTER); icon.setPadding(0, Ui.dp(this, 58), 0, 0); content.addView(icon);
        TextView t = Ui.label(this, "No simulated updates yet", 17, true);
        t.setTextColor(Color.WHITE); t.setGravity(Gravity.CENTER); t.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 8)); content.addView(t);
        TextView s = Ui.label(this, "This tab is only a visual simulator.", 13, false);
        s.setTextColor(MUTED); s.setGravity(Gravity.CENTER); content.addView(s);
    }

    private void settingsPage() {
        LinearLayout p = Ui.cardRow(this); p.setOrientation(LinearLayout.VERTICAL);
        TextView h = Ui.label(this, "Fictional number prefix", 15, true); h.setTextColor(Color.WHITE); p.addView(h);
        EditText e = new EditText(this); e.setText(Store.getPrefix(this)); e.setTextColor(Color.WHITE); e.setHintTextColor(MUTED); p.addView(e, new LinearLayout.LayoutParams(-1, -2));
        TextView save = Ui.label(this, "Save", 15, true); save.setTextColor(GREEN); save.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10));
        save.setOnClickListener(v -> { String q = e.getText().toString().trim(); Store.setPrefix(this, q.isEmpty() ? "+963" : q); Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show(); }); p.addView(save); content.addView(p);

        LinearLayout snd = Ui.cardRow(this); snd.setOrientation(LinearLayout.VERTICAL);
        TextView s1 = Ui.label(this, "Chat notification sound", 15, true); s1.setTextColor(Color.WHITE); s1.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10)); s1.setOnClickListener(v -> pick(false)); snd.addView(s1);
        TextView s2 = Ui.label(this, "Simulated call ringtone", 15, true); s2.setTextColor(Color.WHITE); s2.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10)); s2.setOnClickListener(v -> pick(true)); snd.addView(s2); content.addView(snd);

        LinearLayout demo = Ui.cardRow(this); demo.setOrientation(LinearLayout.VERTICAL);
        TextView dh = Ui.label(this, "Simulation data", 15, true); dh.setTextColor(Color.WHITE); demo.addView(dh);
        TextView reset = Ui.label(this, "Reset chats to 3–4 unread messages", 14, false); reset.setTextColor(GREEN); reset.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 12));
        reset.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("Reset demo data?").setPositiveButton("Reset", (d, w) -> { Store.resetDemo(this); render(); }).setNegativeButton("Cancel", null).show()); demo.addView(reset); content.addView(demo);

        TextView sys = Ui.label(this, "Android notification settings", 14, false); sys.setTextColor(MUTED); sys.setGravity(Gravity.CENTER); sys.setPadding(0, Ui.dp(this, 22), 0, Ui.dp(this, 22));
        sys.setOnClickListener(v -> { Intent in = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS); in.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()); startActivity(in); }); content.addView(sys);
    }

    private void chooseBot() {
        List<Store.Bot> bots = Store.loadBots(this); String[] n = new String[bots.size()];
        for (int i = 0; i < bots.size(); i++) n[i] = bots.get(i).name + "  " + bots.get(i).phone;
        new AlertDialog.Builder(this).setTitle("Choose fictional character").setItems(n, (d, w) -> delay(bots.get(w))).show();
    }

    private void delay(Store.Bot bot) {
        String[] n = {"10 seconds", "1 minute", "5 minutes", "15 minutes", "1 hour"};
        long[] ms = {10000, 60000, 300000, 900000, 3600000};
        new AlertDialog.Builder(this).setTitle("When should it ring?").setItems(n, (d, w) -> schedule(bot, ms[w])).show();
    }

    private void schedule(Store.Bot bot, long ms) {
        Intent in = new Intent(this, CallAlarmReceiver.class); in.putExtra("bot_id", bot.id);
        PendingIntent pi = PendingIntent.getBroadcast(this, (int) (System.currentTimeMillis() & 0x7fffffff), in, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        ((AlarmManager) getSystemService(ALARM_SERVICE)).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + ms, pi);
        Toast.makeText(this, "Simulated call scheduled", Toast.LENGTH_SHORT).show();
    }

    private void pick(boolean call) {
        Intent in = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        in.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, call ? RingtoneManager.TYPE_RINGTONE : RingtoneManager.TYPE_NOTIFICATION);
        in.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        in.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
        startActivityForResult(in, call ? CALL_SOUND : CHAT_SOUND);
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK || data == null) return;
        Uri u = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
        String x = u == null ? "" : u.toString();
        if (req == CALL_SOUND) Store.setCallSound(this, x);
        if (req == CHAT_SOUND) Store.setNotificationSound(this, x);
    }
}
