package com.earspeakervolumebooster.callvolumeincreaser;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.preference.PreferenceActivity;
import android.preference.ListPreference;
import android.preference.Preference;
import android.util.Log;
import android.widget.Toast;
import android.text.TextUtils;

import android.preference.CheckBoxPreference;
import android.preference.PreferenceManager;

import com.google.android.ump.ConsentInformation;
import com.google.android.ump.UserMessagingPlatform;

public class Options extends PreferenceActivity {
    private static final int REQUEST_PHONE_STATE_PERMISSION = 1001;
    public static final int NOTIFY_ALWAYS = 2;
    public static final int NOTIFY_AUTO = 1;
    public static final int NOTIFY_NEVER = 0;
    public static final String PREF_AUTO_SPEAKER_PHONE = "autoSpeakerPhone";
    public static final String PREF_BOOST = "boost";
    public static final String PREF_DISABLE_KEYGUARD = "disableKeyguard";
    public static final String PREF_DARK_THEME = "darkTheme";
    public static final String PREF_EARPIECE_ACTIVE = "earpieceActive";
    public static final String PREF_EQUALIZER_ACTIVE = "equalizerActive";
    public static final String PREF_FIRST_TIME = "firstTime";
    public static final String PREF_LAST_VERSION = "lastVersion1";
    public static final String PREF_LEGACY = "legacy";
    public static final String PREF_MAXIMUM_BOOST = "maximumBoostPerc2";
    public static final String PREF_MAXIMUM_BOOST_OLD = "maximumBoostPerc";
    public static final String PREF_NOTIFY = "notification";
    public static final String PREF_NOTIFY_LIGHT_ONLY_WHEN_OFF = "notifyLightOnlyWhenOff";
    public static final String PREF_PROXIMITY = "proximity";
    public static final String PREF_QUIET_CAMERA = "quietCamera";
    public static final String PREF_REMOVE_BOOST = "removeBoost";
    public static final String PREF_SHAPE = "shape";
    public static final String PREF_WARNED_LAST_VERSION = "warnedLastVersion";

    static boolean isDarkTheme(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean(PREF_DARK_THEME, false)
                || preferences.getBoolean("altThemeActive", false)
                || (context.getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    public void onCreate(Bundle icicle) {
        if (PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(PREF_DARK_THEME, false)) {
            setTheme(R.style.Theme_EarpieceSpeakerSettings_Dark);
        }
        super.onCreate(icicle);
        PreferenceManager.setDefaultValues(this, R.xml.options, false);
        addPreferencesFromResource(R.xml.options);
        ensureListPreferenceValue(
                (ListPreference) findPreference(PREF_NOTIFY),
                isKindle() ? "2" : "1");
        ensureListPreferenceValue(
                (ListPreference) findPreference(PREF_MAXIMUM_BOOST),
                "60");
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
        Preference privacyOptions = findPreference("privacyOptions");
        privacyOptions.setOnPreferenceClickListener(preference -> {
            ConsentInformation consentInformation =
                    UserMessagingPlatform.getConsentInformation(this);
            if (consentInformation.getPrivacyOptionsRequirementStatus()
                    == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED) {
                UserMessagingPlatform.showPrivacyOptionsForm(this, formError -> {
                    if (formError != null) {
                        Log.w("Options", "Privacy options form error: " + formError.getMessage());
                    }
                });
            } else {
                Toast.makeText(this, R.string.privacy_options_unavailable, Toast.LENGTH_SHORT).show();
            }
            return true;
        });
        findPreference(PREF_AUTO_SPEAKER_PHONE).setOnPreferenceChangeListener((preference, newValue) -> {
            if (Boolean.TRUE.equals(newValue)
                    && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    && checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{Manifest.permission.READ_PHONE_STATE},
                        REQUEST_PHONE_STATE_PERMISSION);
                return false;
            }
            return true;
        });
    }

    private static void ensureListPreferenceValue(
            ListPreference preference, String fallbackValue) {
        if (preference == null) {
            throw new IllegalStateException("Required list preference is missing");
        }
        String currentValue = preference.getValue();
        CharSequence[] entryValues = preference.getEntryValues();
        CharSequence[] validValues = new CharSequence[entryValues.length];
        for (int i = 0; i < entryValues.length; i++) {
            CharSequence entryValue = entryValues[i];
            validValues[i] = entryValue == null ? "" : entryValue;
        }
        preference.setEntryValues(validValues);
        if (currentValue != null) {
            for (CharSequence entryValue : validValues) {
                if (entryValue != null && TextUtils.equals(currentValue, entryValue)) {
                    return;
                }
            }
        }
        Log.w("Options", "Replacing missing or invalid preference value for "
                + preference.getKey());
        preference.setValue(fallbackValue);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PHONE_STATE_PERMISSION
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            ((CheckBoxPreference) findPreference(PREF_AUTO_SPEAKER_PHONE)).setChecked(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finishSettings();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        finishSettings();
    }

    private void finishSettings() {
        setResult(RESULT_OK);
        finish();
    }

    public void onResume() {
        super.onResume();
    }

    public void onStop() {
        super.onStop();
    }

    private static boolean isKindle() {
        return Build.MODEL.equalsIgnoreCase("Kindle Fire");
    }

    public static int getNotify(SharedPreferences options) {
        try {
            return parseNotify(options.getString(PREF_NOTIFY, isKindle() ? "2" : "1"), isKindle());
        } catch (NumberFormatException | ClassCastException e) {
            return isKindle() ? NOTIFY_ALWAYS : NOTIFY_AUTO;
        }
    }

    static int parseNotify(String value, boolean kindle) {
        try {
            int notify = Integer.parseInt(value);
            return Math.max(NOTIFY_NEVER, Math.min(NOTIFY_ALWAYS, notify));
        } catch (NumberFormatException e) {
            return kindle ? NOTIFY_ALWAYS : NOTIFY_AUTO;
        }
    }

    public static int getMaximumBoost(SharedPreferences options) {
        try {
            int old = Integer.parseInt(options.getString(PREF_MAXIMUM_BOOST_OLD, "-1"));
            if (old < 0) {
                int current = Integer.parseInt(options.getString(PREF_MAXIMUM_BOOST, "60"));
                return Math.max(0, Math.min(60, current));
            }
            options.edit().putString(PREF_MAXIMUM_BOOST_OLD, "-1").apply();
            if (old >= 60) {
                return 60;
            }
            return Math.max(0, old);
        } catch (NumberFormatException | ClassCastException e) {
            return 60;
        }
    }
}
