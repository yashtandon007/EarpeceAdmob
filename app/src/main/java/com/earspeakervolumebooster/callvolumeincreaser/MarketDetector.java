package com.earspeakervolumebooster.callvolumeincreaser;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;

public class MarketDetector {
    public static final int APPSTORE = 1;
    public static final int MARKET = 0;

    public static void launch(Context c) {
        Intent i = new Intent("android.intent.action.VIEW");
        i.setFlags(268435456);
        if (detect(c) == 0) {
            i.setData(Uri.parse("market://search?q=pub:\"Omega Centauri Software\""));
        } else {
            i.setData(Uri.parse("http://www.amazon.com/gp/mas/dl/android?p=mobi.omegacentauri.ScreenDim.Full&showAll=1"));
        }
        c.startActivity(i);
    }

    public static int detect(Context c) {
        if (VERSION.SDK_INT < 5) {
            return 1;
        }
        PackageManager pm = c.getPackageManager();
        String installer = pm.getInstallerPackageName(c.getPackageName());
        if (installer != null && installer.equals("com.android.vending")) {
            return 0;
        }
        if (Build.MODEL.equalsIgnoreCase("Kindle Fire")) {
            return 1;
        }
        try {
            if (pm.getPackageInfo("com.amazon.venezia", 0) != null) {
                return 1;
            }
        } catch (NameNotFoundException e) {
        }
        return 0;
    }
}
