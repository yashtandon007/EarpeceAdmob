package com.earspeakervolumebooster.callvolumeincreaser;

import static com.earspeakervolumebooster.callvolumeincreaser.Options.getNotify;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.app.Service;
import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Build.VERSION;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.os.Looper;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.Random;

public class EarpieceService extends Service implements SensorEventListener {
    private static final String GUARD_TAG = "mobi.omegacentauri.Earpiece.EarpieceService.guard";
    private static final int PROXIMITY_SCREEN_OFF_WAKE_LOCK = 32;
    private static final String PROXIMITY_TAG = "EarpeceAdmob:proximity";
    private boolean closeToPhone = false;
    private boolean closeToPhoneValid = false;
    private KeyguardLock guardLock = null;
    private volatile boolean interruptReader = false;
    private KeyguardManager km;
    private volatile Process logProcess = null;
    private Thread logThread = null;
    private Messenger messenger;
    private SharedPreferences options;
    protected boolean phoneOn = false;
    private PhoneStateListener phoneStateListener;
    private PowerManager pm;
    private Sensor proximitySensor = null;
    private boolean quietedCamera = false;
    private ScreenOnOffReceiver screenOnOffReceiver;
    private Settings settings;
    private long t0;
    private TelephonyManager tm;
    private Handler unquietCameraHandler = new Handler();
    private final Runnable unquietCameraRunnable = new C00091();
    private WakeLock wakeLock = null;

    class C00091 implements Runnable {
        C00091() {
        }

        public void run() {
            if (EarpieceService.this.quietedCamera) {
                EarpieceService.this.settings.setEqualizer();
                EarpieceService.this.settings.setEarpiece();
                EarpieceService.this.quietedCamera = false;
                Earpiece.log("unquieting camera...");
            }
        }
    }

    class C00102 extends PhoneStateListener {
        C00102() {
        }

        public void onCallStateChanged(int state, String incomingNumber) {
            Earpiece.log("phone state:" + state);
            EarpieceService.this.phoneOn = state == 2;
            EarpieceService.this.closeToPhoneValid = true;
            EarpieceService.this.closeToPhone = false;
            if (EarpieceService.this.phoneOn) {
                if (EarpieceService.this.proximitySensor == null) {
                    EarpieceService.this.proximitySensor = EarpieceService.this.settings.proximitySensor;
                        if (EarpieceService.this.proximitySensor != null
                                && EarpieceService.this.settings.sensorManager != null) {
                            EarpieceService.this.settings.sensorManager.registerListener(
                                    EarpieceService.this, EarpieceService.this.proximitySensor,
                                    SensorManager.SENSOR_DELAY_NORMAL);
                            Earpiece.log("Registering proximity sensor");
                        }
                }
            } else if (EarpieceService.this.proximitySensor != null) {
                EarpieceService.this.disableProximitySensor();
                Earpiece.log("Closing proximity sensor");
            }
            EarpieceService.this.updateSpeakerPhone();
        }
    }

    class C00113 implements Runnable {
        C00113() {
        }

        public void run() {
            EarpieceService.this.monitorLog();
        }
    }

    private static class IncomingHandler extends Handler {
        public static final int MSG_OFF = 0;
        public static final int MSG_ON = 1;
        public static final int MSG_RELOAD_SETTINGS = 2;

        private final WeakReference<EarpieceService> serviceReference;

        IncomingHandler(EarpieceService service) {
            super(Looper.getMainLooper());
            this.serviceReference = new WeakReference<>(service);
        }

        public void handleMessage(Message m) {
            EarpieceService service = this.serviceReference.get();
            if (service == null) {
                return;
            }
            Earpiece.log("Message: " + m.what);
            switch (m.what) {
                case 2:
                    service.settings.load(service.options);
                    service.settings.setEqualizer();
                    service.updateProximity();
                    return;
                default:
                    super.handleMessage(m);
                    return;
            }
        }
    }

    class ScreenOnOffReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                handleScreen(context, true);
            } else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                handleScreen(context, false);
            }
        }

        void handleScreen(Context context, boolean on) {
            if (EarpieceService.this.settings.notifyLightOnlyWhenOff) {
                Earpiece.log("setting notify light status " + on);
                EarpieceService.this.setNotificationLight(!on);
            }
        }
    }

    public IBinder onBind(Intent arg0) {
        if (this.messenger == null) {
            this.messenger = new Messenger(new IncomingHandler(this));
        }
        return this.messenger.getBinder();
    }

    @SuppressLint({"NewApi"})
    public void onCreate() {
        int i;
        this.t0 = System.currentTimeMillis();
        Earpiece.log("create service " + this.t0);
        this.options = PreferenceManager.getDefaultSharedPreferences(this);
        this.pm = (PowerManager) getSystemService(POWER_SERVICE);
        this.km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        this.settings = new Settings(this, true);
        this.settings.load(this.options);
        if (this.settings.needScreenOnOffReceiver()) {
            this.screenOnOffReceiver = new ScreenOnOffReceiver();
            IntentFilter filter = new IntentFilter("android.intent.action.SCREEN_ON");
            filter.addAction("android.intent.action.SCREEN_OFF");
            registerReceiver(this.screenOnOffReceiver, filter);
        } else {
            this.screenOnOffReceiver = null;
        }
        if (this.settings.haveTelephony()) {
            this.tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        } else {
            this.tm = null;
        }
        if (getNotify(this.options) != 0) {
            i = R.drawable.equalizer;
        } else {
            i = 0;
        }
        // n.setLatestEventInfo(this, "Earpiece", this.settings.describe(), PendingIntent.getActivity(this, 0, i2, 0));
        if (this.settings.isEqualizerActive()) {
            this.settings.setEqualizer();
        } else {
            this.settings.disableEqualizer();
        }
        updateProximity();
        if (this.tm != null) {
            this.phoneStateListener = new C00102();
        }
        updateAutoSpeakerPhone();
        if (this.options.getBoolean(Options.PREF_DISABLE_KEYGUARD, false)) {
            enableDisableKeyguard();
        }
        if (this.settings.quietCamera) {
            this.logThread = new Thread(new C00113());
            this.logThread.start();
        }
    }

    private void disableProximitySensor() {
        if (this.proximitySensor != null) {
            Earpiece.log("Unregistering proximity sensor");
            this.settings.sensorManager.unregisterListener(this, this.proximitySensor);
            this.proximitySensor = null;
        }
    }

    private void updateSpeakerPhone() {
        boolean z = true;
        if (this.settings.isAutoSpeakerPhoneActive() && hasPhoneStatePermission()) {
            Earpiece.log("updateSpeakerPhone " + this.phoneOn + " " + this.closeToPhone);
            StringBuilder stringBuilder = new StringBuilder("Speaker phone ");
            boolean z2 = this.phoneOn && !this.closeToPhone;
            Earpiece.log(stringBuilder.append(z2).toString());
            if (this.closeToPhoneValid) {
                AudioManager audioManager = this.settings.audioManager;
                if (!this.phoneOn || this.closeToPhone) {
                    z = false;
                }
                audioManager.setSpeakerphoneOn(z);
            }
            updateProximity();
        }
    }

    private void updateAutoSpeakerPhone() {
        if (this.tm != null) {
            if (this.settings.isAutoSpeakerPhoneActive() && hasPhoneStatePermission()) {
                Earpiece.log("Auto speaker phone mode on");
                this.tm.listen(this.phoneStateListener, PROXIMITY_SCREEN_OFF_WAKE_LOCK);
                return;
            }
            if (this.settings.isAutoSpeakerPhoneActive()) {
                Log.w("Earpiece", "Phone state permission is required for automatic speaker mode");
            }
            Earpiece.log("Auto speaker phone mode off");
            this.tm.listen(this.phoneStateListener, 0);
        }
    }

    private boolean hasPhoneStatePermission() {
        return VERSION.SDK_INT < 23
                || checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint({"NewApi"})
    public void onDestroy() {
        Earpiece.log("stop service " + this.t0);
        super.onDestroy();
        this.settings.load(this.options);
        if (this.screenOnOffReceiver != null) {
            unregisterReceiver(this.screenOnOffReceiver);
            this.screenOnOffReceiver = null;
            if (this.settings.notifyLightOnlyWhenOff) {
                setNotificationLight(true);
            }
        }
        if (this.quietedCamera) {
            this.settings.setEarpiece(false);
            this.quietedCamera = false;
            this.unquietCameraHandler.removeCallbacks(this.unquietCameraRunnable);
        }
        Earpiece.log("disabling equalizer");
        this.settings.destroyEqualizer();
        try {
            disableProximity();
        } catch (Exception e) {
            e.printStackTrace();
        }
        disableDisableKeyguard();
        disableProximitySensor();
        if (!(this.tm == null || this.phoneStateListener == null)) {
            this.tm.listen(this.phoneStateListener, 0);
        }
        if (this.logThread != null) {
            this.interruptReader = true;
            try {
                if (this.logProcess != null) {
                    Earpiece.log("Destroying service, killing reader");
                    this.logProcess.destroy();
                }
            } catch (Exception e) {
            }
        }
        if (VERSION.SDK_INT >= 5) {
            stopForeground(true);
        }
    }

    private void setNotificationLight(boolean b) {
        try {
            boolean updated = android.provider.Settings.System.putInt(
                    getContentResolver(), "notification_light_pulse", b ? 1 : 0);
            if (!updated) {
                Log.w("Earpiece", "Notification light setting is unavailable on this device");
            }
        } catch (SecurityException exception) {
            Log.w("Earpiece", "Permission denied while changing notification light setting",
                    exception);
        }
    }

    public void onStart(Intent intent, int flags) {
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        onStart(intent, flags);
        return START_STICKY;
    }

    private void activateProximity() {
        if (this.wakeLock == null) {
            Earpiece.log("activating proximity " + this.t0);
            this.wakeLock = this.pm.newWakeLock(PROXIMITY_SCREEN_OFF_WAKE_LOCK, PROXIMITY_TAG);
            this.wakeLock.setReferenceCounted(false);
            this.wakeLock.acquire();
        }
        enableDisableKeyguard();
    }

    private void disableDisableKeyguard() {
        if (this.guardLock != null) {
            try {
                this.guardLock.reenableKeyguard();
            } catch (Exception e) {
                e.printStackTrace();
            }
            this.guardLock = null;
        }
    }

    private void enableDisableKeyguard() {
        if (this.guardLock == null) {
            this.guardLock = this.km.newKeyguardLock(GUARD_TAG);
            try {
                this.guardLock.disableKeyguard();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void disableProximity() {
        Earpiece.log("wakeLock " + (this.wakeLock != null));
        if (this.wakeLock != null) {
            Earpiece.log("disabling proximity " + this.t0);
            if (this.wakeLock.isHeld()) {
                this.wakeLock.release();
            }
            this.wakeLock = null;
        }
        if (!this.settings.disableKeyguardActive) {
            disableDisableKeyguard();
        }
    }

    private void updateProximity() {
        if (this.settings.isProximityActive() || (this.settings.isAutoSpeakerPhoneActive() && this.phoneOn)) {
            Earpiece.log("Activate proximity");
            activateProximity();
            return;
        }
        Earpiece.log("Disable proximity");
        disableProximity();
    }

    public void onAccuracyChanged(Sensor arg0, int arg1) {
    }

    public void onSensorChanged(SensorEvent event) {
        boolean z = true;
        if (event.sensor == this.settings.proximitySensor) {
            this.closeToPhone = event.values[0] < this.settings.proximitySensor.getMaximumRange();
            this.closeToPhoneValid = true;
            if (!hasPhoneStatePermission()
                    || this.tm.getCallState() != TelephonyManager.CALL_STATE_OFFHOOK) {
                z = false;
            }
            this.phoneOn = z;
            Earpiece.log("onSensorChanged, phone = " + this.tm.getCallState());
            updateSpeakerPhone();
        }
    }

    private boolean needToQuiet(String line) {
        if (line.startsWith("E/AudioPolicyManager")) {
            return line.contains("media stream unmute for camera sound (enforce stream)");
        }
        if (line.startsWith("I/ShotSingle")) {
            return line.contains("ShotSingle::takePicture start");
        }
        if (line.startsWith("E/AXLOG")) {
            return line.contains("Total-Shot2Shot**StartU");
        }
        if (line.startsWith("V/CameraEngine")) {
            return line.contains("scheduleAutoFocus");
        }
        return false;
    }

    private void monitorLog() {
        Random x = new Random();
        Log.v("hook", "set hook");
        while (!this.interruptReader) {
            this.logProcess = null;
            String marker = "mobi.omegacentauri.Earpiece:marker:" + System.currentTimeMillis() + ":" + x.nextLong() + ":";
            try {
                Earpiece.log("logcat monitor starting");
                Log.i("EarpieceMarker", marker);
                this.logProcess = Runtime.getRuntime().exec(VERSION.SDK_INT >= 16 ? new String[]{"su", "-c", "logcat", "-b", "main", "EarpieceMarker:I", "AudioPolicyManager:E", "ShotSingle:I", "AXLOG:E", "CameraEngine:V", "*:S"} : new String[]{"logcat", "-b", "main", "EarpieceMarker:I", "AudioPolicyManager:E", "ShotSingle:I", "AXLOG:E", "CameraEngine:V", "*:S"});
                try (BufferedReader logReader = new BufferedReader(new InputStreamReader(this.logProcess.getInputStream()))) {
                    Earpiece.log("reading");
                    String line;
                    while (!this.interruptReader && (line = logReader.readLine()) != null) {
                        if (marker != null) {
                            if (line.contains(marker)) {
                                marker = null;
                            }
                        } else if (needToQuiet(line)) {
                            this.settings.setEarpiece(true);
                            this.settings.setEqualizer(this.settings.rangeLow);
                            this.quietedCamera = true;
                            this.unquietCameraHandler.removeCallbacks(this.unquietCameraRunnable);
                            this.unquietCameraHandler.postDelayed(this.unquietCameraRunnable, 2000);
                        }
                    }
                }
            } catch (IOException e) {
                Earpiece.log("logcat: " + e);
            } finally {
                if (this.logProcess != null) {
                    this.logProcess.destroy();
                    this.logProcess = null;
                }
            }
            if (this.interruptReader) {
                Earpiece.log("reader interrupted");
                return;
            } else {
                Earpiece.log("logcat monitor died");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e2) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
}
