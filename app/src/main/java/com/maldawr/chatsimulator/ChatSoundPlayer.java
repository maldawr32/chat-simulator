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
    private int incomingStreamId;
    private int outgoingStreamId;
    private volatile boolean incomingReady;
    private volatile boolean outgoingReady;
    private volatile boolean pendingIncoming;

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
            if (sampleId == incomingId) {
                incomingReady = true;
                if (pendingIncoming) {
                    pendingIncoming = false;
                    incomingStreamId = playLoaded(sampleId, incomingStreamId);
                }
            }
            if (sampleId == outgoingId) outgoingReady = true;
        });
        incomingId = soundPool.load(this.context, R.raw.message_incoming_v3, 1);
        outgoingId = soundPool.load(this.context, R.raw.message_outgoing, 1);
    }

    public void playIncoming() {
        if (!incomingReady || incomingId == 0) {
            pendingIncoming = true;
            return;
        }
        pendingIncoming = false;
        incomingStreamId = playLoaded(incomingId, incomingStreamId);
    }

    public void playOutgoing() {
        if (!outgoingReady || outgoingId == 0) {
            playFallback();
            return;
        }
        outgoingStreamId = playLoaded(outgoingId, outgoingStreamId);
    }

    private int playLoaded(int sampleId, int previousStreamId) {
        SoundPool pool = soundPool;
        if (pool == null || sampleId == 0) return 0;
        try {
            if (previousStreamId != 0) pool.stop(previousStreamId);
            return pool.play(sampleId, 1f, 1f, 1, 0, 1f);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private void playFallback() {
        try {
            Ringtone fallback = RingtoneManager.getRingtone(
                    context,
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            );
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
        incomingStreamId = 0;
        outgoingStreamId = 0;
        incomingReady = false;
        outgoingReady = false;
        pendingIncoming = false;
    }
}
