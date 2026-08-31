package com.maldawr.chatsimulator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        Store.ensureSeeded(context);
        V8Seeder.ensure(context);
        NotificationHelper.ensureChannels(context);
        AutomationManager.ensureScheduled(context);
    }
}
