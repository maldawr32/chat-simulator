package com.maldawr.chatsimulator;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class DeepSeekClient {
    public interface Callback { void onSuccess(String value); void onError(String error); }
    private static final String ENDPOINT = "https://api.deepseek.com/chat/completions";
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private DeepSeekClient() {}

    public static String replyBlocking(Context context, Store.Bot bot, String userText) throws Exception {
        String key = AiPrefs.getApiKey(context);
        if (key.isEmpty()) throw new IllegalStateException("DeepSeek API key is not configured");
        JSONArray messages = new JSONArray();
        JSONObject system = new JSONObject();
        system.put("role", "system");
        String group = bot.groupChat ? " This is a fictional group. Members shown by the app: " + bot.groupSubtitle + ". Return only the message text; the app chooses the fictional sender." : "";
        system.put("content", "You are a fictional participant inside a local app called Chat Simulator. Never claim to be a real person, WhatsApp, or a real messaging service. Reply naturally in the same language as the user. Keep routine replies concise and conversational. The visible UI always labels the conversation as SIMULATION. Fictional contact name: " + bot.name + ". Personality: " + (bot.personality == null ? "friendly" : bot.personality) + "." + group);
        messages.put(system);

        List<Store.Message> recent = Store.recentMessages(context, bot.id, 12);
        boolean currentIncluded = false;
        for (Store.Message m : recent) {
            if ("reaction_event".equals(m.kind) || m.text == null || m.text.trim().isEmpty()) continue;
            JSONObject item = new JSONObject();
            item.put("role", m.incoming ? "assistant" : "user");
            String content = m.text;
            if (bot.groupChat && m.incoming && m.sender != null && !m.sender.isEmpty()) content = m.sender + ": " + content;
            item.put("content", content);
            messages.put(item);
            if (!m.incoming && m.text.trim().equals(userText == null ? "" : userText.trim())) currentIncluded = true;
        }
        if (!currentIncluded) messages.put(new JSONObject().put("role","user").put("content",userText == null ? "" : userText));
        return request(context, key, messages, 220);
    }

    public static void testAsync(Context context, Callback callback) {
        Context app = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            try {
                String key = AiPrefs.getApiKey(app);
                if (key.isEmpty()) throw new IllegalStateException("API key is empty");
                JSONArray messages = new JSONArray();
                messages.put(new JSONObject().put("role","system").put("content","You are testing a fictional Chat Simulator integration."));
                messages.put(new JSONObject().put("role","user").put("content","Reply with OK only."));
                String value = request(app, key, messages, 16);
                MAIN.post(() -> callback.onSuccess(value));
            } catch (Exception e) {
                String msg = e.getMessage() == null ? "Connection failed" : e.getMessage();
                MAIN.post(() -> callback.onError(msg));
            }
        });
    }

    private static String request(Context context, String apiKey, JSONArray messages, int maxTokens) throws Exception {
        JSONObject body = new JSONObject();
        body.put("model", AiPrefs.getModel(context));
        body.put("messages", messages);
        body.put("max_tokens", maxTokens);
        body.put("thinking", new JSONObject().put("type", AiPrefs.isThinking(context) ? "enabled" : "disabled"));
        if (AiPrefs.isThinking(context)) body.put("reasoning_effort", AiPrefs.getEffort(context));
        body.put("stream", false);

        HttpURLConnection connection = (HttpURLConnection) new URL(ENDPOINT).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(45000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
        connection.setFixedLengthStreamingMode(bytes.length);
        try (OutputStream out = connection.getOutputStream()) { out.write(bytes); }

        int code = connection.getResponseCode();
        InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
        String response = read(stream);
        connection.disconnect();
        if (code < 200 || code >= 300) {
            String detail = "HTTP " + code;
            try {
                JSONObject error = new JSONObject(response).optJSONObject("error");
                if (error != null) detail += " • " + error.optString("message", "DeepSeek request failed");
            } catch (Exception ignored) {}
            throw new IllegalStateException(detail);
        }
        JSONObject json = new JSONObject(response);
        JSONArray choices = json.optJSONArray("choices");
        if (choices == null || choices.length() == 0) throw new IllegalStateException("DeepSeek returned no reply");
        String content = choices.getJSONObject(0).getJSONObject("message").optString("content", "").trim();
        if (content.isEmpty()) throw new IllegalStateException("DeepSeek returned an empty reply");
        return content;
    }

    private static String read(InputStream input) throws Exception {
        if (input == null) return "";
        try (InputStream in = input; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192]; int n;
            while ((n = in.read(buffer)) >= 0) out.write(buffer, 0, n);
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }
}
