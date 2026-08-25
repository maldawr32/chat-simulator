package com.maldawr.chatsimulator;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.SoundPool;

public final class ChatSoundPlayer {
    private final Context context;
    private SoundPool soundPool;
    private int incomingId;
    private int outgoingId;
    private volatile boolean incomingReady;
    private volatile boolean outgoingReady;

    public ChatSoundPlayer(Context context) {
        this.context = context.getApplicationContext();
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(3)
                .setAudioAttributes(attrs)
                .build();
        soundPool.setOnLoadCompleteListener((pool, sampleId, status) -> {
            if (status != 0) return;
            if (sampleId == incomingId) incomingReady = true;
            if (sampleId == outgoingId) outgoingReady = true;
        });
        incomingId = soundPool.load(this.context, R.raw.message_incoming, 1);
        outgoingId = soundPool.load(this.context, R.raw.message_outgoing, 1);
    }

    public void playIncoming() {
        play(incomingId, incomingReady);
    }

    public void playOutgoing() {
        play(outgoingId, outgoingReady);
    }

    private void play(int sampleId, boolean ready) {
        SoundPool pool = soundPool;
        if (pool != null && ready && sampleId != 0) {
            try {
                pool.stop(sampleId);
                pool.play(sampleId, 1f, 1f, 1, 0, 1f);
                return;
            } catch (Exception ignored) {}
        }
        playFallback();
    }

    private void playFallback() {
        try {
            Ringtone fallback = RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
            if (fallback != null) {
                fallback.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
                fallback.play();
            }
        } catch (Exception ignored) {}
    }

    public void release() {
        try {
            if (soundPool != null) soundPool.release();
        } catch (Exception ignored) {}
        soundPool = null;
        incomingReady = false;
        outgoingReady = false;
    }
}
