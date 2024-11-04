package com.earspeakervolumebooster.callvolumeincreaser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;

public class OnBootReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        Settings settings = new Settings(context, false);
        settings.load(PreferenceManager.getDefaultSharedPreferences(context));
        settings.setEarpiece();
        if (settings.needService()) {
            context.startService(new Intent(context, EarpieceService.class));
        }
    }
}
