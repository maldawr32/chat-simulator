package com.maldawr.chatsimulator;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Random;

public class ChatActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private long botId;
    private Store.Bot bot;
    private LinearLayout messages;
    private ScrollView scroll;
    private EditText input;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Store.ensureSeeded(this);
        botId = getIntent().getLongExtra("bot_id", -1L);
        bot = Store.getBot(this, botId);
        if (bot == null) {
            finish();
            return;
        }
        bot.unread = 0;
        Store.saveBot(this, bot);
        buildUi();
        renderMessages();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.bg(this));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10));
        top.setBackgroundColor(Ui.teal());
        top.addView(Ui.avatar(this, bot, 46));

        LinearLayout meta = new LinearLayout(this);
        meta.setOrientation(LinearLayout.VERTICAL);
        meta.setPadding(Ui.dp(this, 10), 0, Ui.dp(this, 8), 0);
        TextView name = Ui.label(this, bot.name, 17, true);
        name.setTextColor(0xFFFFFFFF);
        TextView status = Ui.label(this, bot.phone + " - " + bot.status, 11, false);
        status.setTextColor(0xFFD9F0F1);
        meta.addView(name);
        meta.addView(status);
        top.addView(meta, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button timer = Ui.button(this, "Timer");
        timer.setOnClickListener(v -> chooseMessageDelay());
        top.addView(timer);
        root.addView(top);
        root.addView(Ui.safetyBanner(this));

        scroll = new ScrollView(this);
        messages = new LinearLayout(this);
        messages.setOrientation(LinearLayout.VERTICAL);
        messages.setPadding(Ui.dp(this, 8), Ui.dp(this, 10), Ui.dp(this, 8), Ui.dp(this, 16));
        scroll.addView(messages, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout composer = new LinearLayout(this);
        composer.setOrientation(LinearLayout.HORIZONTAL);
        composer.setGravity(Gravity.CENTER_VERTICAL);
        composer.setPadding(Ui.dp(this, 8), Ui.dp(this, 6), Ui.dp(this, 8), Ui.dp(this, 8));
        composer.setBackgroundColor(Ui.card(this));
        input = new EditText(this);
        input.setSingleLine(false);
        input.setMaxLines(4);
        input.setHint("Message the fictional bot...");
        input.setTextColor(Ui.text(this));
        input.setHintTextColor(Ui.sub(this));
        input.setImeOptions(EditorInfo.IME_ACTION_SEND);
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
        Button send = Ui.button(this, "Send");
        send.setOnClickListener(v -> sendMessage());
        composer.addView(input, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        composer.addView(send);
        root.addView(composer);

        setContentView(root);
    }

    private void renderMessages() {
        messages.removeAllViews();
        List<Store.Message> list = Store.loadMessages(this, botId);
        for (Store.Message m : list) addBubble(m);
        messages.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    private void addBubble(Store.Message m) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        row.setGravity(m.incoming ? Gravity.START : Gravity.END);
        row.setPadding(Ui.dp(this, 6), Ui.dp(this, 3), Ui.dp(this, 6), Ui.dp(this, 3));

        LinearLayout bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setPadding(Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 7));
        int color = m.incoming ? Ui.card(this) : (Ui.isDark(this) ? 0xFF23555B : 0xFFD8EEF0);
        bubble.setBackground(Ui.rounded(color, 15, this));

        TextView text = Ui.label(this, m.text, 15, false);
        text.setGravity(Gravity.START);
        TextView time = Ui.label(this, Store.formatTime(m.time), 10, false);
        time.setTextColor(Ui.sub(this));
        time.setGravity(Gravity.END);
        bubble.addView(text);
        bubble.addView(time);

        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bp.width = Math.min(getResources().getDisplayMetrics().widthPixels * 4 / 5, Ui.dp(this, 360));
        row.addView(bubble, bp);
        messages.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
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
        renderMessages();

        if (!bot.autoReply) return;
        if (!Store.isBotActive(bot)) {
            Toast.makeText(this, "Bot is outside its configured active hours", Toast.LENGTH_SHORT).show();
            return;
        }

        long delay = 1200L + random.nextInt(2800);
        handler.postDelayed(() -> {
            Store.Bot latest = Store.getBot(this, botId);
            if (latest == null) return;
            String reply = Store.smartReply(text);
            long t = System.currentTimeMillis();
            Store.addMessage(this, new Store.Message(t, botId, reply, true, t));
            latest.lastMessage = reply;
            latest.lastTime = t;
            latest.unread = 0;
            Store.saveBot(this, latest);
            bot = latest;
            renderMessages();
        }, delay);
    }

    private void chooseMessageDelay() {
        String[] labels = {"10 seconds (test)", "1 minute", "5 minutes", "15 minutes"};
        long[] delays = {10_000L, 60_000L, 5 * 60_000L, 15 * 60_000L};
        new AlertDialog.Builder(this)
                .setTitle("Schedule an incoming simulated message")
                .setItems(labels, (d, which) -> scheduleMessage(delays[which]))
                .show();
    }

    private void scheduleMessage(long delayMs) {
        Intent intent = new Intent(this, MessageAlarmReceiver.class);
        intent.putExtra("bot_id", botId);
        int requestCode = (int) (System.currentTimeMillis() & 0x7fffffff);
        PendingIntent pi = PendingIntent.getBroadcast(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delayMs, pi);
        Toast.makeText(this, "Simulated message scheduled", Toast.LENGTH_SHORT).show();
    }
}
