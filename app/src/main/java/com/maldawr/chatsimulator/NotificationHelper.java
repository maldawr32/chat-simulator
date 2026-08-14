package com.maldawr.chatsimulator;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;

public final class NotificationHelper {
    private NotificationHelper() {}

    private static String messageChannel(Context c) {
        String configured = Store.getNotificationSound(c);
        Uri sound = configured.isEmpty()
                ? Uri.parse("android.resource://" + c.getPackageName() + "/" + R.raw.message_incoming)
                : Uri.parse(configured);
        String id = "sim_messages_v2_" + Integer.toHexString((configured + "m").hashCode());
        NotificationChannel channel = new NotificationChannel(id, "Simulated messages", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Fictional local chat simulator notifications");
        channel.setSound(sound, new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build());
        ((NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        return id;
    }

    private static String callChannel(Context c) {
        String configured = Store.getCallSound(c);
        Uri sound = configured.isEmpty() ? RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE) : Uri.parse(configured);
        String id = "sim_calls_" + Integer.toHexString((configured + "c").hashCode());
        NotificationChannel channel = new NotificationChannel(id, "Simulated calls", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Fictional local incoming call simulator");
        channel.setSound(sound, new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE).build());
        channel.enableVibration(true);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        ((NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        return id;
    }

    public static void showMessage(Context c, Store.Bot bot, String text) {
        Intent open = new Intent(c, ChatActivity.class);
        open.putExtra("bot_id", bot.id);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, (int) bot.id, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification n = new Notification.Builder(c, messageChannel(c))
                .setSmallIcon(R.drawable.ic_notification_custom)
                .setContentTitle("SIMULATION - " + bot.name)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text + "\nFictional local chat data"))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .build();
        ((NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE)).notify((int) (System.currentTimeMillis() & 0x7fffffff), n);
    }

    public static void showCall(Context c, Store.Bot bot) {
        Intent open = new Intent(c, IncomingCallActivity.class);
        open.putExtra("bot_id", bot.id);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, (int) (bot.id + 4000), open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification n = new Notification.Builder(c, callChannel(c))
                .setSmallIcon(R.drawable.ic_notification_custom)
                .setContentTitle("SIMULATED INCOMING CALL")
                .setContentText(bot.name + " - " + bot.phone)
                .setContentIntent(pi)
                .setFullScreenIntent(pi, true)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_CALL)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .build();
        ((NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE)).notify((int) (bot.id + 8000), n);
    }

    public static void cancelCall(Context c, Store.Bot bot) {
        ((NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE)).cancel((int) (bot.id + 8000));
    }
}
