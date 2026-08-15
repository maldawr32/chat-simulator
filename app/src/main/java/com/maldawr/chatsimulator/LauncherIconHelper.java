package com.maldawr.chatsimulator;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.widget.Toast;

public final class LauncherIconHelper {
    private LauncherIconHelper() {}

    public static void apply(Context context, String selected) {
        PackageManager pm = context.getPackageManager();
        String pkg = context.getPackageName();
        String[] names = {"LauncherGreen", "LauncherBlue", "LauncherPurple"};
        for (String name : names) {
            pm.setComponentEnabledSetting(
                    new ComponentName(pkg, pkg + "." + name),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
            );
        }
        String target = "blue".equals(selected) ? "LauncherBlue" : "purple".equals(selected) ? "LauncherPurple" : "LauncherGreen";
        pm.setComponentEnabledSetting(
                new ComponentName(pkg, pkg + "." + target),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
        );
        Store.setIcon(context, selected);
        Toast.makeText(context, "Launcher icon updated", Toast.LENGTH_SHORT).show();
    }
}
