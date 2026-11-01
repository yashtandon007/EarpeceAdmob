package com.earspeakervolumebooster.callvolumeincreaser;


import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import android.preference.PreferenceManager;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.ump.UserMessagingPlatform;

import java.util.Date;


/**
 * Created by Magic Lenz on 7/27/2017.
 */

public class MyApplication extends Application implements Application.ActivityLifecycleCallbacks {

    private static final String LOG_TAG = "AppOpenAdManager";

    private static final long APP_OPEN_AD_COOLDOWN_MILLIS = 60 * 60 * 1000L;
    private static final String PREF_APP_OPEN_AD_LAST_SHOWN = "lastAppOpenAdShown";
    static final String PREF_LAST_FULLSCREEN_AD_SHOWN = "lastFullscreenAdShown";
    private static final long MIN_FULLSCREEN_AD_INTERVAL_MILLIS = 15 * 60 * 1000L;

    /**
     * Interface definition for a callback to be invoked when an app open ad is complete.
     */
    public interface OnShowAdCompleteListener {
        void onShowAdComplete();
    }

    private Activity currentActivity;

    private AppOpenAdManager appOpenAdManager;

    // ...

    /**
     * Inner class that loads and shows app open ads.
     */
    private class AppOpenAdManager implements DefaultLifecycleObserver {

        /**
         * Keep track of the time an app open ad is loaded to ensure you don't show an expired ad.
         */
        private long loadTime = 0;

        /**
         * Constructor.
         */
        public AppOpenAdManager() {
            ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        }

        @Override
        public void onStart(@NonNull LifecycleOwner owner) {
            DefaultLifecycleObserver.super.onStart(owner);
            if (currentActivity != null && hasAdConsent()) {
                if (!isWithinAdCooldown() && !hasRecentFullscreenAd()) {
                    showAdIfAvailable(currentActivity);
                } else {
                    Log.d(LOG_TAG, "App-open ad skipped by frequency cap.");
                }
            }
        }

        private boolean isWithinAdCooldown() {
            long lastShown = PreferenceManager.getDefaultSharedPreferences(MyApplication.this)
                    .getLong(PREF_APP_OPEN_AD_LAST_SHOWN, 0);
            long timeSinceLastShown = System.currentTimeMillis() - lastShown;
            return lastShown > 0 && timeSinceLastShown >= 0
                    && timeSinceLastShown < APP_OPEN_AD_COOLDOWN_MILLIS;
        }

        private boolean hasRecentFullscreenAd() {
            long lastShown = PreferenceManager.getDefaultSharedPreferences(MyApplication.this)
                    .getLong(PREF_LAST_FULLSCREEN_AD_SHOWN, 0);
            long timeSinceLastShown = System.currentTimeMillis() - lastShown;
            return lastShown > 0 && timeSinceLastShown >= 0
                    && timeSinceLastShown < MIN_FULLSCREEN_AD_INTERVAL_MILLIS;
        }

        /**
         * Show the ad if one isn't already showing.
         */
        private void showAdIfAvailable(@NonNull final Activity activity) {
            showAdIfAvailable(
                    activity,
                    new OnShowAdCompleteListener() {
                        @Override
                        public void onShowAdComplete() {
                            // Empty because the user will go back to the activity that shows the ad.
                        }
                    });
        }


        /**
         * Shows the ad if one isn't already showing.
         */
        public void showAdIfAvailable(
                @NonNull final Activity activity,
                @NonNull OnShowAdCompleteListener onShowAdCompleteListener) {
            if (activity.isFinishing() || activity.isDestroyed()) {
                Log.d(LOG_TAG, "App-open ad skipped because activity is not usable.");
                onShowAdCompleteListener.onShowAdComplete();
                return;
            }
            // If the app open ad is already showing, do not show the ad again.
            if (isShowingAd) {
                Log.d(LOG_TAG, "The app open ad is already showing.");
                return;
            }

            // If the app open ad is not available yet, invoke the callback then load the ad.
            if (!isAdAvailable()) {
                Log.d(LOG_TAG, "The app open ad is not ready yet.");
                onShowAdCompleteListener.onShowAdComplete();
                loadAd(MyApplication.this);
                return;
            }

            appOpenAd.setFullScreenContentCallback(
                    new FullScreenContentCallback() {

                        @Override
                        public void onAdDismissedFullScreenContent() {
                            // Called when fullscreen content is dismissed.
                            // Set the reference to null so isAdAvailable() returns false.
                            Log.d(LOG_TAG, "Ad dismissed fullscreen content.");
                            appOpenAd = null;
                            isShowingAd = false;

                            onShowAdCompleteListener.onShowAdComplete();
                            loadAd(activity);
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(AdError adError) {
                            // Called when fullscreen content failed to show.
                            // Set the reference to null so isAdAvailable() returns false.
                            Log.w(LOG_TAG, "App-open ad failed to show: " + adError.getMessage());
                            appOpenAd = null;
                            isShowingAd = false;

                            onShowAdCompleteListener.onShowAdComplete();
                            loadAd(MyApplication.this);
                        }

                        @Override
                        public void onAdShowedFullScreenContent() {
                            // Called when fullscreen content is shown.
                            Log.d(LOG_TAG, "Ad showed fullscreen content.");
                            PreferenceManager.getDefaultSharedPreferences(MyApplication.this)
                                    .edit()
                                    .putLong(PREF_APP_OPEN_AD_LAST_SHOWN, System.currentTimeMillis())
                                    .putLong(PREF_LAST_FULLSCREEN_AD_SHOWN, System.currentTimeMillis())
                                    .apply();
                        }
                    });
            isShowingAd = true;
            try {
                appOpenAd.show(activity);
            } catch (RuntimeException exception) {
                Log.e(LOG_TAG, "App-open ad could not be shown.", exception);
                appOpenAd = null;
                isShowingAd = false;
                onShowAdCompleteListener.onShowAdComplete();
                loadAd(MyApplication.this);
            }
        }

        /**
         * Utility method to check if ad was loaded more than n hours ago.
         */
        private boolean wasLoadTimeLessThanNHoursAgo(long numHours) {
            long dateDifference = (new Date()).getTime() - this.loadTime;
            long numMilliSecondsPerHour = 3600000;
            return dateDifference >= 0 && dateDifference < (numMilliSecondsPerHour * numHours);
        }

        /**
         * Check if ad exists and can be shown.
         */
        public boolean isAdAvailable() {
            return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4);
        }

        private AppOpenAd appOpenAd = null;
        private boolean isLoadingAd = false;
        private boolean isShowingAd = false;

        /**
         * Request an ad.
         */
        public void loadAd(Context context) {
            // Do not load ad if there is an unused ad or one is already loading.
            if (!MyApplication.this.hasAdConsent() || isLoadingAd || isAdAvailable()) {
                return;
            }

            isLoadingAd = true;
            AdRequest request = new AdRequest.Builder().build();
            AppOpenAd.load(
                    context, getString(R.string.appopen), request,
                    AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                    new AppOpenAd.AppOpenAdLoadCallback() {
                        @Override
                        public void onAdLoaded(AppOpenAd ad) {
                            // Called when an app open ad has loaded.
                            Log.d(LOG_TAG, "Ad was loaded.");
                            appOpenAd = ad;
                            isLoadingAd = false;
                            loadTime = (new Date()).getTime();
                        }

                        @Override
                        public void onAdFailedToLoad(LoadAdError loadAdError) {
                            // Called when an app open ad has failed to load.
                            Log.w(LOG_TAG, "App-open ad failed to load: " + loadAdError.getMessage());
                            appOpenAd = null;
                            loadTime = 0;
                            isLoadingAd = false;
                        }
                    });
        }

    }

    private boolean hasAdConsent() {
        return UserMessagingPlatform.getConsentInformation(this).canRequestAds();
    }

    void loadAppOpenAd(Activity activity) {
        if (this.appOpenAdManager != null && hasAdConsent()) {
            this.appOpenAdManager.loadAd(this);
        }
    }

    boolean canShowInterstitial() {
        if (!hasAdConsent() || (this.appOpenAdManager != null
                && this.appOpenAdManager.isShowingAd)) {
            return false;
        }
        long lastShown = PreferenceManager.getDefaultSharedPreferences(this)
                .getLong(PREF_LAST_FULLSCREEN_AD_SHOWN, 0);
        long timeSinceLastShown = System.currentTimeMillis() - lastShown;
        return lastShown == 0 || timeSinceLastShown < 0
                || timeSinceLastShown >= MIN_FULLSCREEN_AD_INTERVAL_MILLIS;
    }

    boolean canRequestAds() {
        return hasAdConsent();
    }

    void recordFullscreenAdShown() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putLong(PREF_LAST_FULLSCREEN_AD_SHOWN, System.currentTimeMillis())
                .apply();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.registerActivityLifecycleCallbacks(this);
        Log.d(LOG_TAG, "onCreate");

        // Initialize the Google Mobile Ads SDK.
        try {
            MobileAds.initialize(this, initializationStatus -> {
            });
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to initialize MobileAds: " + e.getMessage());
        }

        appOpenAdManager = new AppOpenAdManager();
    }

    /**
     * ActivityLifecycleCallback methods.
     */
    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
        currentActivity = activity;
    }

    @Override
    public void onActivityResumed(Activity activity) {
    }

    @Override
    public void onActivityStopped(Activity activity) {
        if (currentActivity == activity) {
            currentActivity = null;
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        if (currentActivity == activity) {
            currentActivity = null;
        }
    }
}
