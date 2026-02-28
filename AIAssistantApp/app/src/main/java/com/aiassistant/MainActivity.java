package com.aiassistant;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import okhttp3.*;

public class MainActivity extends AppCompatActivity {

    // ─── Cloudflare AI Endpoints (fallback chain) ─────────────────────────────
    private static final String[] API_URLS = {
        "https://ai-chat.pastefyuser1231.workers.dev/api/chat",
        "https://steep-union-c19f.eee199425.workers.dev/api/chat",
        "https://holy-glitter-7345.foals-option9u.workers.dev/api/chat"
    };

    private static final String MODEL       = "@cf/meta/llama-3-8b-instruct";
    private static final String SYS_PROMPT  =
        "You are a helpful AI assistant running on Android. " +
        "When the user wants to find or open something, reply ONLY with one of these commands:\n" +
        "  FIND:<query>        — search YouTube for <query>\n" +
        "  OPEN:<url>          — open a URL\n" +
        "  CHAT:<message>      — send a plain chat reply\n" +
        "  WORDLE:<hint>       — give a Wordle word suggestion\n" +
        "For all other conversation, use CHAT:<message>. Keep replies concise.";

    // ─── UI ───────────────────────────────────────────────────────────────────
    private ScrollView  scrollView;
    private TextView    chatLog;
    private EditText    inputField;
    private Button      sendButton;
    private ProgressBar loadingBar;

    // ─── Networking ───────────────────────────────────────────────────────────
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
        .connectTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build();

    // ─── Conversation memory (last 10 turns) ──────────────────────────────────
    private final JSONArray conversationHistory = new JSONArray();

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scrollView  = findViewById(R.id.scrollView);
        chatLog     = findViewById(R.id.chatLog);
        inputField  = findViewById(R.id.inputField);
        sendButton  = findViewById(R.id.sendButton);
        loadingBar  = findViewById(R.id.loadingBar);

        appendMessage("AI", "Hello! I'm your AI assistant. Ask me anything, or say 'find cat videos' or 'open YouTube'.");

        sendButton.setOnClickListener(v -> handleUserInput());

        inputField.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND ||
               (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                handleUserInput();
                return true;
            }
            return false;
        });

        // Quick-action buttons
        findViewById(R.id.btnExplore).setOnClickListener(v -> submitMessage("Show me something interesting"));
        findViewById(R.id.btnYouTube).setOnClickListener(v -> submitMessage("Open YouTube"));
        findViewById(R.id.btnWordle).setOnClickListener(v -> submitMessage("Give me a Wordle hint"));
    }

    // ─── Input handling ───────────────────────────────────────────────────────
    private void handleUserInput() {
        String text = inputField.getText().toString().trim();
        if (text.isEmpty()) return;
        inputField.setText("");
        submitMessage(text);
    }

    private void submitMessage(String userMessage) {
        appendMessage("You", userMessage);
        setLoading(true);

        // Add user turn to history
        try {
            JSONObject userObj = new JSONObject();
            userObj.put("role", "user");
            userObj.put("content", userMessage);
            conversationHistory.put(userObj);
            // Keep only last 10 messages to avoid huge payloads
            while (conversationHistory.length() > 10) conversationHistory.remove(0);
        } catch (JSONException e) { e.printStackTrace(); }

        new AiRequestTask().execute(userMessage);
    }

    // ─── Async AI request ─────────────────────────────────────────────────────
    private class AiRequestTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            return sendAiMessage();
        }

        @Override
        protected void onPostExecute(String reply) {
            setLoading(false);
            processAiReply(reply);

            // Remember assistant reply
            try {
                JSONObject assistantObj = new JSONObject();
                assistantObj.put("role", "assistant");
                assistantObj.put("content", reply);
                conversationHistory.put(assistantObj);
            } catch (JSONException e) { e.printStackTrace(); }
        }
    }

    // ─── HTTP call with fallback ───────────────────────────────────────────────
    private String sendAiMessage() {
        JSONObject payload = buildPayload();
        if (payload == null) return "Error building request.";

        RequestBody body = RequestBody.create(
            payload.toString(), MediaType.parse("application/json"));

        for (String url : API_URLS) {
            try {
                Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("User-Agent", "AndroidAIAssistant/1.0")
                    .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.body() != null) {
                        String json = response.body().string();
                        JSONObject obj = new JSONObject(json);
                        if (obj.has("reply") && !obj.getString("reply").isEmpty()) {
                            return obj.getString("reply");
                        }
                    }
                }
            } catch (Exception e) {
                // Silent — try next URL
            }
        }
        return "CHAT:Sorry, I couldn't reach any AI endpoint right now. Try again shortly.";
    }

    private JSONObject buildPayload() {
        try {
            JSONArray messages = new JSONArray();

            // System prompt first
            JSONObject sys = new JSONObject();
            sys.put("role", "system");
            sys.put("content", SYS_PROMPT);
            messages.put(sys);

            // Append conversation history
            for (int i = 0; i < conversationHistory.length(); i++) {
                messages.put(conversationHistory.getJSONObject(i));
            }

            JSONObject payload = new JSONObject();
            payload.put("messages", messages);
            payload.put("model",    MODEL);
            payload.put("system",   SYS_PROMPT);
            return payload;

        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    // ─── Command parser ────────────────────────────────────────────────────────
    private void processAiReply(String reply) {
        if (reply == null || reply.isEmpty()) {
            appendMessage("AI", "I didn't get a response. Please try again.");
            return;
        }

        if (reply.startsWith("FIND:")) {
            String target = reply.substring(5).trim();
            appendMessage("AI", "🔍 Searching for: " + target);
            findTarget(target);

        } else if (reply.startsWith("OPEN:")) {
            String url = reply.substring(5).trim();
            appendMessage("AI", "🌐 Opening: " + url);
            openUrl(url);

        } else if (reply.startsWith("CHAT:")) {
            String message = reply.substring(5).trim();
            appendMessage("AI", message);

        } else if (reply.startsWith("WORDLE:")) {
            String hint = reply.substring(7).trim();
            appendMessage("AI", "🟩 Wordle suggestion: " + hint);

        } else {
            // Raw reply — show as-is
            appendMessage("AI", reply);
        }
    }

    // ─── Actions ──────────────────────────────────────────────────────────────

    /** Try installed apps first, fall back to YouTube search */
    private void findTarget(String target) {
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo appInfo : apps) {
            String label = pm.getApplicationLabel(appInfo).toString();
            if (label.toLowerCase().contains(target.toLowerCase())) {
                Intent launch = pm.getLaunchIntentForPackage(appInfo.packageName);
                if (launch != null) {
                    startActivity(launch);
                    Toast.makeText(this, "Opening: " + label, Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }

        // Fall back to YouTube search
        String ytUrl = "https://www.youtube.com/results?search_query=" + Uri.encode(target);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(ytUrl));
        intent.setPackage("com.google.android.youtube");
        try {
            startActivity(intent);
        } catch (Exception e) {
            // YouTube not installed — open in browser
            intent.setPackage(null);
            startActivity(intent);
        }
        Toast.makeText(this, "Searching YouTube: " + target, Toast.LENGTH_SHORT).show();
    }

    private void openUrl(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            appendMessage("AI", "⚠️ Couldn't open that URL.");
        }
    }

    // ─── UI helpers ───────────────────────────────────────────────────────────
    private void appendMessage(String sender, String message) {
        runOnUiThread(() -> {
            String emoji = sender.equals("You") ? "🧑 You" : "🤖 AI";
            chatLog.append(emoji + ": " + message + "\n\n");
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        });
    }

    private void setLoading(boolean loading) {
        runOnUiThread(() -> {
            loadingBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            sendButton.setEnabled(!loading);
            inputField.setEnabled(!loading);
        });
    }
}
