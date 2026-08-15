package com.maldawr.chatsimulator;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsAnimation;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
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
    private FrameLayout sendWrap;
    private IconView sendIcon;
    private TextView status;
    private int lastMessageCount = -1;

    private SoundPool chatSoundPool;
    private int incomingSoundId;
    private int outgoingSoundId;
    private boolean incomingSoundReady;
    private boolean outgoingSoundReady;

    private final Runnable refreshTask = new Runnable() {
        @Override public void run() {
            if (!isFinishing() && botId != -1L) {
                List<Store.Message> cur = Store.loadMessages(ChatActivity.this, botId);
                int count = cur.size();
                if (count != lastMessageCount) {
                    if (lastMessageCount >= 0 && count > lastMessageCount && !cur.isEmpty() && cur.get(cur.size() - 1).incoming) {
                        playChatSound(true);
                    }
                    bot = Store.getBot(ChatActivity.this, botId);
                    renderMessages();
                }
                handler.postDelayed(this, 900L);
            }
        }
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);

        setVolumeControlStream(AudioManager.STREAM_NOTIFICATION);
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        } else {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        getWindow().setStatusBarColor(0xFF0B141A);
        getWindow().setNavigationBarColor(0xFF0B141A);

        initChatSounds();
        Store.ensureSeeded(this);
        NotificationHelper.ensureChannels(this);
        botId = getIntent().getLongExtra("bot_id", -1L);
        bot = Store.getBot(this, botId);
        if (bot == null) {
            finish();
            return;
        }
        Store.markRead(this, botId);
        buildUi();
        renderMessages();
    }

    @Override protected void onResume() {
        super.onResume();
        visibleBotId = botId;
        Store.markRead(this, botId);
        handler.removeCallbacks(refreshTask);
        handler.post(refreshTask);
    }

    @Override protected void onPause() {
        if (visibleBotId == botId) visibleBotId = -1L;
        handler.removeCallbacks(refreshTask);
        super.onPause();
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(refreshTask);
        if (chatSoundPool != null) {
            try { chatSoundPool.release(); } catch (Exception ignored) {}
            chatSoundPool = null;
        }
        super.onDestroy();
    }

    private void initChatSounds() {
        try {
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            chatSoundPool = new SoundPool.Builder()
                    .setMaxStreams(2)
                    .setAudioAttributes(attrs)
                    .build();
            incomingSoundId = chatSoundPool.load(this, R.raw.message_incoming, 1);
            outgoingSoundId = chatSoundPool.load(this, R.raw.message_outgoing, 1);
            chatSoundPool.setOnLoadCompleteListener((pool, sampleId, statusCode) -> {
                if (statusCode != 0) return;
                if (sampleId == incomingSoundId) incomingSoundReady = true;
                if (sampleId == outgoingSoundId) outgoingSoundReady = true;
            });
        } catch (Exception ignored) {
            chatSoundPool = null;
        }
    }

    private void playChatSound(boolean incoming) {
        if (chatSoundPool == null) return;
        try {
            int id = incoming ? incomingSoundId : outgoingSoundId;
            boolean ready = incoming ? incomingSoundReady : outgoingSoundReady;
            if (ready && id != 0) chatSoundPool.play(id, 1f, 1f, 1, 0, 1f);
        } catch (Exception ignored) {}
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0B141A);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setBackgroundColor(0xFF0B141A);
        int side = Ui.dp(this, 5);
        int base = Ui.dp(this, 5);
        int bottom = Ui.dp(this, 5);
        top.setPadding(side, base, side, bottom);
        top.setOnApplyWindowInsetsListener((v, insets) -> {
            int statusTop = 0;
            if (Build.VERSION.SDK_INT >= 30) {
                statusTop = insets.getInsets(WindowInsets.Type.statusBars()).top;
            }
            v.setPadding(side, base + statusTop, side, bottom);
            return insets;
        });

        IconView back = new IconView(this, IconView.BACK, 42);
        back.setOnClickListener(v -> finish());
        top.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 42), Ui.dp(this, 42)));
        top.addView(Ui.avatar(this, bot, 43));

        LinearLayout meta = new LinearLayout(this);
        meta.setOrientation(LinearLayout.VERTICAL);
        meta.setPadding(Ui.dp(this, 10), 0, Ui.dp(this, 4), 0);
        TextView name = Ui.oneLine(this, bot.name, 17, Color.WHITE);
        name.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        meta.addView(name, new LinearLayout.LayoutParams(-1, Ui.dp(this, 24)));
        String sub = bot.groupChat && !bot.groupSubtitle.isEmpty() ? bot.groupSubtitle : replyModeLabel(bot);
        status = Ui.oneLine(this, sub, 12, 0xFFBEC7CB);
        meta.addView(status, new LinearLayout.LayoutParams(-1, Ui.dp(this, 19)));
        top.addView(meta, new LinearLayout.LayoutParams(0, -2, 1));

        IconView video = new IconView(this, IconView.VIDEO, 42);
        top.addView(video, new LinearLayout.LayoutParams(Ui.dp(this, 42), Ui.dp(this, 42)));
        IconView phone = new IconView(this, IconView.PHONE, 42);
        top.addView(phone, new LinearLayout.LayoutParams(Ui.dp(this, 42), Ui.dp(this, 42)));
        IconView more = new IconView(this, IconView.MORE, 38);
        more.setOnClickListener(v -> showConversationMenu());
        top.addView(more, new LinearLayout.LayoutParams(Ui.dp(this, 38), Ui.dp(this, 38)));
        root.addView(top);

        TextView sim = Ui.safetyBanner(this);
        sim.setBackgroundColor(0xFF0B141A);
        root.addView(sim);

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackground(new ChatPatternDrawable(this));
        messages = new LinearLayout(this);
        messages.setOrientation(LinearLayout.VERTICAL);
        messages.setPadding(Ui.dp(this, 7), Ui.dp(this, 8), Ui.dp(this, 7), Ui.dp(this, 10));
        scroll.addView(messages, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout composer = new LinearLayout(this);
        composer.setGravity(Gravity.CENTER_VERTICAL);
        composer.setBackgroundColor(0xFF0B141A);
        final int composerHorizontal = Ui.dp(this, 6);
        final int composerVertical = Ui.dp(this, 5);
        composer.setPadding(composerHorizontal, composerVertical, composerHorizontal, composerVertical);

        LinearLayout field = new LinearLayout(this);
        field.setGravity(Gravity.CENTER_VERTICAL);
        field.setBackground(Ui.rounded(0xFF1F2C33, 26, this));
        field.setPadding(Ui.dp(this, 7), 0, Ui.dp(this, 7), 0);

        TextView emoji = Ui.label(this, "☺", 22, false);
        emoji.setGravity(Gravity.CENTER);
        emoji.setTextColor(0xFF8696A0);
        field.addView(emoji, new LinearLayout.LayoutParams(Ui.dp(this, 38), Ui.dp(this, 42)));

        input = new EditText(this);
        input.setSingleLine(false);
        input.setMaxLines(5);
        input.setMinLines(1);
        input.setHint("Message");
        input.setTextSize(17);
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(0xFF8696A0);
        input.setBackgroundColor(Color.TRANSPARENT);
        input.setPadding(Ui.dp(this, 4), Ui.dp(this, 7), Ui.dp(this, 4), Ui.dp(this, 7));
        input.setImeOptions(EditorInfo.IME_ACTION_SEND);
        field.addView(input, new LinearLayout.LayoutParams(0, -2, 1));

        IconView clip = new IconView(this, IconView.PLUS, 34).tint(0xFF8696A0);
        field.addView(clip, new LinearLayout.LayoutParams(Ui.dp(this, 36), Ui.dp(this, 40)));
        IconView cam = new IconView(this, IconView.CAMERA, 34).tint(0xFF8696A0);
        field.addView(cam, new LinearLayout.LayoutParams(Ui.dp(this, 38), Ui.dp(this, 40)));
        composer.addView(field, new LinearLayout.LayoutParams(0, -2, 1));

        sendWrap = new FrameLayout(this);
        sendWrap.setBackground(Ui.circle(Ui.brandBright()));
        sendIcon = new IconView(this, IconView.MIC, 30);
        sendWrap.addView(sendIcon, new FrameLayout.LayoutParams(Ui.dp(this, 30), Ui.dp(this, 30), Gravity.CENTER));
        LinearLayout.LayoutParams sw = new LinearLayout.LayoutParams(Ui.dp(this, 52), Ui.dp(this, 52));
        sw.setMargins(Ui.dp(this, 6), 0, 0, 0);
        composer.addView(sendWrap, sw);
        sendWrap.setOnClickListener(v -> {
            if (input.getText().toString().trim().isEmpty()) {
                Toast.makeText(this, "اكتب رسالة أولاً", Toast.LENGTH_SHORT).show();
            } else {
                sendMessage();
            }
        });
        root.addView(composer);

        if (Build.VERSION.SDK_INT >= 30) {
            installImeSync(composer, composerHorizontal, composerVertical);
        }

        input.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                int type = s.toString().trim().isEmpty() ? IconView.MIC : IconView.SEND;
                sendWrap.removeAllViews();
                sendIcon = new IconView(ChatActivity.this, type, 30);
                sendWrap.addView(sendIcon, new FrameLayout.LayoutParams(Ui.dp(ChatActivity.this, 30), Ui.dp(ChatActivity.this, 30), Gravity.CENTER));
            }
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

    private void installImeSync(LinearLayout composer, int horizontal, int vertical) {
        composer.setOnApplyWindowInsetsListener((v, insets) -> {
            applyComposerInsets(composer, insets, horizontal, vertical);
            return insets;
        });

        composer.setWindowInsetsAnimationCallback(new WindowInsetsAnimation.Callback(
                WindowInsetsAnimation.Callback.DISPATCH_MODE_CONTINUE_ON_SUBTREE) {
            @Override
            public WindowInsets onProgress(WindowInsets insets, List<WindowInsetsAnimation> runningAnimations) {
                applyComposerInsets(composer, insets, horizontal, vertical);
                return insets;
            }
        });
    }

    private void applyComposerInsets(LinearLayout composer, WindowInsets insets, int horizontal, int vertical) {
        if (Build.VERSION.SDK_INT < 30) return;
        int imeBottom = insets.getInsets(WindowInsets.Type.ime()).bottom;
        int navBottom = insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
        int bottomInset = Math.max(imeBottom, navBottom);
        composer.setPadding(horizontal, vertical, horizontal, vertical + bottomInset);
        if (imeBottom > 0 && scroll != null) {
            scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
        }
    }

    private String replyModeLabel(Store.Bot b) {
        String m = b.replyMode == null ? "natural" : b.replyMode;
        if ("instant".equals(m)) return "رد فوري • محاكاة";
        if ("slow".equals(m)) return "رد بطيء • محاكاة";
        return "متصل • رد طبيعي • محاكاة";
    }

    private void renderMessages() {
        if (messages == null) return;
        messages.removeAllViews();
        List<Store.Message> list = Store.loadMessages(this, botId);
        lastMessageCount = list.size();
        long lastDay = -1;
        for (Store.Message m : list) {
            long day = m.time / 86400000L;
            if (day != lastDay) {
                TextView d = Ui.label(this, day == (System.currentTimeMillis() / 86400000L) ? "Today" : "Earlier", 11, false);
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
        row.setGravity(m.incoming ? Gravity.START : Gravity.END);
        row.setPadding(Ui.dp(this, 5), Ui.dp(this, 2), Ui.dp(this, 5), Ui.dp(this, 2));

        LinearLayout bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setPadding(Ui.dp(this, 9), Ui.dp(this, 6), Ui.dp(this, 9), Ui.dp(this, 5));
        bubble.setBackground(Ui.rounded(m.incoming ? 0xFF202C33 : 0xFF005C4B, 9, this));

        if (bot.groupChat && m.incoming) {
            TextView sender = Ui.label(this, bot.name, 11, true);
            sender.setTextColor(0xFFFFB36B);
            bubble.addView(sender);
        }

        TextView text = Ui.label(this, m.text, 16, false);
        text.setTextColor(0xFFE9EDEF);
        text.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * .78f));
        bubble.addView(text);

        LinearLayout meta = new LinearLayout(this);
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

    private void sendMessage() {
        String text = input.getText().toString().trim();
        if (text.isEmpty()) return;
        long now = System.currentTimeMillis();
        Store.addMessage(this, new Store.Message(now, botId, text, false, now));
        input.setText("");
        playChatSound(false);
        renderMessages();

        Store.ReplyPlan plan = Store.buildReplyPlan(bot, text);
        if (!plan.isEmpty()) {
            long first = plan.delaysMs.get(0);
            if (first <= 90000 && !bot.groupChat) {
                status.setText("typing… • Simulation");
                handler.postDelayed(() -> {
                    Store.Bot latest = Store.getBot(this, botId);
                    if (latest != null) status.setText(replyModeLabel(latest));
                }, Math.min(first, 90000));
            }
            ReplyScheduler.schedulePlan(this, botId, plan);
        }
    }

    private void showConversationMenu() {
        String[] items = {bot.favorite ? "Remove favorite" : "Add favorite", "Mark as read", "Schedule simulated message", "Edit conversation"};
        new AlertDialog.Builder(this).setTitle(bot.name).setItems(items, (d, which) -> {
            if (which == 0) {
                bot.favorite = !bot.favorite;
                Store.saveBot(this, bot);
            } else if (which == 1) {
                Store.markRead(this, botId);
            } else if (which == 2) {
                chooseMessageDelay();
            } else {
                android.content.Intent in = new android.content.Intent(this, BotEditorActivity.class);
                in.putExtra("bot_id", botId);
                startActivity(in);
            }
        }).show();
    }

    private void chooseMessageDelay() {
        String[] labels = {"After 10 seconds", "After 1 minute", "After 5 minutes", "After 15 minutes"};
        long[] delays = {10000, 60000, 300000, 900000};
        new AlertDialog.Builder(this).setTitle("Schedule incoming message").setItems(labels, (x, which) -> {
            ReplyScheduler.scheduleOne(this, botId, Store.smartReply("scheduled"), System.currentTimeMillis() + delays[which], 700 + which);
            Toast.makeText(this, "Message scheduled", Toast.LENGTH_SHORT).show();
        }).show();
    }
}
