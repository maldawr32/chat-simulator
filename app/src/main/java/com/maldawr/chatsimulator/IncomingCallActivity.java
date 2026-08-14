package com.maldawr.chatsimulator;

import android.app.Activity;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class IncomingCallActivity extends Activity {
    private Store.Bot bot;
    private Ringtone ringtone;
    private boolean answered = false;
    private long answeredAt = 0L;
    private TextView state;
    private Button answer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setShowWhenLocked(true);
        setTurnScreenOn(true);
        Store.ensureSeeded(this);
        long botId = getIntent().getLongExtra("bot_id", -1L);
        bot = Store.getBot(this, botId);
        if (bot == null) {
            finish();
            return;
        }
        buildUi();
        startRinging();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(Ui.dp(this, 22), Ui.dp(this, 24), Ui.dp(this, 22), Ui.dp(this, 28));
        root.setBackgroundColor(Ui.bg(this));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        root.addView(Ui.safetyBanner(this), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = Ui.label(this, "SIMULATED INCOMING CALL", 16, true);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParams.setMargins(0, Ui.dp(this, 30), 0, Ui.dp(this, 20));
        root.addView(title, titleParams);

        root.addView(Ui.avatar(this, bot, 124));
        TextView name = Ui.label(this, bot.name, 28, true);
        name.setGravity(Gravity.CENTER);
        root.addView(name);
        TextView phone = Ui.label(this, bot.phone, 16, false);
        phone.setTextColor(Ui.sub(this));
        phone.setGravity(Gravity.CENTER);
        root.addView(phone);

        state = Ui.label(this, "Ringing - fictional local call", 14, false);
        state.setTextColor(Ui.sub(this));
        state.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams stateParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        stateParams.setMargins(0, Ui.dp(this, 14), 0, Ui.dp(this, 28));
        root.addView(state, stateParams);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER);
        answer = Ui.button(this, "Answer simulation");
        Button decline = Ui.button(this, "Decline");
        answer.setOnClickListener(v -> {
            if (!answered) answerCall();
            else endCall();
        });
        decline.setOnClickListener(v -> endCall());
        actions.addView(answer, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        actions.addView(decline, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(actions, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(root);
    }

    private void startRinging() {
        try {
            String configured = Store.getCallSound(this);
            Uri uri = configured.isEmpty() ? RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE) : Uri.parse(configured);
            ringtone = RingtoneManager.getRingtone(this, uri);
            if (ringtone != null) ringtone.play();
        } catch (Exception ignored) {}
    }

    private void stopRinging() {
        try {
            if (ringtone != null && ringtone.isPlaying()) ringtone.stop();
        } catch (Exception ignored) {}
    }

    private void answerCall() {
        stopRinging();
        answered = true;
        answeredAt = System.currentTimeMillis();
        state.setText("Connected simulation - no real audio call is placed");
        answer.setText("Hang up");
    }

    private void endCall() {
        stopRinging();
        NotificationHelper.cancelCall(this, bot);
        if (answered) {
            long now = System.currentTimeMillis();
            int duration = (int) Math.max(1L, (now - answeredAt) / 1000L);
            Store.addCall(this, new Store.CallItem(now, bot.id, "incoming", now, duration));
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        stopRinging();
        super.onDestroy();
    }
}
