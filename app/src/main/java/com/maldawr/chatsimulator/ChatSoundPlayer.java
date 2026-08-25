package com.maldawr.chatsimulator;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;

public final class ChatSoundPlayer {
    private final Context context;
    private MediaPlayer incoming;
    private MediaPlayer outgoing;

    public ChatSoundPlayer(Context context) {
        this.context = context.getApplicationContext();
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        incoming = create(R.raw.message_incoming, attrs);
        outgoing = create(R.raw.message_outgoing, attrs);
    }

    private MediaPlayer create(int resId, AudioAttributes attrs) {
        try {
            MediaPlayer player = MediaPlayer.create(context, resId, attrs, 0);
            if (player != null) player.setOnCompletionListener(mp -> { try { mp.seekTo(0); } catch (Exception ignored) {} });
            return player;
        } catch (Exception ignored) {
            return null;
        }
    }

    public void playIncoming() { play(incoming); }
    public void playOutgoing() { play(outgoing); }

    private void play(MediaPlayer player) {
        if (player != null) {
            try {
                if (player.isPlaying()) player.seekTo(0);
                player.start();
                return;
            } catch (Exception ignored) {}
        }
        try {
            Ringtone fallback = RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
            if (fallback != null) {
                fallback.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build());
                fallback.play();
            }
        } catch (Exception ignored) {}
    }

    public void release() {
        try { if (incoming != null) incoming.release(); } catch (Exception ignored) {}
        try { if (outgoing != null) outgoing.release(); } catch (Exception ignored) {}
        incoming = null;
        outgoing = null;
    }
}
