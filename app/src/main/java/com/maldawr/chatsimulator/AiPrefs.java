package com.maldawr.chatsimulator;

import android.content.Context;
import android.content.SharedPreferences;

public final class AiPrefs {
    private static final String PREFS = "chat_simulator_ai_v8";
    private static final String ENABLED = "enabled", MODEL = "model", THINKING = "thinking", EFFORT = "effort";

    private AiPrefs() {}
    private static SharedPreferences p(Context c){ return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    public static boolean isEnabled(Context c){ return p(c).getBoolean(ENABLED, false); }
    public static void setEnabled(Context c, boolean value){ p(c).edit().putBoolean(ENABLED, value).apply(); }
    public static String getModel(Context c){ String m=p(c).getString(MODEL,"deepseek-v4-flash"); return "deepseek-v4-pro".equals(m)?m:"deepseek-v4-flash"; }
    public static void setModel(Context c,String value){ p(c).edit().putString(MODEL,"deepseek-v4-pro".equals(value)?value:"deepseek-v4-flash").apply(); }
    public static boolean isThinking(Context c){ return p(c).getBoolean(THINKING, false); }
    public static void setThinking(Context c,boolean value){ p(c).edit().putBoolean(THINKING,value).apply(); }
    public static String getEffort(Context c){ String e=p(c).getString(EFFORT,"low"); return "max".equals(e)||"high".equals(e)?e:"low"; }
    public static void setEffort(Context c,String value){ p(c).edit().putString(EFFORT,"max".equals(value)||"high".equals(value)?value:"low").apply(); }
    public static String getApiKey(Context c){ return SecureSecretStore.load(c); }
    public static boolean saveApiKey(Context c,String value){ return SecureSecretStore.save(c,value); }
    public static void clearApiKey(Context c){ SecureSecretStore.clear(c); }
    public static boolean shouldUseDeepSeek(Context c){ return isEnabled(c) && !getApiKey(c).isEmpty(); }
}
