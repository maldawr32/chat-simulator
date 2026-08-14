package com.maldawr.chatsimulator;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class Store {
    private static final String PREFS = "chat_simulator_data";
    private static final String KEY_BOTS = "bots";
    private static final String KEY_MESSAGES = "messages";
    private static final String KEY_CALLS = "calls";
    private static final String KEY_PREFIX = "prefix";
    private static final String KEY_NOTIFICATION_SOUND = "notification_sound";
    private static final String KEY_CALL_SOUND = "call_sound";
    private static final String KEY_ICON = "icon";
    private static final Random RANDOM = new Random();

    private Store() {}

    public static final class Bot {
        public long id;
        public String name;
        public String phone;
        public String status;
        public int unread;
        public String lastMessage;
        public long lastTime;
        public boolean autoReply;
        public String avatarUri;
        public int activeFrom;
        public int activeTo;

        public Bot(long id, String name, String phone) {
            this.id = id;
            this.name = name;
            this.phone = phone;
            this.status = "Fictional bot";
            this.unread = 0;
            this.lastMessage = "Simulation ready";
            this.lastTime = System.currentTimeMillis();
            this.autoReply = true;
            this.avatarUri = "";
            this.activeFrom = 0;
            this.activeTo = 24;
        }

        JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("id", id);
                o.put("name", name);
                o.put("phone", phone);
                o.put("status", status);
                o.put("unread", unread);
                o.put("lastMessage", lastMessage);
                o.put("lastTime", lastTime);
                o.put("autoReply", autoReply);
                o.put("avatarUri", avatarUri);
                o.put("activeFrom", activeFrom);
                o.put("activeTo", activeTo);
            } catch (Exception ignored) {}
            return o;
        }

        static Bot fromJson(JSONObject o) {
            Bot b = new Bot(o.optLong("id"), o.optString("name"), o.optString("phone"));
            b.status = o.optString("status", "Fictional bot");
            b.unread = o.optInt("unread", 0);
            b.lastMessage = o.optString("lastMessage", "Simulation ready");
            b.lastTime = o.optLong("lastTime", System.currentTimeMillis());
            b.autoReply = o.optBoolean("autoReply", true);
            b.avatarUri = o.optString("avatarUri", "");
            b.activeFrom = o.optInt("activeFrom", 0);
            b.activeTo = o.optInt("activeTo", 24);
            return b;
        }
    }

    public static final class Message {
        public long id;
        public long botId;
        public String text;
        public boolean incoming;
        public long time;

        public Message(long id, long botId, String text, boolean incoming, long time) {
            this.id = id;
            this.botId = botId;
            this.text = text;
            this.incoming = incoming;
            this.time = time;
        }

        JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("id", id);
                o.put("botId", botId);
                o.put("text", text);
                o.put("incoming", incoming);
                o.put("time", time);
            } catch (Exception ignored) {}
            return o;
        }

        static Message fromJson(JSONObject o) {
            return new Message(
                    o.optLong("id"),
                    o.optLong("botId"),
                    o.optString("text"),
                    o.optBoolean("incoming"),
                    o.optLong("time")
            );
        }
    }

    public static final class CallItem {
        public long id;
        public long botId;
        public String type;
        public long time;
        public int durationSec;

        public CallItem(long id, long botId, String type, long time, int durationSec) {
            this.id = id;
            this.botId = botId;
            this.type = type;
            this.time = time;
            this.durationSec = durationSec;
        }

        JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("id", id);
                o.put("botId", botId);
                o.put("type", type);
                o.put("time", time);
                o.put("durationSec", durationSec);
            } catch (Exception ignored) {}
            return o;
        }

        static CallItem fromJson(JSONObject o) {
            return new CallItem(
                    o.optLong("id"),
                    o.optLong("botId"),
                    o.optString("type"),
                    o.optLong("time"),
                    o.optInt("durationSec")
            );
        }
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static synchronized void ensureSeeded(Context c) {
        if (prefs(c).contains(KEY_BOTS)) return;

        long now = System.currentTimeMillis();
        List<Bot> bots = new ArrayList<>();
        String[] names = {
                "Executive Bot 01", "Project Bot 02", "Office Bot 03",
                "Schedule Bot 04", "Support Bot 05", "Partner Bot 06",
                "Team Bot 07", "Meeting Bot 08", "Assistant Bot 09",
                "VIP Demo Bot 10", "Work Bot 11", "Travel Bot 12"
        };
        int[] unread = {428, 219, 673, 154, 999, 306, 521, 188, 744, 263, 391, 112};
        for (int i = 0; i < names.length; i++) {
            Bot b = new Bot(1001 + i, names[i], String.format(Locale.US, "+963 000 000 %03d", i + 1));
            b.status = "SIMULATION - fictional contact";
            b.unread = unread[i];
            b.lastMessage = i % 2 == 0 ? "Can we talk later?" : "I sent the demo details.";
            b.lastTime = now - (long) (i * 13 + 2) * 60_000L;
            b.activeFrom = i % 3 == 0 ? 8 : 0;
            b.activeTo = i % 3 == 0 ? 23 : 24;
            bots.add(b);
        }
        saveBots(c, bots);

        List<Message> messages = new ArrayList<>();
        for (Bot b : bots) {
            messages.add(new Message(now + b.id, b.id, "This is a fictional demo conversation.", true, now - 3_600_000L));
            messages.add(new Message(now + b.id + 1, b.id, "Okay, I will reply when I am available.", false, now - 3_300_000L));
            messages.add(new Message(now + b.id + 2, b.id, b.lastMessage, true, b.lastTime));
        }
        saveMessages(c, messages);

        List<CallItem> calls = new ArrayList<>();
        long[] ages = {25 * 60_000L, 2 * 3_600_000L, 8 * 3_600_000L, 26 * 3_600_000L, 3 * 86_400_000L, 9 * 86_400_000L, 22 * 86_400_000L};
        String[] types = {"missed", "incoming", "missed", "outgoing", "incoming", "missed", "outgoing"};
        for (int i = 0; i < ages.length; i++) {
            calls.add(new CallItem(now + i, bots.get(i).id, types[i], now - ages[i], i % 2 == 0 ? 0 : 85 + i * 20));
        }
        saveCalls(c, calls);
        prefs(c).edit().putString(KEY_PREFIX, "+963").putString(KEY_ICON, "green").apply();
    }

    public static List<Bot> loadBots(Context c) {
        List<Bot> result = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(prefs(c).getString(KEY_BOTS, "[]"));
            for (int i = 0; i < arr.length(); i++) result.add(Bot.fromJson(arr.getJSONObject(i)));
        } catch (Exception ignored) {}
        return result;
    }

    public static synchronized void saveBots(Context c, List<Bot> bots) {
        JSONArray arr = new JSONArray();
        for (Bot b : bots) arr.put(b.toJson());
        prefs(c).edit().putString(KEY_BOTS, arr.toString()).apply();
    }

    public static Bot getBot(Context c, long id) {
        for (Bot b : loadBots(c)) if (b.id == id) return b;
        return null;
    }

    public static synchronized void saveBot(Context c, Bot bot) {
        List<Bot> bots = loadBots(c);
        boolean found = false;
        for (int i = 0; i < bots.size(); i++) {
            if (bots.get(i).id == bot.id) {
                bots.set(i, bot);
                found = true;
                break;
            }
        }
        if (!found) bots.add(0, bot);
        saveBots(c, bots);
    }

    public static List<Message> loadMessages(Context c, long botId) {
        List<Message> result = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(prefs(c).getString(KEY_MESSAGES, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                Message m = Message.fromJson(arr.getJSONObject(i));
                if (m.botId == botId) result.add(m);
            }
        } catch (Exception ignored) {}
        return result;
    }

    private static List<Message> loadAllMessages(Context c) {
        List<Message> result = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(prefs(c).getString(KEY_MESSAGES, "[]"));
            for (int i = 0; i < arr.length(); i++) result.add(Message.fromJson(arr.getJSONObject(i)));
        } catch (Exception ignored) {}
        return result;
    }

    private static void saveMessages(Context c, List<Message> messages) {
        JSONArray arr = new JSONArray();
        for (Message m : messages) arr.put(m.toJson());
        prefs(c).edit().putString(KEY_MESSAGES, arr.toString()).apply();
    }

    public static synchronized void addMessage(Context c, Message m) {
        List<Message> all = loadAllMessages(c);
        all.add(m);
        if (all.size() > 5000) all = new ArrayList<>(all.subList(all.size() - 5000, all.size()));
        saveMessages(c, all);
    }

    public static List<CallItem> loadCalls(Context c) {
        List<CallItem> result = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(prefs(c).getString(KEY_CALLS, "[]"));
            for (int i = 0; i < arr.length(); i++) result.add(CallItem.fromJson(arr.getJSONObject(i)));
        } catch (Exception ignored) {}
        return result;
    }

    private static void saveCalls(Context c, List<CallItem> calls) {
        JSONArray arr = new JSONArray();
        for (CallItem item : calls) arr.put(item.toJson());
        prefs(c).edit().putString(KEY_CALLS, arr.toString()).apply();
    }

    public static synchronized void addCall(Context c, CallItem item) {
        List<CallItem> calls = loadCalls(c);
        calls.add(0, item);
        if (calls.size() > 500) calls = new ArrayList<>(calls.subList(0, 500));
        saveCalls(c, calls);
    }

    public static String smartReply(String input) {
        String x = input == null ? "" : input.toLowerCase(Locale.ROOT);
        if (x.contains("hello") || x.contains("hi") || x.contains("\u0645\u0631\u062d\u0628\u0627")) return "Hello. I received your message.";
        if (x.contains("meeting") || x.contains("\u0645\u0648\u0639\u062f")) return "The schedule looks busy. Send me the preferred time.";
        if (x.contains("call") || x.contains("\u0645\u0643\u0627\u0644\u0645\u0629")) return "I can call you later. This is a simulated reply.";
        if (x.contains("thanks") || x.contains("\u0634\u0643\u0631\u0627")) return "You are welcome.";
        String[] replies = {
                "Got it. I will get back to you shortly.",
                "Noted. I am checking that now.",
                "Okay. Send me the remaining details when ready.",
                "I saw your message. I will reply when available.",
                "Understood. This is an automatic simulated response."
        };
        return replies[RANDOM.nextInt(replies.length)];
    }

    public static String formatTime(long time) {
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(time));
    }

    public static String formatDateTime(long time) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(time));
    }

    public static String getPrefix(Context c) {
        return prefs(c).getString(KEY_PREFIX, "+963");
    }

    public static void setPrefix(Context c, String value) {
        prefs(c).edit().putString(KEY_PREFIX, value).apply();
    }

    public static String getNotificationSound(Context c) {
        return prefs(c).getString(KEY_NOTIFICATION_SOUND, "");
    }

    public static void setNotificationSound(Context c, String uri) {
        prefs(c).edit().putString(KEY_NOTIFICATION_SOUND, uri == null ? "" : uri).apply();
    }

    public static String getCallSound(Context c) {
        return prefs(c).getString(KEY_CALL_SOUND, "");
    }

    public static void setCallSound(Context c, String uri) {
        prefs(c).edit().putString(KEY_CALL_SOUND, uri == null ? "" : uri).apply();
    }

    public static String getIcon(Context c) {
        return prefs(c).getString(KEY_ICON, "green");
    }

    public static void setIcon(Context c, String icon) {
        prefs(c).edit().putString(KEY_ICON, icon).apply();
    }

    public static boolean isBotActive(Bot b) {
        int hour = Integer.parseInt(new SimpleDateFormat("H", Locale.US).format(new Date()));
        if (b.activeFrom == b.activeTo) return true;
        if (b.activeFrom < b.activeTo) return hour >= b.activeFrom && hour < b.activeTo;
        return hour >= b.activeFrom || hour < b.activeTo;
    }

    public static synchronized void resetDemo(Context c) {
        prefs(c).edit().remove(KEY_BOTS).remove(KEY_MESSAGES).remove(KEY_CALLS).apply();
        ensureSeeded(c);
    }
}
