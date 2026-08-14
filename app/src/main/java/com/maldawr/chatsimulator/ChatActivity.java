package com.maldawr.chatsimulator;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class ChatActivity extends Activity {
    private static volatile long visibleBotId = -1L;

    public static boolean isConversationVisible(long botId) {
        return visibleBotId == botId;
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private long botId;
    private Store.Bot bot;
    private LinearLayout messages;
    private ScrollView scroll;
    private EditText input;
    private TextView send;
    private TextView status;
    private int lastMessageCount = -1;

    private final Runnable refreshTask = new Runnable() {
        @Override public void run() {
            if (!isFinishing() && botId != -1L) {
                List<Store.Message> current = Store.loadMessages(ChatActivity.this, botId);
                int count = current.size();
                if (count != lastMessageCount) {
                    if (lastMessageCount >= 0 && count > lastMessageCount && !current.isEmpty()) {
                        Store.Message newest = current.get(current.size() - 1);
                        if (newest.incoming) playChatSound(R.raw.message_incoming);
                    }
                    bot = Store.getBot(ChatActivity.this, botId);
                    renderMessages();
                }
                handler.postDelayed(this, 900L);
            }
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(true);
        getWindow().setStatusBarColor(Color.rgb(11, 20, 26));
        getWindow().setNavigationBarColor(Color.rgb(11, 20, 26));

        Store.ensureSeeded(this);
        NotificationHelper.ensureChannels(this);
        botId = getIntent().getLongExtra("bot_id", -1L);
        bot = Store.getBot(this, botId);
        if (bot == null) { finish(); return; }
        bot.unread = 0;
        Store.saveBot(this, bot);
        buildUi();
        renderMessages();
    }

    @Override protected void onResume() {
        super.onResume();
        visibleBotId = botId;
        handler.removeCallbacks(refreshTask);
        handler.post(refreshTask);
    }

    @Override protected void onPause() {
        if (visibleBotId == botId) visibleBotId = -1L;
        handler.removeCallbacks(refreshTask);
        super.onPause();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(11, 20, 26));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setBackgroundColor(Color.rgb(11, 20, 26));
        int side = Ui.dp(this, 7), baseTop = Ui.dp(this, 5), bottom = Ui.dp(this, 6);
        top.setPadding(side, baseTop, side, bottom);
        top.setOnApplyWindowInsetsListener((v, insets) -> {
            int t;
            if (Build.VERSION.SDK_INT >= 30) t = insets.getInsets(WindowInsets.Type.statusBars()).top;
            else t = insets.getSystemWindowInsetTop();
            v.setPadding(side, baseTop + t, side, bottom);
            return insets;
        });

        TextView back = Ui.iconButton(this, "‹", 43, 35, Color.TRANSPARENT, Color.WHITE);
        back.setOnClickListener(v -> finish());
        top.addView(back);
        top.addView(Ui.avatar(this, bot, 43));

        LinearLayout meta = new LinearLayout(this);
        meta.setOrientation(LinearLayout.VERTICAL);
        meta.setPadding(Ui.dp(this, 10), 0, Ui.dp(this, 5), 0);
        TextView name = Ui.oneLine(this, bot.name, 17, Color.WHITE);
        name.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        meta.addView(name, new LinearLayout.LayoutParams(-1, Ui.dp(this, 24)));
        status = Ui.oneLine(this, replyModeLabel(bot), 12, 0xFFBEC7CB);
        meta.addView(status, new LinearLayout.LayoutParams(-1, Ui.dp(this, 19)));
        top.addView(meta, new LinearLayout.LayoutParams(0, -2, 1f));

        top.addView(Ui.iconButton(this, "▣", 43, 22, Color.TRANSPARENT, Color.WHITE));
        top.addView(Ui.iconButton(this, "☎", 43, 20, Color.TRANSPARENT, Color.WHITE));
        TextView menu = Ui.iconButton(this, "⋮", 38, 27, Color.TRANSPARENT, Color.WHITE);
        menu.setOnClickListener(v -> showConversationMenu());
        top.addView(menu);
        root.addView(top);

        TextView simulation = Ui.safetyBanner(this);
        simulation.setBackgroundColor(Color.rgb(11, 20, 26));
        root.addView(simulation);

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackground(new ChatPatternDrawable(this));
        messages = new LinearLayout(this);
        messages.setOrientation(LinearLayout.VERTICAL);
        messages.setPadding(Ui.dp(this, 7), Ui.dp(this, 8), Ui.dp(this, 7), Ui.dp(this, 10));
        messages.setBackgroundColor(Color.TRANSPARENT);
        scroll.addView(messages, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout composer = new LinearLayout(this);
        composer.setOrientation(LinearLayout.HORIZONTAL);
        composer.setGravity(Gravity.CENTER_VERTICAL);
        composer.setBackgroundColor(Color.rgb(11, 20, 26));
        int pad = Ui.dp(this, 6);
        composer.setPadding(pad, Ui.dp(this, 5), pad, Ui.dp(this, 5));

        LinearLayout field = new LinearLayout(this);
        field.setOrientation(LinearLayout.HORIZONTAL);
        field.setGravity(Gravity.CENTER_VERTICAL);
        field.setBackground(Ui.rounded(Color.rgb(31, 44, 51), 26, this));
        field.setPadding(Ui.dp(this, 5), 0, Ui.dp(this, 5), 0);
        field.addView(Ui.iconButton(this, "☺", 39, 22, Color.TRANSPARENT, 0xFF8696A0));

        input = new EditText(this);
        input.setSingleLine(false);
        input.setMaxLines(5);
        input.setMinLines(1);
        input.setHint("Message");
        input.setTextSize(17);
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(0xFF8696A0);
        input.setBackgroundColor(Color.TRANSPARENT);
        input.setPadding(Ui.dp(this, 5), Ui.dp(this, 8), Ui.dp(this, 5), Ui.dp(this, 8));
        input.setImeOptions(EditorInfo.IME_ACTION_SEND);
        field.addView(input, new LinearLayout.LayoutParams(0, -2, 1f));
        field.addView(Ui.iconButton(this, "⌕", 38, 22, Color.TRANSPARENT, 0xFF8696A0));
        field.addView(Ui.iconButton(this, "◉", 38, 20, Color.TRANSPARENT, 0xFF8696A0));
        composer.addView(field, new LinearLayout.LayoutParams(0, -2, 1f));

        send = Ui.iconButton(this, "●", 52, 21, Ui.brandBright(), Color.WHITE);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(Ui.dp(this, 52), Ui.dp(this, 52));
        sp.setMargins(Ui.dp(this, 6), 0, 0, 0);
        send.setLayoutParams(sp);
        send.setOnClickListener(v -> {
            if (input.getText().toString().trim().isEmpty()) Toast.makeText(this, "اكتب رسالة أولاً", Toast.LENGTH_SHORT).show();
            else sendMessage();
        });
        composer.addView(send);
        root.addView(composer);

        input.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) { send.setText(s.toString().trim().isEmpty() ? "●" : "➤"); }
            public void afterTextChanged(android.text.Editable e) {}
        });
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND && !input.getText().toString().trim().isEmpty()) {
                sendMessage();
                return true;
            }
            return false;
        });

        setContentView(root);
        root.requestApplyInsets();
    }

    private String replyModeLabel(Store.Bot b) {
        String mode = b.replyMode == null ? "natural" : b.replyMode;
        if ("instant".equals(mode)) return "رد فوري • محاكاة";
        if ("slow".equals(mode)) return "رد بطيء • محاكاة";
        return "متصل • رد طبيعي • محاكاة";
    }

    private void renderMessages() {
        if (messages == null) return;
        messages.removeAllViews();
        List<Store.Message> list = Store.loadMessages(this, botId);
        lastMessageCount = list.size();
        long lastDay = -1;
        for (Store.Message m : list) {
            long day = m.time / 86_400_000L;
            if (day != lastDay) {
                TextView d = Ui.label(this, day == (System.currentTimeMillis() / 86_400_000L) ? "Today" : "Earlier", 11, false);
                d.setTextColor(0xFFBEC7CB);
                d.setGravity(Gravity.CENTER);
                d.setBackground(Ui.rounded(0xFF1E2A30, 7, this));
                LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(-2, Ui.dp(this, 28));
                dp.gravity = Gravity.CENTER;
                dp.setMargins(0, Ui.dp(this, 6), 0, Ui.dp(this, 6));
                messages.addView(d, dp);
                lastDay = day;
            }
            addBubble(m);
        }
        messages.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    private void addBubble(Store.Message m) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        row.setGravity(m.incoming ? Gravity.START : Gravity.END);
        row.setPadding(Ui.dp(this, 5), Ui.dp(this, 2), Ui.dp(this, 5), Ui.dp(this, 2));

        LinearLayout bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setPadding(Ui.dp(this, 9), Ui.dp(this, 6), Ui.dp(this, 9), Ui.dp(this, 5));
        bubble.setBackground(Ui.rounded(m.incoming ? 0xFF202C33 : 0xFF005C4B, 9, this));

        TextView text = Ui.label(this, m.text, 16, false);
        text.setTextColor(0xFFE9EDEF);
        text.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * 0.78f));
        text.setGravity(Gravity.START);
        bubble.addView(text);

        LinearLayout meta = new LinearLayout(this);
        meta.setOrientation(LinearLayout.HORIZONTAL);
        meta.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        TextView time = Ui.label(this, Store.formatTime(m.time), 10, false);
        time.setTextColor(0xFF8696A0);
        meta.addView(time);
        if (!m.incoming) {
            TextView ticks = Ui.label(this, " ✓✓", 11, true);
            ticks.setTextColor(0xFF53BDEB);
            meta.addView(ticks);
        }
        bubble.addView(meta);
        row.addView(bubble, new LinearLayout.LayoutParams(-2, -2));
        messages.addView(row, new LinearLayout.LayoutParams(-1, -2));
    }

    private void playChatSound(int resId) {
        try {
            MediaPlayer mp = MediaPlayer.create(this, resId);
            if (mp == null) return;
            mp.setOnCompletionListener(MediaPlayer::release);
            mp.start();
        } catch (Exception ignored) {}
    }

    private void sendMessage() {
        String text = input.getText().toString().trim();
        if (text.isEmpty()) return;
        long now = System.currentTimeMillis();
        Store.addMessage(this, new Store.Message(now, botId, text, false, now));
        bot.lastMessage = text;
        bot.lastTime = now;
        bot.unread = 0;
        Store.saveBot(this, bot);
        input.setText("");
        playChatSound(R.raw.message_outgoing);
        renderMessages();

        Store.ReplyPlan plan = Store.buildReplyPlan(bot, text);
        if (!plan.isEmpty()) {
            long first = plan.delaysMs.get(0);
            if (first <= 90_000L) {
                status.setText("typing… • Simulation");
                handler.postDelayed(() -> {
                    Store.Bot latest = Store.getBot(this, botId);
                    if (latest != null) status.setText(replyModeLabel(latest));
                }, Math.min(first, 90_000L));
            }
            ReplyScheduler.schedulePlan(this, botId, plan);
        }
    }

    private void showConversationMenu() {
        String mode = bot.replyMode == null ? "natural" : bot.replyMode;
        String[] items = {"جدولة رسالة محاكية", "وضع الرد الحالي: " + mode, "فتح إعدادات البوت"};
        new AlertDialog.Builder(this).setTitle("Conversation simulator").setItems(items, (d, which) -> {
            if (which == 0) chooseMessageDelay();
            else if (which == 2) {
                android.content.Intent in = new android.content.Intent(this, BotEditorActivity.class);
                in.putExtra("bot_id", botId);
                startActivity(in);
            }
        }).show();
    }

    private void chooseMessageDelay() {
        String[] labels = {"رسالة بعد 10 ثوان", "بعد دقيقة", "بعد 5 دقائق", "بعد 15 دقيقة"};
        long[] delays = {10_000L, 60_000L, 300_000L, 900_000L};
        new AlertDialog.Builder(this).setTitle("جدولة رسالة واردة محاكية").setItems(labels, (d, w) -> {
            String text = Store.smartReply("scheduled message");
            ReplyScheduler.scheduleOne(this, botId, text, System.currentTimeMillis() + delays[w], 700 + w);
            Toast.makeText(this, "تمت جدولة الرسالة", Toast.LENGTH_SHORT).show();
        }).show();
    }
}
