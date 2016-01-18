package com.teinvdlugt.android.bloggerreader;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

public class AlarmUtils {

    private AlarmUtils() {
    }

    public static void setOrCancelAlarm(Context context, boolean set) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 1, new Intent(context, Receiver.class), PendingIntent.FLAG_UPDATE_CURRENT);

        if (set) alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                AlarmManager.INTERVAL_HOUR, AlarmManager.INTERVAL_HOUR, pendingIntent);
        else alarmManager.cancel(pendingIntent);

        ComponentName receiver = new ComponentName(context, Receiver.class);
        PackageManager pm = context.getPackageManager();
        int receiverEnabled = set ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        pm.setComponentEnabledSetting(receiver,
                receiverEnabled,
                PackageManager.DONT_KILL_APP);
    }
}
