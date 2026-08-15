package com.maldawr.chatsimulator;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Person;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;

import java.util.List;

public final class NotificationHelper {
    public static final String MESSAGE_CHANNEL_ID = "sim_messages_v5";
    public static final String CALL_CHANNEL_ID = "sim_calls_v2";

    private NotificationHelper() {}

    private static int messageNotificationId(long botId) {
        return (int) (10000 + (botId % 100000));
    }

    public static void ensureChannels(Context c) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel messages = new NotificationChannel(
                MESSAGE_CHANNEL_ID,
                "Simulated chat messages",
                NotificationManager.IMPORTANCE_HIGH
        );
        messages.setDescription("Notifications for fictional local simulator conversations");
        messages.setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build()
        );
        messages.enableVibration(true);
        messages.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        nm.createNotificationChannel(messages);

        String configuredCall = Store.getCallSound(c);
        Uri callSound = configuredCall.isEmpty()
                ? RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                : Uri.parse(configuredCall);
        NotificationChannel calls = new NotificationChannel(
                CALL_CHANNEL_ID,
                "Simulated calls",
                NotificationManager.IMPORTANCE_HIGH
        );
        calls.setDescription("Fictional local incoming call simulator");
        calls.setSound(
                callSound,
                new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE).build()
        );
        calls.enableVibration(true);
        calls.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(calls);
    }

    public static void showMessage(Context c, Store.Bot bot, String newestText) {
        ensureChannels(c);

        Intent open = new Intent(c, ChatActivity.class);
        open.putExtra("bot_id", bot.id);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPi = PendingIntent.getActivity(
                c,
                (int) bot.id,
                open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent read = new Intent(c, MarkReadReceiver.class);
        read.putExtra("bot_id", bot.id);
        PendingIntent readPi = PendingIntent.getBroadcast(
                c,
                (int) (bot.id + 200000),
                read,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Person person = new Person.Builder()
                .setName(bot.name + " • Simulation")
                .setKey("sim-bot-" + bot.id)
                .build();
        Notification.MessagingStyle style = new Notification.MessagingStyle(person)
                .setConversationTitle(bot.name + " • Simulation");

        List<Store.Message> recent = Store.loadMessages(c, bot.id);
        int start = Math.max(0, recent.size() - 8);
        for (int i = start; i < recent.size(); i++) {
            Store.Message m = recent.get(i);
            style.addMessage(m.text, m.time, m.incoming ? person : null);
        }

        Notification.Action markRead = new Notification.Action.Builder(
                Icon.createWithResource(c, R.drawable.ic_notification),
                "Mark as read",
                readPi
        ).setSemanticAction(Notification.Action.SEMANTIC_ACTION_MARK_AS_READ).build();

        Notification.Builder builder = new Notification.Builder(c, MESSAGE_CHANNEL_ID)
                .setSmallIcon(CustomizationHelper.notificationSmallIcon(c))
                .setContentTitle(bot.name + " • Simulation")
                .setContentText(newestText)
                .setStyle(style)
                .setContentIntent(openPi)
                .addAction(markRead)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setGroup("sim_chat_" + bot.id)
                .setOnlyAlertOnce(false)
                .setNumber(bot.unread);

        Bitmap large = CustomizationHelper.notificationLargeBitmap(c);
        if (large != null) builder.setLargeIcon(large);

        ((NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(messageNotificationId(bot.id), builder.build());
    }

    public static void cancelMessage(Context c, long botId) {
        ((NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE))
                .cancel(messageNotificationId(botId));
    }

    public static void showCall(Context c, Store.Bot bot) {
        ensureChannels(c);
        Intent open = new Intent(c, IncomingCallActivity.class);
        open.putExtra("bot_id", bot.id);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(
                c,
                (int) (bot.id + 4000),
                open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder = new Notification.Builder(c, CALL_CHANNEL_ID)
                .setSmallIcon(CustomizationHelper.notificationSmallIcon(c))
                .setContentTitle("SIMULATED INCOMING CALL")
                .setContentText(bot.name + " - " + bot.phone)
                .setContentIntent(pi)
                .setFullScreenIntent(pi, true)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_CALL)
                .setVisibility(Notification.VISIBILITY_PUBLIC);

        Bitmap large = CustomizationHelper.notificationLargeBitmap(c);
        if (large != null) builder.setLargeIcon(large);

        ((NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE))
                .notify((int) (bot.id + 8000), builder.build());
    }

    public static void cancelCall(Context c, Store.Bot bot) {
        ((NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE))
                .cancel((int) (bot.id + 8000));
    }
}
