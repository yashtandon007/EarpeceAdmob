package com.earspeakervolumebooster.callvolumeincreaser;

import static com.earspeakervolumebooster.callvolumeincreaser.Options.PREF_AUTO_SPEAKER_PHONE;
import static com.earspeakervolumebooster.callvolumeincreaser.Options.PREF_BOOST;
import static com.earspeakervolumebooster.callvolumeincreaser.Options.*;
import static com.earspeakervolumebooster.callvolumeincreaser.Options.PREF_EARPIECE_ACTIVE;
import static com.earspeakervolumebooster.callvolumeincreaser.Options.PREF_EQUALIZER_ACTIVE;
import static com.earspeakervolumebooster.callvolumeincreaser.Options.PREF_NOTIFY_LIGHT_ONLY_WHEN_OFF;
import static com.earspeakervolumebooster.callvolumeincreaser.Options.PREF_PROXIMITY;
import static com.earspeakervolumebooster.callvolumeincreaser.Options.PREF_REMOVE_BOOST;
import static com.earspeakervolumebooster.callvolumeincreaser.Options.getMaximumBoost;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.audiofx.Equalizer;
import android.os.Build;
import android.os.Build.VERSION;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ViewConfiguration;

public class Settings {
    public AudioManager audioManager;
    public boolean autoSpeakerPhoneActive;
    public short bands;
    public int boostValue;
    private Context context;
    public boolean disableKeyguardActive;
    public boolean earpieceActive;
    private Equalizer eq;
    public boolean equalizerActive;
    private boolean legacy;
    public int maximumBoostPercent;
    public boolean notifyLightOnlyWhenOff;
    private PackageManager pm;
    public boolean proximity;
    public Sensor proximitySensor;
    public boolean quietCamera;
    public short rangeHigh;
    public short rangeLow;
    private boolean released = true;
    public SensorManager sensorManager;
    private boolean shape = true;

    @SuppressLint({"NewApi"})
    public Settings(Context context, boolean activeEqualizer) {
        this.context = context;
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        this.pm = context.getPackageManager();
        this.proximitySensor = this.sensorManager == null
                ? null : this.sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        this.eq = null;
        if (9 <= VERSION.SDK_INT && !PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_REMOVE_BOOST, false)) {
            try {
                this.eq = new Equalizer(activeEqualizer ? 87654323 : Integer.MIN_VALUE, 0);
                this.bands = this.eq.getNumberOfBands();
                this.rangeLow = this.eq.getBandLevelRange()[0];
                this.rangeHigh = this.eq.getBandLevelRange()[1];
                if (activeEqualizer) {
                    this.released = false;
                    return;
                }
                this.eq.release();
                this.eq = null;
                this.released = true;
            } catch (RuntimeException exception) {
                Log.w("Settings", "Audio effect is unavailable", exception);
                releaseEqualizer();
            }
        }
    }

    public void load(SharedPreferences pref) {
        boolean z;
        this.notifyLightOnlyWhenOff = pref.getBoolean(PREF_NOTIFY_LIGHT_ONLY_WHEN_OFF, false);
        this.equalizerActive = pref.getBoolean(PREF_EQUALIZER_ACTIVE, false);
        this.earpieceActive = pref.getBoolean(PREF_EARPIECE_ACTIVE, false);
        if (pref.getBoolean(PREF_AUTO_SPEAKER_PHONE, false) && haveProximity()) {
            z = true;
        } else {
            z = false;
        }
        this.autoSpeakerPhoneActive = z;
        if (pref.getBoolean(PREF_PROXIMITY, false) && haveProximity()) {
            z = true;
        } else {
            z = false;
        }
        this.proximity = z;
        this.boostValue = pref.getInt(PREF_BOOST, 0);
        int maxBoost = (getMaximumBoost(pref) * this.rangeHigh) / 100;
        if (this.boostValue < 0) {
            this.boostValue = 0;
        } else if (this.boostValue > maxBoost) {
            this.boostValue = maxBoost;
        }
        this.disableKeyguardActive = pref.getBoolean(PREF_DISABLE_KEYGUARD, false);
        this.shape = pref.getBoolean(PREF_SHAPE, true);
        this.quietCamera = pref.getBoolean(PREF_QUIET_CAMERA, false);
        this.maximumBoostPercent = getMaximumBoost(pref);
        this.legacy = pref.getBoolean(PREF_LEGACY, false);
        Earpiece.log("max boost = " + this.maximumBoostPercent);
    }

    public void save(SharedPreferences pref) {
        Editor ed = pref.edit();
        ed.putBoolean(PREF_EARPIECE_ACTIVE, this.earpieceActive);
        ed.putBoolean(PREF_EQUALIZER_ACTIVE, this.equalizerActive);
        ed.putBoolean(PREF_AUTO_SPEAKER_PHONE, this.autoSpeakerPhoneActive);
        ed.putBoolean(PREF_PROXIMITY, this.proximity);
        ed.putBoolean(PREF_DISABLE_KEYGUARD, this.disableKeyguardActive);
        ed.putInt(PREF_BOOST, this.boostValue);
        ed.putBoolean(PREF_SHAPE, this.shape);
        ed.putBoolean(PREF_NOTIFY_LIGHT_ONLY_WHEN_OFF, this.notifyLightOnlyWhenOff);
        ed.putString(PREF_MAXIMUM_BOOST, this.maximumBoostPercent+"");
        ed.apply();
    }

    public void saveBoost(SharedPreferences pref) {
        Editor ed = pref.edit();
        ed.putInt(PREF_BOOST, this.boostValue);
        ed.apply();
    }

    public void setEarpiece() {
        setEarpiece(this.earpieceActive);
    }

    public void setEarpiece(boolean value) {
        if (haveTelephony() && this.audioManager != null) {
            try {
                this.audioManager.setSpeakerphoneOn(false);
                if (value) {
                    Earpiece.log("Earpiece mode on");
                    this.audioManager.setMode(this.legacy
                            ? AudioManager.MODE_IN_CALL
                            : AudioManager.MODE_IN_COMMUNICATION);
                    this.audioManager.setSpeakerphoneOn(false);
                    return;
                }
                Earpiece.log("Earpiece mode off\t");
                this.audioManager.setMode(AudioManager.MODE_NORMAL);
                this.audioManager.setRouting(0, 2, -1);
            } catch (RuntimeException exception) {
                Log.w("Settings", "Unable to apply audio routing", exception);
            }
        }
    }

    @SuppressLint({"NewApi"})
    public void setEqualizer(short v) {
        if (this.eq != null) {
            try {
                this.eq.setEnabled(v != (short) 0);
                if (v == (short) 0) {
                    Earpiece.log("no boost");
                    return;
                }
                for (short i = (short) 0; i < this.bands; i = (short) (i + 1)) {
                    short adj = v;
                    if (this.shape && v >= (short) 0) {
                        int hz = this.eq.getCenterFreq(i) / 1000;
                        if (hz < 150) {
                            adj = (short) 0;
                        } else if (hz < 250) {
                            adj = (short) (v / 2);
                        } else if (hz > 8000) {
                            adj = (short) ((v * 3) / 4);
                        }
                    }
                    this.eq.setBandLevel(i, adj);
                }
            } catch (RuntimeException exception) {
                Log.w("Settings", "Unable to apply equalizer settings", exception);
                releaseEqualizer();
            }
        }
    }

    public void setEqualizer() {
        Earpiece.log("setEqualizer " + this.boostValue);
        if (this.eq != null) {
            short v = (short) this.boostValue;
            if (v < (short) 0 || !this.equalizerActive) {
                v = (short) 0;
            }
            if (v > this.rangeHigh) {
                v = this.rangeHigh;
            }
            setEqualizer(v);
        }
    }

    public void setAll() {
        setEarpiece();
        setEqualizer();
    }

    public boolean haveEqualizer() {
        return this.eq != null;
    }

    public void disableEqualizer() {
        if (this.eq != null && !this.released) {
            try {
                Earpiece.log("Closing equalizer");
                this.eq.setEnabled(false);
            } catch (RuntimeException exception) {
                Log.w("Settings", "Unable to disable equalizer", exception);
                releaseEqualizer();
            }
        }
    }

    public void destroyEqualizer() {
        if (this.eq != null) {
            disableEqualizer();
            releaseEqualizer();
        }
    }

    private void releaseEqualizer() {
        if (this.eq != null) {
            try {
                this.eq.release();
            } catch (RuntimeException exception) {
                Log.w("Settings", "Unable to release equalizer", exception);
            } finally {
                this.eq = null;
                this.released = true;
            }
        }
    }

    public boolean haveProximity() {
        return this.proximitySensor != null;
    }

    public boolean isEqualizerActive() {
        return this.eq != null && this.equalizerActive;
    }

    public boolean isDisableKeyguardActive() {
        return this.disableKeyguardActive;
    }

    public boolean isProximityActive() {
        return haveProximity() && this.earpieceActive && this.proximity;
    }

    public boolean isAutoSpeakerPhoneActive() {
        return haveProximity() && this.autoSpeakerPhoneActive;
    }

    public boolean isQuietCameraActive() {
        return this.eq != null && this.quietCamera;
    }

    public boolean needService() {
        return isEqualizerActive() || isProximityActive() || this.notifyLightOnlyWhenOff || isAutoSpeakerPhoneActive() || isDisableKeyguardActive() || isQuietCameraActive();
    }

    public boolean somethingOn() {
        return this.earpieceActive || this.notifyLightOnlyWhenOff || isEqualizerActive() || isAutoSpeakerPhoneActive() || isDisableKeyguardActive() || isQuietCameraActive();
    }

    public String describe() {
        if (!somethingOn()) {
            return "Earpiece application is off";
        }
        String[] list = new String[7];
        int i = 0;
        int count = 0 + 1;
        if (this.earpieceActive) {

            list[0] = "earpiece";
            i = count;
        }
        if (isProximityActive()) {
            count = i + 1;
            list[i] = PREF_PROXIMITY;
            i = count;
        }
        if (isEqualizerActive()) {
            count = i + 1;
            list[i] = PREF_BOOST;
            i = count;
        }
        if (isAutoSpeakerPhoneActive()) {
            count = i + 1;
            list[i] = "auto speaker";
            i = count;
        }
        if (isDisableKeyguardActive()) {
            count = i + 1;
            list[i] = "no lock";
            i = count;
        }
        if (isQuietCameraActive()) {
            count = i + 1;
            list[i] = "quiet camera";
            i = count;
        }
        if (this.notifyLightOnlyWhenOff) {
            count = i + 1;
            list[i] = "notify LED";
            i = count;
        }
        String out = "";
        for (int i2 = 0; i2 < i; i2++) {
            out = new StringBuilder(String.valueOf(out)).append(list[i2]).toString();
            if (i2 + 1 < i) {
                out = new StringBuilder(String.valueOf(out)).append(", ").toString();
            }
        }
        return out;
    }

    public boolean haveTelephony() {
        return this.pm.hasSystemFeature("android.hardware.telephony");
    }

    public static boolean isKindle() {
        return Build.MODEL.equalsIgnoreCase("Kindle Fire");
    }

    @SuppressLint({"NewApi"})
    public boolean hasMenuKey() {
        if (isKindle() || VERSION.SDK_INT < 14) {
            return true;
        }
        return ViewConfiguration.get(this.context).hasPermanentMenuKey();
    }

    public boolean needScreenOnOffReceiver() {
        return this.notifyLightOnlyWhenOff;
    }
}
