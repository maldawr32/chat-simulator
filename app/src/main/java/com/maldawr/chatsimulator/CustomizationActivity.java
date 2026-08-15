package com.maldawr.chatsimulator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class CustomizationActivity extends Activity {
    private static final int PICK_NOTIFICATION_ICON = 901;
    private static final int PICK_HOME_ICON = 902;

    private EditText titleInput;
    private TextView sizeValue;
    private SeekBar sizeSeek;
    private ImageView notificationPreview;
    private ImageView homePreview;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0B141A);
        root.addView(Ui.safetyBanner(this));

        ScrollView scroll = new ScrollView(this);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 32));

        TextView title = Ui.label(this, "Appearance & custom images", 25, true);
        title.setTextColor(0xFFFFFFFF);
        body.addView(title);

        addSection(body, "Home title");
        titleInput = new EditText(this);
        titleInput.setSingleLine(true);
        titleInput.setText(Store.getHomeTitle(this));
        titleInput.setTextColor(0xFFFFFFFF);
        titleInput.setHintTextColor(0xFF8696A0);
        titleInput.setHint("Chat Simulator");
        titleInput.setBackground(Ui.rounded(0xFF1F2C33, 14, this));
        titleInput.setPadding(Ui.dp(this, 14), 0, Ui.dp(this, 14), 0);
        body.addView(titleInput, new LinearLayout.LayoutParams(-1, Ui.dp(this, 52)));

        LinearLayout sizeRow = new LinearLayout(this);
        sizeRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView sizeLabel = Ui.label(this, "Home title size", 15, true);
        sizeLabel.setTextColor(0xFFFFFFFF);
        sizeRow.addView(sizeLabel, new LinearLayout.LayoutParams(0, Ui.dp(this, 44), 1f));
        sizeValue = Ui.label(this, Store.getHomeTitleSize(this) + " sp", 14, true);
        sizeValue.setTextColor(0xFF25D366);
        sizeValue.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        sizeRow.addView(sizeValue);
        body.addView(sizeRow);

        sizeSeek = new SeekBar(this);
        sizeSeek.setMax(20);
        sizeSeek.setProgress(Store.getHomeTitleSize(this) - 18);
        sizeSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sizeValue.setText((18 + progress) + " sp");
            }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        body.addView(sizeSeek);

        Button saveTitle = Ui.button(this, "Save home title");
        saveTitle.setOnClickListener(v -> {
            String value = titleInput.getText().toString().trim();
            if (value.isEmpty()) value = "Chat Simulator";
            Store.setHomeTitle(this, value);
            Store.setHomeTitleSize(this, 18 + sizeSeek.getProgress());
            Toast.makeText(this, "Home title updated", Toast.LENGTH_SHORT).show();
        });
        body.addView(saveTitle);

        addSection(body, "Notification icon from gallery");
        TextView notificationNote = Ui.label(this,
                "Choose any image. The simulator converts it to a notification-safe monochrome icon and adds a small S marker. The color image can also appear as the large notification image.",
                13, false);
        notificationNote.setTextColor(0xFF8696A0);
        notificationNote.setPadding(0, 0, 0, Ui.dp(this, 10));
        body.addView(notificationNote);

        notificationPreview = preview();
        body.addView(notificationPreview, previewParams());
        refreshNotificationPreview();

        Button pickNotification = Ui.button(this, "Choose notification image from gallery");
        pickNotification.setOnClickListener(v -> pickImage(PICK_NOTIFICATION_ICON));
        body.addView(pickNotification);

        Button resetNotification = Ui.button(this, "Use default notification icon");
        resetNotification.setOnClickListener(v -> {
            CustomizationHelper.resetNotificationIcon(this);
            refreshNotificationPreview();
            Toast.makeText(this, "Default notification icon restored", Toast.LENGTH_SHORT).show();
        });
        body.addView(resetNotification);

        addSection(body, "Custom home-screen app icon");
        TextView homeNote = Ui.label(this,
                "Android does not allow replacing the installed app-drawer icon with an arbitrary gallery image. This creates or updates a pinned home-screen shortcut using your selected image and a visible SIM badge.",
                13, false);
        homeNote.setTextColor(0xFF8696A0);
        homeNote.setPadding(0, 0, 0, Ui.dp(this, 10));
        body.addView(homeNote);

        homePreview = preview();
        body.addView(homePreview, previewParams());
        refreshHomePreview();

        Button pickHome = Ui.button(this, "Choose home-screen icon from gallery");
        pickHome.setOnClickListener(v -> pickImage(PICK_HOME_ICON));
        body.addView(pickHome);

        Button publish = Ui.button(this, "Add / update custom home shortcut");
        publish.setOnClickListener(v -> {
            if (!CustomizationHelper.hasHomeIcon(this)) {
                Toast.makeText(this, "Choose an image first", Toast.LENGTH_SHORT).show();
                return;
            }
            boolean ok = CustomizationHelper.publishOrUpdateHomeShortcut(this);
            Toast.makeText(this, ok ? "Shortcut request sent" : "Launcher did not accept the shortcut", Toast.LENGTH_LONG).show();
        });
        body.addView(publish);

        addSection(body, "Built-in launcher icon");
        Button launcher = Ui.button(this, "Choose built-in launcher icon");
        launcher.setOnClickListener(v -> chooseLauncher());
        body.addView(launcher);

        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(root);
    }

    private void addSection(LinearLayout body, String value) {
        TextView section = Ui.label(this, value, 15, true);
        section.setTextColor(0xFF25D366);
        section.setPadding(0, Ui.dp(this, 24), 0, Ui.dp(this, 10));
        body.addView(section);
    }

    private ImageView preview() {
        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackground(Ui.rounded(0xFF1F2C33, 18, this));
        image.setPadding(Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8));
        return image;
    }

    private LinearLayout.LayoutParams previewParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(this, 96), Ui.dp(this, 96));
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        lp.setMargins(0, 0, 0, Ui.dp(this, 12));
        return lp;
    }

    private void refreshNotificationPreview() {
        android.graphics.Bitmap bitmap = CustomizationHelper.notificationLargeBitmap(this);
        if (bitmap != null) notificationPreview.setImageBitmap(bitmap);
        else notificationPreview.setImageResource(R.drawable.ic_notification_custom);
    }

    private void refreshHomePreview() {
        android.graphics.Bitmap bitmap = CustomizationHelper.homeIconBitmap(this);
        if (bitmap != null) homePreview.setImageBitmap(bitmap);
        else homePreview.setImageResource(R.mipmap.ic_launcher);
    }

    private void pickImage(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, requestCode);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {}

        if (requestCode == PICK_NOTIFICATION_ICON) {
            boolean ok = CustomizationHelper.saveNotificationIcon(this, uri);
            refreshNotificationPreview();
            Toast.makeText(this, ok ? "Notification icon saved" : "Could not process that image", Toast.LENGTH_SHORT).show();
        } else if (requestCode == PICK_HOME_ICON) {
            boolean ok = CustomizationHelper.saveHomeIcon(this, uri);
            refreshHomePreview();
            Toast.makeText(this, ok ? "Home icon saved" : "Could not process that image", Toast.LENGTH_SHORT).show();
        }
    }

    private void chooseLauncher() {
        String[] values = {"Green custom", "Blue", "Purple"};
        new AlertDialog.Builder(this)
                .setTitle("Built-in launcher icon")
                .setItems(values, (dialog, which) -> LauncherIconHelper.apply(this, which == 1 ? "blue" : which == 2 ? "purple" : "green"))
                .show();
    }
}
