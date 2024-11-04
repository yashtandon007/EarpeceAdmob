package com.earspeakervolumebooster.callvolumeincreaser;


import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.OnLifecycleEvent;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.UnityAds;

import java.util.Date;


/**
 * Created by Magic Lenz on 7/27/2017.
 */

public class MyApplication extends Application implements Application.ActivityLifecycleCallbacks {

    private static final String LOG_TAG = "AppOpenAdManager";

    public static final String ADMOB_INTERSTICIAL = "ca-app-pub-7742762973514262/3046023282";


    // test
  //  public static final String ADMOB_INTERSTICIAL = "ca-app-pub-3940256099942544/1033173712";

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
    private class AppOpenAdManager {

        /**
         * Keep track of the time an app open ad is loaded to ensure you don't show an expired ad.
         */
        private long loadTime = 0;

        // ...

        /**
         * Utility method to check if ad was loaded more than n hours ago.
         */
        private boolean wasLoadTimeLessThanNHoursAgo(long numHours) {
            long dateDifference = (new Date()).getTime() - this.loadTime;
            long numMilliSecondsPerHour = 3600000;
            return (dateDifference < (numMilliSecondsPerHour * numHours));
        }

        /**
         * Check if ad exists and can be shown.
         */
        public boolean isAdAvailable() {
            return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4);
        }


        private static final String AD_UNIT_ID = "ca-app-pub-7742762973514262/2016846793";

        // TEST ADD UNIT
     //   private static final String AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921";

        private AppOpenAd appOpenAd = null;
        private boolean isLoadingAd = false;
        private boolean isShowingAd = false;


        /**
         * LifecycleObserver method that shows the app open ad when the app moves to foreground.
         */
        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        protected void onMoveToForeground() {
            // Show the ad (if available) when the app moves to foreground.
            appOpenAdManager.showAdIfAvailable(currentActivity);
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
            // If the app open ad is already showing, do not show the ad again.
            if (isShowingAd) {
                Log.d(LOG_TAG, "The app open ad is already showing.");
                return;
            }

            // If the app open ad is not available yet, invoke the callback then load the ad.
            if (!isAdAvailable()) {
                Log.d(LOG_TAG, "The app open ad is not ready yet.");
                onShowAdCompleteListener.onShowAdComplete();
                loadAd(activity);
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
                            Log.d(LOG_TAG, adError.getMessage());
                            appOpenAd = null;
                            isShowingAd = false;

                            onShowAdCompleteListener.onShowAdComplete();
                            loadAd(activity);
                        }

                        @Override
                        public void onAdShowedFullScreenContent() {
                            // Called when fullscreen content is shown.
                            Log.d(LOG_TAG, "Ad showed fullscreen content.");
                        }
                    });
            isShowingAd = true;
            appOpenAd.show(activity);
        }
        // ...

        /**
         * Constructor.
         */
        public AppOpenAdManager() {
        }

        /**
         * Request an ad.
         */
        public void loadAd(Context context) {
            // Do not load ad if there is an unused ad or one is already loading.
            if (isLoadingAd || isAdAvailable()) {
                return;
            }

            isLoadingAd = true;
            AdRequest request = new AdRequest.Builder().build();
            AppOpenAd.load(
                    context, AD_UNIT_ID, request,
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
                            Log.d(LOG_TAG, loadAdError.getMessage());
                            isLoadingAd = false;
                        }
                    });
        }

    }


    String gameID = "5027483";
    Boolean TESTMODE = false;
    public static String adUnitId = "Interstitial_Android";


    @Override
    public void onCreate() {
        super.onCreate();
        this.registerActivityLifecycleCallbacks(this);
        Log.d(LOG_TAG, "onCreate");

        new Thread(
                () -> {
                    // Initialize the Google Mobile Ads SDK on a background thread.
                    MobileAds.initialize(this, initializationStatus -> {
                    });
                })
                .start();

        appOpenAdManager = new AppOpenAdManager();

//        UnityAds.initialize(this, gameID, TESTMODE, new IUnityAdsInitializationListener() {
//            @Override
//            public void onInitializationComplete() {
//                Log.e("unity", "onInitializationComplete");
//            }
//
//            @Override
//            public void onInitializationFailed(UnityAds.UnityAdsInitializationError unityAdsInitializationError, String s) {
//                Log.e("unity", "onInitializationFailed");
//
//            }
//        });


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
    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }
}

