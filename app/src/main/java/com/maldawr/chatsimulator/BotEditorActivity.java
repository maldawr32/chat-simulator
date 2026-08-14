package com.maldawr.chatsimulator;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class BotEditorActivity extends Activity {
    private static final int REQ_IMAGE = 701;

    private Store.Bot bot;
    private EditText name;
    private EditText phone;
    private EditText status;
    private EditText unread;
    private EditText activeFrom;
    private EditText activeTo;
    private CheckBox autoReply;
    private ImageView avatarPreview;
    private String avatarUri = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Store.ensureSeeded(this);
        long id = getIntent().getLongExtra("bot_id", -1L);
        bot = id == -1L ? null : Store.getBot(this, id);
        if (bot == null) {
            bot = new Store.Bot(System.currentTimeMillis(), "New Fictional Bot", Store.getPrefix(this) + " 000 000 000");
        }
        avatarUri = bot.avatarUri == null ? "" : bot.avatarUri;
        buildUi();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.bg(this));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.addView(Ui.safetyBanner(this));

        ScrollView scroll = new ScrollView(this);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 24));

        TextView title = Ui.label(this, "Fictional bot editor", 24, true);
        form.addView(title);

        avatarPreview = new ImageView(this);
        avatarPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        avatarPreview.setBackground(Ui.circle(0xFF4C7F86));
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(Ui.dp(this, 96), Ui.dp(this, 96));
        avatarParams.setMargins(0, Ui.dp(this, 12), 0, Ui.dp(this, 8));
        form.addView(avatarPreview, avatarParams);
        if (!avatarUri.isEmpty()) {
            try { avatarPreview.setImageURI(Uri.parse(avatarUri)); } catch (Exception ignored) {}
        }
        Button choose = Ui.button(this, "Choose profile image");
        choose.setOnClickListener(v -> chooseImage());
        form.addView(choose);

        name = field("Name", bot.name);
        phone = field("Fictional phone number", bot.phone);
        status = field("Status", bot.status);
        unread = field("Unread counter", String.valueOf(bot.unread));
        activeFrom = field("Active from hour (0-23)", String.valueOf(bot.activeFrom));
        activeTo = field("Active until hour (1-24)", String.valueOf(bot.activeTo));
        form.addView(name);
        form.addView(phone);
        form.addView(status);
        form.addView(unread);
        form.addView(activeFrom);
        form.addView(activeTo);

        autoReply = new CheckBox(this);
        autoReply.setText("Automatic smart routine replies");
        autoReply.setTextColor(Ui.text(this));
        autoReply.setChecked(bot.autoReply);
        form.addView(autoReply);

        Button save = Ui.button(this, "Save fictional bot");
        save.setOnClickListener(v -> save());
        form.addView(save);

        TextView note = Ui.label(this, "Tip: long-press a bot on the chat list to edit it again.", 12, false);
        note.setTextColor(Ui.sub(this));
        form.addView(note);

        scroll.addView(form, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
    }

    private EditText field(String hint, String value) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setText(value == null ? "" : value);
        e.setTextColor(Ui.text(this));
        e.setHintTextColor(Ui.sub(this));
        return e;
    }

    private void chooseImage() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        startActivityForResult(i, REQ_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_IMAGE || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            getContentResolver().takePersistableUriPermission(uri, flags);
        } catch (Exception ignored) {}
        avatarUri = uri.toString();
        avatarPreview.setImageURI(uri);
    }

    private void save() {
        bot.name = clean(name, "Fictional Bot");
        bot.phone = clean(phone, Store.getPrefix(this) + " 000 000 000");
        bot.status = clean(status, "SIMULATION - fictional contact");
        bot.unread = clampInt(unread.getText().toString(), 0, 99999, 0);
        bot.activeFrom = clampInt(activeFrom.getText().toString(), 0, 23, 0);
        bot.activeTo = clampInt(activeTo.getText().toString(), 1, 24, 24);
        bot.autoReply = autoReply.isChecked();
        bot.avatarUri = avatarUri;
        Store.saveBot(this, bot);
        Toast.makeText(this, "Bot saved", Toast.LENGTH_SHORT).show();
        finish();
    }

    private String clean(EditText e, String fallback) {
        String x = e.getText().toString().trim();
        return x.isEmpty() ? fallback : x;
    }

    private int clampInt(String raw, int min, int max, int fallback) {
        try {
            int value = Integer.parseInt(raw.trim());
            return Math.max(min, Math.min(max, value));
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
