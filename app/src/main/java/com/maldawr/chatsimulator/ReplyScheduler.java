package com.maldawr.chatsimulator;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public final class ReplyScheduler {
    private ReplyScheduler() {}

    public static void schedulePlan(Context c, long botId, Store.ReplyPlan plan) {
        if (plan == null || plan.isEmpty()) return;
        long now = System.currentTimeMillis();
        for (int i = 0; i < plan.messages.size(); i++) {
            scheduleOne(c, botId, plan.messages.get(i), now + plan.delaysMs.get(i), i);
        }
    }

    public static void scheduleOne(Context c, long botId, String text, long triggerAt, int sequence) {
        Intent in = new Intent(c, ReplyAlarmReceiver.class);
        in.putExtra("bot_id", botId);
        in.putExtra("text", text);
        in.putExtra("sequence", sequence);
        int requestCode = (int) ((botId * 31 + triggerAt + sequence) & 0x7fffffff);
        PendingIntent pi = PendingIntent.getBroadcast(
                c,
                requestCode,
                in,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= 31 && am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        } else if (Build.VERSION.SDK_INT < 31) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
    }
}
