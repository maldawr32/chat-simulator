package com.maldawr.chatsimulator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MessageAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Store.ensureSeeded(context);
        long botId = intent.getLongExtra("bot_id", -1L);
        Store.Bot bot = Store.getBot(context, botId);
        if (bot == null) return;
        long now = System.currentTimeMillis();
        String reply = Store.smartReply("scheduled message");
        Store.addMessage(context, new Store.Message(now, botId, reply, true, now));
        bot.lastMessage = reply;
        bot.lastTime = now;
        bot.unread = Math.min(99999, bot.unread + 1);
        Store.saveBot(context, bot);
        NotificationHelper.showMessage(context, bot, reply);
    }
}
