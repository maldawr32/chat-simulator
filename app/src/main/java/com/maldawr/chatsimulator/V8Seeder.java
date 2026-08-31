package com.maldawr.chatsimulator;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

public final class V8Seeder {
    private static final String PREFS = "chat_simulator_v8_seed";
    private static final String TEAM_DONE = "kazmoz_team_done";
    private static final long TEAM_ID = 8801L;

    private V8Seeder() {}

    public static synchronized void ensure(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (prefs.getBoolean(TEAM_DONE, false)) return;
        for (Store.Bot existing : Store.loadBots(context)) {
            if (existing.id == TEAM_ID || existing.name.toLowerCase().contains("kazmoz team service")) {
                prefs.edit().putBoolean(TEAM_DONE, true).apply();
                return;
            }
        }

        Store.Bot team = new Store.Bot(TEAM_ID, "Kazmoz Team Service • SIM", "+963 000 880 001");
        team.status = "فريق خيالي داخل Chat Simulator • ليس مجموعة حقيقية";
        team.groupChat = true;
        team.groupSubtitle = "Maimouna, Abdullah, Farouk, You";
        team.lastSender = "Maimouna";
        team.personality = "work";
        team.replyMode = "natural";
        team.favorite = true;
        team.autoReply = true;
        team.initiative = true;
        team.maxBurst = 4;
        team.emojiRate = 48;
        team.humorRate = 38;
        team.replyChance = 92;
        team.unread = 0;
        Store.saveBot(context, team);

        List<Store.GroupMember> members = new ArrayList<>();
        members.add(new Store.GroupMember(TEAM_ID * 100 + 1, TEAM_ID, "Maimouna", "organized", "📌", 88, 28, 0xFFE6A55E));
        members.add(new Store.GroupMember(TEAM_ID * 100 + 2, TEAM_ID, "Abdullah", "calm", "🙂", 76, 24, 0xFF58B7DD));
        members.add(new Store.GroupMember(TEAM_ID * 100 + 3, TEAM_ID, "Farouk", "funny", "😄", 72, 70, 0xFF62C97A));
        Store.replaceGroupMembers(context, TEAM_ID, members);

        long now = System.currentTimeMillis();
        Store.addMessage(context, new Store.Message(Store.nextMessageId(), TEAM_ID, "أهلاً 👋 هاي مجموعة الفريق الخيالية للتجربة.", true, now - 180000L, "Maimouna", "", 0L, "text"));
        Store.addMessage(context, new Store.Message(Store.nextMessageId(), TEAM_ID, "تمام، أي تحديث بالشغل منحطه هون.", true, now - 120000L, "Abdullah", "", 0L, "text"));
        Store.addMessage(context, new Store.Message(Store.nextMessageId(), TEAM_ID, "وأنا بتابع الأشياء العالقة 😄", true, now - 60000L, "Farouk", "", 0L, "text"));
        team = Store.getBot(context, TEAM_ID);
        if (team != null) {
            team.unread = 0;
            Store.saveBot(context, team);
        }
        prefs.edit().putBoolean(TEAM_DONE, true).apply();
    }
}
