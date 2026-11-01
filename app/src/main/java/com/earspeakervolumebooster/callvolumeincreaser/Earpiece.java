package com.earspeakervolumebooster.callvolumeincreaser;

import static com.earspeakervolumebooster.callvolumeincreaser.Options.PREF_REMOVE_BOOST;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;
import com.suke.widget.SwitchButton;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


public class Earpiece extends Activity implements ServiceConnection {
    private static final String PREF_LAUNCH_COUNT = "launchCount";
    private static final String PREF_RATE_PROMPT_SHOWN = "ratePromptShown";
    private static final String PREF_ALT_THEME_UNLOCKED = "altThemeUnlocked";
    private static final String PREF_ALT_THEME_ACTIVE = "altThemeActive";
    private static final String PREF_BANNER_FREE_UNTIL = "bannerFreeUntil";
    private static final long BANNER_FREE_DURATION_MILLIS = 60 * 60 * 1000L;
    private static final int REQUEST_SETTINGS = 1;
    private static final boolean DEBUG = BuildConfig.DEBUG;
    static final int NOTIFICATION_ID = 1;
    private int SLIDER_MAX = 10000;
    private SeekBar boostBar;
    SwitchButton earpieceBox;
    SwitchButton equalizerBox;
    private View equalizerContainer;
    private View earpieceRow;
    private View proximityRow;
    private View equalizerRow;
    private Messenger messenger;
    private SharedPreferences options;
    SwitchButton proximityBox;
    private Settings settings;
    private AdView adView;
    private boolean serviceBound;
    private boolean bannerLoadRequested;
    private InterstitialAd interstitialAd;
    private boolean interstitialLoading;
    private NativeAd nativeAd;
    private RewardedAd rewardedAd;
    private boolean rewardedAdLoading;
    private RewardedInterstitialAd rewardedInterstitialAd;
    private boolean rewardedInterstitialAdLoading;
    private Button themeRewardButton;
    private Button bannerFreeRewardButton;
    private final Handler bannerRewardHandler = new Handler(Looper.getMainLooper());
    private boolean bannerFreeActive;
    private boolean activityResumed;
    private final Runnable bannerFreeExpiration = this::refreshBannerFreeState;

    class C00001 implements OnClickListener {
        C00001() {
        }

        public void onClick(DialogInterface dialog, int which) {
        }
    }

    class C00012 implements OnCancelListener {
        C00012() {
        }

        public void onCancel(DialogInterface dialog) {
        }
    }


    class C00056 implements SwitchButton.OnCheckedChangeListener {
        C00056() {
        }

        public void onCheckedChanged(SwitchButton button, boolean value) {
            Earpiece.this.settings.equalizerActive = value;
            Earpiece.this.settings.save(Earpiece.this.options);
            Earpiece.this.updateEqualizerDisplay();
            Earpiece.this.updateService();
        }
    }


    class C00067 implements OnSeekBarChangeListener {
        C00067() {
        }

        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                Earpiece.this.settings.boostValue = Earpiece.this.fromSlider(progress, 0, Earpiece.this.settings.rangeHigh);
                Earpiece.this.settings.save(Earpiece.this.options);
                Earpiece.this.reloadSettings();
            }
            Earpiece.this.updateBoostText(progress);
        }

        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    }


    class C00078 implements SwitchButton.OnCheckedChangeListener {
        C00078() {
        }


        @Override
        public void onCheckedChanged(SwitchButton switchButton, boolean value) {
            Earpiece.this.settings.earpieceActive = value;
            Earpiece.this.settings.save(Earpiece.this.options);
            Earpiece.this.settings.setEarpiece();
            Earpiece.this.proximityRow.setVisibility(
                    value && Earpiece.this.settings.haveProximity() ? View.VISIBLE : View.GONE);
            Earpiece.this.updateService();

        }
    }

    class C00089 implements SwitchButton.OnCheckedChangeListener {
        C00089() {
        }

        public void onCheckedChanged(SwitchButton button, boolean value) {
            Earpiece.this.settings.proximity = value;
            Earpiece.this.settings.save(Earpiece.this.options);
            Earpiece.log("proximity=" + value + " needService()" + Earpiece.this.settings.needService());
            Earpiece.this.updateService();
        }
    }

    public static void log(String s) {
        if (DEBUG) {
            Log.v("Earpiece", s);
        }
    }

    @SuppressLint({"NewApi"})
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(3);
        this.options = PreferenceManager.getDefaultSharedPreferences(this);
        this.options.edit()
                .putInt(PREF_LAUNCH_COUNT, this.options.getInt(PREF_LAUNCH_COUNT, 0) + 1)
                .apply();
        setContentView(R.layout.main);
        findViewById(R.id.settings_button).setOnClickListener(this::optionsClick);
        findViewById(R.id.share_button).setOnClickListener(this::shareApp);
        this.themeRewardButton = findViewById(R.id.theme_reward_button);
        this.themeRewardButton.setOnClickListener(this::onThemeRewardClicked);
        this.bannerFreeRewardButton = findViewById(R.id.banner_free_reward_button);
        this.bannerFreeRewardButton.setOnClickListener(this::onBannerFreeRewardClicked);
        FrameLayout bannerContainer = findViewById(R.id.banner_container);
        this.adView = new AdView(this);
        this.adView.setAdUnitId(getString(R.string.banner));
        float density = getResources().getDisplayMetrics().density;
        int adWidth = (int) (getResources().getDisplayMetrics().widthPixels / density);
        this.adView.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth));
        FrameLayout.LayoutParams bannerLayoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.CENTER);
        bannerContainer.addView(this.adView, bannerLayoutParams);
        applySystemBarInsets();
        refreshBannerFreeState();
        requestConsentAndAds();

        this.settings = new Settings(this, false);
        this.boostBar = (SeekBar) findViewById(R.id.boost);
        this.earpieceRow = findViewById(R.id.earpiece_row);
        this.earpieceBox = (SwitchButton) findViewById(R.id.earpiece);
        this.earpieceRow.setVisibility(
                this.settings.haveTelephony() ? View.VISIBLE : View.GONE);
        this.proximityRow = findViewById(R.id.proximity_row);
        this.proximityRow.setVisibility(
                this.settings.haveProximity() ? View.VISIBLE : View.GONE);
        this.proximityBox = (SwitchButton) findViewById(R.id.proximity);
        this.equalizerBox = (SwitchButton) findViewById(R.id.equalizer);
        this.equalizerRow = findViewById(R.id.equalizer_row);
        this.equalizerContainer = findViewById(R.id.equalizer_inside);
        applySavedTheme();

    }


    void market() {
        MarketDetector.launch(this);
    }


    private void message(String title, String msg) {
        AlertDialog alertDialog = new Builder(this).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(Html.fromHtml(msg));
        alertDialog.setButton(-1, "OK", new C00001());
        alertDialog.setOnCancelListener(new C00012());
        alertDialog.show();
    }

    private void show(String title, String filename) {
        message(title, getAssetFile(filename));
    }

    void updateService(boolean value) {
        if (value) {
            restartService(true);
            return;
        }
        stopService();
    }

    void updateService() {
        updateService(this.settings.needService());
    }

    private void updateBoostText(int progress) {
        ((TextView) findViewById(R.id.boost_value)).setText(getString(
                R.string.boost_value,
                (((progress * 100) + (this.SLIDER_MAX / 2)) / this.SLIDER_MAX)));
    }

    private void applySystemBarInsets() {
        View root = findViewById(R.id.main_root);
        int initialLeft = root.getPaddingLeft();
        int initialTop = root.getPaddingTop();
        int initialRight = root.getPaddingRight();
        int initialBottom = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(
                    initialLeft + systemBars.left,
                    initialTop + systemBars.top,
                    initialRight + systemBars.right,
                    initialBottom + systemBars.bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    private void requestConsentAndAds() {
        ConsentInformation consentInformation =
                UserMessagingPlatform.getConsentInformation(this);
        consentInformation.requestConsentInfoUpdate(
                this,
                new ConsentRequestParameters.Builder().build(),
                () -> UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                        this,
                        formError -> {
                            if (formError != null) {
                                Log.w("Earpiece", "Consent form error: " + formError.getMessage());
                            }
                            requestAdsIfAllowed(consentInformation);
                        }),
                requestError -> {
                    Log.w("Earpiece", "Consent info update error: " + requestError.getMessage());
                    requestAdsIfAllowed(consentInformation);
                });
        requestAdsIfAllowed(consentInformation);
    }

    private void requestAdsIfAllowed(ConsentInformation consentInformation) {
        if (!this.bannerLoadRequested && consentInformation.canRequestAds()) {
            this.bannerLoadRequested = true;
            try {
                this.adView.loadAd(new AdRequest.Builder().build());
            } catch (IllegalStateException exception) {
                Log.e("Earpiece", "Banner ad was not configured before load", exception);
                this.bannerLoadRequested = false;
            }
            MyApplication application = (MyApplication) getApplication();
            application.loadAppOpenAd(this);
            loadInterstitialAd();
            loadNativeAd();
            loadRewardedAd();
            loadRewardedInterstitialAd();
        }
    }

    private void loadNativeAd() {
        NativeAdView nativeAdView = findViewById(R.id.native_ad_view);
        String adUnitId = getString(R.string.native_ad_unit);
        if (adUnitId.contains("REPLACE_WITH_")) {
            Log.w("Earpiece", "Configure a production native ad unit ID to enable native ads");
            return;
        }
        new AdLoader.Builder(this, adUnitId)
                .forNativeAd(ad -> {
                    if (isDestroyed()) {
                        ad.destroy();
                        return;
                    }
                    if (this.nativeAd != null) {
                        this.nativeAd.destroy();
                    }
                    this.nativeAd = ad;
                    populateNativeAd(ad, nativeAdView);
                    nativeAdView.setVisibility(View.VISIBLE);
                    applySavedTheme();
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(@androidx.annotation.NonNull LoadAdError error) {
                        Log.w("Earpiece", "Native ad load failed: " + error.getMessage());
                    }
                })
                .build()
                .loadAd(new AdRequest.Builder().build());
    }

    private void populateNativeAd(NativeAd ad, NativeAdView nativeAdView) {
        TextView headline = nativeAdView.findViewById(R.id.native_ad_headline);
        headline.setText(ad.getHeadline());
        nativeAdView.setHeadlineView(headline);

        TextView body = nativeAdView.findViewById(R.id.native_ad_body);
        body.setText(ad.getBody());
        body.setVisibility(ad.getBody() == null ? View.GONE : View.VISIBLE);
        nativeAdView.setBodyView(body);

        TextView advertiser = nativeAdView.findViewById(R.id.native_ad_advertiser);
        advertiser.setText(ad.getAdvertiser());
        advertiser.setVisibility(ad.getAdvertiser() == null ? View.GONE : View.VISIBLE);
        nativeAdView.setAdvertiserView(advertiser);

        Button action = nativeAdView.findViewById(R.id.native_ad_action);
        action.setText(ad.getCallToAction() == null
                ? getString(R.string.native_ad_action) : ad.getCallToAction());
        action.setVisibility(ad.getCallToAction() == null ? View.GONE : View.VISIBLE);
        nativeAdView.setCallToActionView(action);

        ImageView icon = nativeAdView.findViewById(R.id.native_ad_icon);
        if (ad.getIcon() != null) {
            icon.setImageDrawable(ad.getIcon().getDrawable());
            icon.setVisibility(View.VISIBLE);
        } else {
            icon.setVisibility(View.GONE);
        }
        nativeAdView.setIconView(icon);

        MediaView media = nativeAdView.findViewById(R.id.native_ad_media);
        if (ad.getMediaContent() != null) {
            media.setMediaContent(ad.getMediaContent());
            media.setVisibility(View.VISIBLE);
        } else {
            media.setVisibility(View.GONE);
        }
        nativeAdView.setMediaView(media);
        nativeAdView.setNativeAd(ad);
    }

    private void loadRewardedAd() {
        if (this.rewardedAd != null || this.rewardedAdLoading) {
            return;
        }
        String adUnitId = getString(R.string.rewarded_ad_unit);
        if (adUnitId.contains("REPLACE_WITH_")) {
            this.themeRewardButton.setEnabled(false);
            Log.w("Earpiece", "Configure a production rewarded ad unit ID to enable rewarded ads");
            return;
        }
        this.rewardedAdLoading = true;
        RewardedAd.load(
                this,
                adUnitId,
                new AdRequest.Builder().build(),
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@androidx.annotation.NonNull RewardedAd ad) {
                        rewardedAd = ad;
                        rewardedAdLoading = false;
                        updateThemeRewardButton();
                    }

                    @Override
                    public void onAdFailedToLoad(@androidx.annotation.NonNull LoadAdError error) {
                        Log.w("Earpiece", "Rewarded ad load failed: " + error.getMessage());
                        rewardedAd = null;
                        rewardedAdLoading = false;
                        updateThemeRewardButton();
                    }
                });
    }

    private void loadRewardedInterstitialAd() {
        if (this.rewardedInterstitialAd != null || this.rewardedInterstitialAdLoading) {
            return;
        }
        String adUnitId = getString(R.string.rewarded_interstitial_ad_unit);
        if (adUnitId.contains("REPLACE_WITH_")) {
            Log.w("Earpiece", "Configure a production rewarded-interstitial unit ID to enable ads");
            return;
        }
        this.rewardedInterstitialAdLoading = true;
        RewardedInterstitialAd.load(
                this,
                adUnitId,
                new AdRequest.Builder().build(),
                new RewardedInterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(
                            @androidx.annotation.NonNull RewardedInterstitialAd ad) {
                        rewardedInterstitialAd = ad;
                        rewardedInterstitialAdLoading = false;
                        updateBannerFreeRewardButton();
                    }

                    @Override
                    public void onAdFailedToLoad(
                            @androidx.annotation.NonNull LoadAdError error) {
                        Log.w("Earpiece", "Rewarded-interstitial load failed: "
                                + error.getMessage());
                        rewardedInterstitialAd = null;
                        rewardedInterstitialAdLoading = false;
                        updateBannerFreeRewardButton();
                    }
                });
    }

    private void onThemeRewardClicked(View view) {
        if (this.options.getBoolean(PREF_ALT_THEME_UNLOCKED, false)) {
            boolean activate = !this.options.getBoolean(Options.PREF_DARK_THEME, false);
            this.options.edit()
                    .putBoolean(Options.PREF_DARK_THEME, activate)
                    .putBoolean(PREF_ALT_THEME_ACTIVE, false)
                    .apply();
            applySavedTheme();
            updateThemeRewardButton();
            return;
        }

        showThemeRewardedAd();
    }

    private void onBannerFreeRewardClicked(View view) {
        showBannerFreeRewardedInterstitial();
    }

    private void showThemeRewardedAd() {
        MyApplication application = (MyApplication) getApplication();
        if (!application.canRequestAds()) {
            Toast.makeText(this, R.string.theme_reward_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!application.canShowInterstitial()) {
            Toast.makeText(this, R.string.theme_reward_cooldown, Toast.LENGTH_SHORT).show();
            return;
        }
        if (this.rewardedAd == null) {
            loadRewardedAd();
            Toast.makeText(this, R.string.theme_reward_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }

        RewardedAd adToShow = this.rewardedAd;
        this.rewardedAd = null;
        adToShow.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                application.recordFullscreenAdShown();
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                loadRewardedAd();
                updateThemeRewardButton();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(
                    @androidx.annotation.NonNull com.google.android.gms.ads.AdError error) {
                Log.w("Earpiece", "Rewarded ad show failed: " + error.getMessage());
                loadRewardedAd();
                updateThemeRewardButton();
            }
        });
        adToShow.show(this, rewardItem -> {
            this.options.edit()
                    .putBoolean(PREF_ALT_THEME_UNLOCKED, true)
                    .putBoolean(PREF_ALT_THEME_ACTIVE, true)
                    .putBoolean(Options.PREF_DARK_THEME, true)
                    .apply();
            applySavedTheme();
            updateThemeRewardButton();
            Toast.makeText(this, R.string.theme_reward_unlocked, Toast.LENGTH_SHORT).show();
        });
    }

    private void showBannerFreeRewardedInterstitial() {
        MyApplication application = (MyApplication) getApplication();
        if (!application.canRequestAds()) {
            Toast.makeText(this, R.string.theme_reward_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!application.canShowInterstitial()) {
            Toast.makeText(this, R.string.theme_reward_cooldown, Toast.LENGTH_SHORT).show();
            return;
        }
        if (this.rewardedInterstitialAd == null) {
            loadRewardedInterstitialAd();
            Toast.makeText(this, R.string.theme_reward_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }

        RewardedInterstitialAd adToShow = this.rewardedInterstitialAd;
        this.rewardedInterstitialAd = null;
        adToShow.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                application.recordFullscreenAdShown();
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                loadRewardedInterstitialAd();
                updateBannerFreeRewardButton();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(
                    @androidx.annotation.NonNull com.google.android.gms.ads.AdError error) {
                Log.w("Earpiece", "Rewarded-interstitial show failed: " + error.getMessage());
                loadRewardedInterstitialAd();
                updateBannerFreeRewardButton();
            }
        });
        adToShow.show(this, rewardItem -> {
            long now = System.currentTimeMillis();
            long currentExpiry = this.options.getLong(PREF_BANNER_FREE_UNTIL, 0);
            long startsAt = Math.max(now, currentExpiry);
            long newExpiry = startsAt > Long.MAX_VALUE - BANNER_FREE_DURATION_MILLIS
                    ? Long.MAX_VALUE : startsAt + BANNER_FREE_DURATION_MILLIS;
            this.options.edit().putLong(PREF_BANNER_FREE_UNTIL, newExpiry).apply();
            refreshBannerFreeState();
            Toast.makeText(this, R.string.banner_free_reward_granted, Toast.LENGTH_SHORT).show();
        });
    }

    private void updateThemeRewardButton() {
        boolean unlocked = this.options.getBoolean(PREF_ALT_THEME_UNLOCKED, false);
        String rewardedUnitId = getString(R.string.rewarded_ad_unit);
        boolean configured = !rewardedUnitId.contains("REPLACE_WITH_");
        this.themeRewardButton.setEnabled(unlocked || (configured && !this.rewardedAdLoading));
        if (!unlocked) {
            this.themeRewardButton.setText(this.rewardedAd == null
                    ? R.string.theme_reward_unavailable_button : R.string.theme_reward_watch);
        } else {
            this.themeRewardButton.setText(this.options.getBoolean(PREF_ALT_THEME_ACTIVE, false)
                    ? R.string.theme_reward_switch_back : R.string.theme_reward_switch);
        }
        updateBannerFreeRewardButton();
    }

    private void updateBannerFreeRewardButton() {
        if (this.bannerFreeRewardButton == null || this.options == null) {
            return;
        }
        long remaining = this.options.getLong(PREF_BANNER_FREE_UNTIL, 0)
                - System.currentTimeMillis();
        if (remaining > 0) {
            this.bannerFreeRewardButton.setText(getString(
                    R.string.banner_free_reward_active,
                    Math.max(1, (int) Math.ceil(remaining / 60000.0))));
        } else {
            this.bannerFreeRewardButton.setText(R.string.banner_free_reward_watch);
        }
        boolean configured = !getString(R.string.rewarded_interstitial_ad_unit)
                .contains("REPLACE_WITH_");
        this.bannerFreeRewardButton.setEnabled(
                configured && !this.rewardedInterstitialAdLoading);
        if (remaining > 0) {
            this.bannerFreeRewardButton.setText(getString(
                    R.string.banner_free_reward_active,
                    Math.max(1, (int) Math.ceil(remaining / 60000.0))));
        } else if (this.rewardedInterstitialAd == null) {
            this.bannerFreeRewardButton.setText(R.string.banner_free_reward_unavailable);
        } else {
            this.bannerFreeRewardButton.setText(R.string.banner_free_reward_watch);
        }
    }

    private void refreshBannerFreeState() {
        if (this.options == null || this.adView == null) {
            return;
        }
        this.bannerRewardHandler.removeCallbacks(this.bannerFreeExpiration);
        long remaining = this.options.getLong(PREF_BANNER_FREE_UNTIL, 0)
                - System.currentTimeMillis();
        boolean shouldHideBanner = remaining > 0;
        if (this.bannerFreeActive != shouldHideBanner) {
            this.bannerFreeActive = shouldHideBanner;
            View bannerContainer = findViewById(R.id.banner_container);
            bannerContainer.setVisibility(shouldHideBanner ? View.GONE : View.VISIBLE);
            if (shouldHideBanner) {
                this.adView.pause();
            } else if (this.activityResumed) {
                this.adView.resume();
                if (this.bannerLoadRequested) {
                    this.adView.loadAd(new AdRequest.Builder().build());
                }
            }
        }
        if (shouldHideBanner) {
            this.bannerRewardHandler.postDelayed(
                    this.bannerFreeExpiration, Math.max(1, remaining));
        }
        updateBannerFreeRewardButton();
    }

    private void applySavedTheme() {
        boolean alternate = this.options.getBoolean(Options.PREF_DARK_THEME, false)
                || this.options.getBoolean(PREF_ALT_THEME_ACTIVE, false)
                || Options.isDarkTheme(this);
        int backgroundColor = getResources().getColor(alternate ? R.color.alt_bg : R.color.bg);
        int surfaceColor = getResources().getColor(alternate ? R.color.alt_surface : R.color.surface);
        int primaryTextColor = getResources().getColor(
                alternate ? R.color.alt_text_primary : R.color.text_primary);
        int secondaryTextColor = getResources().getColor(
                alternate ? R.color.alt_text_secondary : R.color.text_secondary);
        int outlineColor = getResources().getColor(alternate ? R.color.alt_outline : R.color.outline);
        findViewById(R.id.main_root).setBackgroundColor(backgroundColor);
        getWindow().setStatusBarColor(getResources().getColor(
                alternate ? R.color.alt_bg : R.color.colorPrimaryDark));
        getWindow().setNavigationBarColor(getResources().getColor(
                alternate ? R.color.alt_bg : R.color.colorPrimaryDark));
        View bannerContainer = findViewById(R.id.banner_container);
        if (bannerContainer != null) {
            bannerContainer.setBackgroundColor(surfaceColor);
        }
        int[] cardIds = {R.id.earpiece_row, R.id.proximity_row, R.id.equalizer_row,
                R.id.equalizer_inside, R.id.theme_reward_card, R.id.native_ad_view};
        for (int id : cardIds) {
            View card = findViewById(id);
            if (card != null) {
                GradientDrawable background = new GradientDrawable();
                background.setColor(surfaceColor);
                background.setCornerRadius(18 * getResources().getDisplayMetrics().density);
                background.setStroke(
                        Math.max(1, (int) getResources().getDisplayMetrics().density), outlineColor);
                card.setBackground(background);
            }
        }
        styleTextViews(findViewById(R.id.main_content), primaryTextColor, secondaryTextColor);
        styleThemeButton(findViewById(R.id.settings_button), surfaceColor, outlineColor, primaryTextColor);
        styleThemeButton(findViewById(R.id.share_button), surfaceColor, outlineColor, primaryTextColor);
        styleThemeButton(this.themeRewardButton, surfaceColor, outlineColor, primaryTextColor);
        styleThemeButton(this.bannerFreeRewardButton, surfaceColor, outlineColor, primaryTextColor);
        NativeAdView nativeView = findViewById(R.id.native_ad_view);
        styleThemeButton(nativeView.findViewById(R.id.native_ad_action),
                surfaceColor, outlineColor, primaryTextColor);
        TextView warning = findViewById(R.id.warn);
        GradientDrawable warningBackground = new GradientDrawable();
        warningBackground.setColor(getResources().getColor(
                alternate ? R.color.alt_warning_surface : R.color.warning_surface));
        warningBackground.setCornerRadius(12 * getResources().getDisplayMetrics().density);
        warning.setBackground(warningBackground);
        warning.setTextColor(getResources().getColor(
                alternate ? R.color.alt_warning_text : R.color.warning_text));
        updateThemeRewardButton();
    }

    private void styleTextViews(View view, int primaryColor, int secondaryColor) {
        if (view instanceof TextView) {
            int id = view.getId();
            boolean warning = id == R.id.warn || id == R.id.native_ad_label;
            boolean secondary = id == R.id.app_tagline
                    || id == R.id.native_ad_body
                    || id == R.id.native_ad_advertiser;
            if (!warning) {
                ((TextView) view).setTextColor(secondary ? secondaryColor : primaryColor);
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                styleTextViews(group.getChildAt(index), primaryColor, secondaryColor);
            }
        }
    }

    private void styleThemeButton(Button button, int surface, int outline, int textColor) {
        if (button == null) {
            return;
        }
        GradientDrawable background = new GradientDrawable();
        background.setColor(surface);
        background.setCornerRadius(14 * getResources().getDisplayMetrics().density);
        background.setStroke(Math.max(1, (int) getResources().getDisplayMetrics().density), outline);
        button.setBackground(background);
        button.setTextColor(textColor);
    }

    private void loadInterstitialAd() {
        if (this.interstitialAd != null || this.interstitialLoading
                || !((MyApplication) getApplication()).canRequestAds()) {
            return;
        }
        this.interstitialLoading = true;
        InterstitialAd.load(
                this,
                getString(R.string.interstitial),
                new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@androidx.annotation.NonNull InterstitialAd ad) {
                        interstitialAd = ad;
                        interstitialLoading = false;
                    }

                    @Override
                    public void onAdFailedToLoad(@androidx.annotation.NonNull LoadAdError error) {
                        Log.w("Earpiece", "Interstitial load failed: " + error.getMessage());
                        interstitialAd = null;
                        interstitialLoading = false;
                    }
                });
    }

    private void showInterstitialAfterSettings() {
        showInterstitialAtEligibleBreak();
    }

    private void showInterstitialAtEligibleBreak() {
        MyApplication application = (MyApplication) getApplication();
        if (!application.canShowInterstitial() || isFinishing() || isDestroyed()) {
            return;
        }
        if (this.interstitialAd == null) {
            loadInterstitialAd();
            return;
        }

        InterstitialAd adToShow = this.interstitialAd;
        this.interstitialAd = null;
        adToShow.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                application.recordFullscreenAdShown();
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                loadInterstitialAd();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(
                    @androidx.annotation.NonNull com.google.android.gms.ads.AdError error) {
                Log.w("Earpiece", "Interstitial show failed: " + error.getMessage());
                loadInterstitialAd();
            }
        });
        adToShow.show(this);
    }

    private void updateEqualizerDisplay() {
        if (this.settings.isEqualizerActive()) {
            this.equalizerContainer.setVisibility(View.VISIBLE);
        } else {
            this.equalizerContainer.setVisibility(View.GONE);
        }
    }

    void setupEqualizer() {
        log("setupEqualizer");
        if (this.settings.haveEqualizer() && !this.options.getBoolean(PREF_REMOVE_BOOST, false)) {
            this.equalizerRow.setVisibility(View.VISIBLE);
            this.equalizerBox.setChecked(this.settings.equalizerActive);
            this.equalizerBox.setOnCheckedChangeListener(new C00056());
            this.boostBar.setOnSeekBarChangeListener(new C00067());
            int progress = toSlider(this.settings.boostValue, 0, this.settings.rangeHigh);
            this.boostBar.setProgress(progress);
            updateEqualizerDisplay();
            updateBoostText(progress);
            return;
        }
        log("no equalizer");
        this.equalizerRow.setVisibility(View.GONE);
    }

    protected void reloadSettings() {
        sendMessage(2, 0, 0);
    }

    private int fromSlider(int value, int min, int max) {
        return ((((this.SLIDER_MAX - value) * min) + (max * value)) + (this.SLIDER_MAX / 2)) / this.SLIDER_MAX;
    }

    private int toSlider(int value, int min, int max) {
        if (max <= min) {
            return 0;
        }
        return (((value - min) * this.SLIDER_MAX) + ((max - min) / 2)) / (max - min);
    }


    public void onStart() {
        super.onStart();
        log("onStart()");
        this.settings.load(this.options);
        if (this.options.getBoolean(PREF_REMOVE_BOOST, false)) {
            log("no boost");
            this.settings.boostValue = 0;
            this.settings.saveBoost(this.options);
            this.settings.disableEqualizer();
            this.boostBar.setVisibility(View.GONE);
            updateEqualizerDisplay();
        } else {
            updateEqualizerDisplay();
        }
        this.boostBar.setMax((this.SLIDER_MAX * this.settings.maximumBoostPercent) / 100);
        if (this.settings.boostValue > (this.settings.rangeHigh * this.settings.maximumBoostPercent) / 100) {
            this.settings.boostValue = (this.settings.rangeHigh * this.settings.maximumBoostPercent) / 100;
            this.settings.save(this.options);
        }
        this.settings.setEarpiece();
        setupEqualizer();
        updateService();
        this.earpieceBox.setChecked(this.settings.earpieceActive);
        this.earpieceBox.setOnCheckedChangeListener(new C00078());
        this.proximityBox.setChecked(this.settings.proximity);
        this.proximityBox.setOnCheckedChangeListener(new C00089());
        if (this.settings.earpieceActive && this.settings.haveProximity()) {
            this.proximityRow.setVisibility(View.VISIBLE);
            return;
        }
        log("hide proximity box");
        this.proximityRow.setVisibility(View.GONE);
    }


    @Override
    protected void onResume() {
        super.onResume();
        this.activityResumed = true;
        if (this.adView != null) {
            this.adView.resume();
        }
        refreshBannerFreeState();
    }

    public void onPause() {
        this.activityResumed = false;
        if (this.adView != null) {
            this.adView.pause();
        }
        super.onPause();
        if (this.serviceBound) {
            log("unbind");
            unbindService(this);
            this.serviceBound = false;
            this.messenger = null;
        }

    }

    @Override
    protected void onDestroy() {
        this.bannerRewardHandler.removeCallbacks(this.bannerFreeExpiration);
        if (this.adView != null) {
            this.adView.destroy();
            this.adView = null;
        }
        if (this.nativeAd != null) {
            this.nativeAd.destroy();
            this.nativeAd = null;
        }
        this.rewardedAd = null;
        this.rewardedInterstitialAd = null;
        super.onDestroy();
    }


    void stopService() {
        log("stop service");
        if (this.serviceBound) {
            unbindService(this);
            this.serviceBound = false;
            this.messenger = null;
        }
        stopService(new Intent(this, EarpieceService.class));
    }

    void bind() {
        log("bind");
        this.serviceBound = bindService(new Intent(this, EarpieceService.class), this, 0);
        if (!this.serviceBound) {
            Log.e("Earpiece", "Failed to bind to EarpieceService");
        }
    }

    void restartService(boolean bind) {
        stopService();
        log("starting service");
        try {
            startService(new Intent(this, EarpieceService.class));
        } catch (IllegalStateException exception) {
            Log.e("Earpiece", "Service start rejected; reopen the app to restore audio features", exception);
            return;
        }
        if (bind) {
            bind();
        }
    }

    public void sendMessage(int n, int arg1, int arg2) {
        if (this.messenger != null) {
            try {
                log("message " + n + " " + arg1 + " " + arg2);
                this.messenger.send(Message.obtain(null, n, arg1, arg2));
            } catch (RemoteException e) {
                Log.w("Earpiece", "Service disconnected before settings update", e);
            }
        }
    }

    public void onServiceConnected(ComponentName classname, IBinder service) {
        log("connected");
        this.messenger = new Messenger(service);
    }

    public void onServiceDisconnected(ComponentName name) {
        log("disconnected");
        this.messenger = null;
    }

    private static String getStreamFile(InputStream stream) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String text = "";
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    return text;
                }
                text = new StringBuilder(String.valueOf(text)).append(line).toString();
            }
        } catch (IOException e) {
            return "";
        }
    }

    public String getAssetFile(String assetName) {
        try {
            return getStreamFile(getAssets().open(assetName));
        } catch (IOException e) {
            return "";
        }
    }

    public void optionsClick(View v) {
        startActivityForResult(new Intent(this, Options.class), REQUEST_SETTINGS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SETTINGS) {
            applySavedTheme();
            if (resultCode == RESULT_OK) {
                showInterstitialAfterSettings();
            }
        }
    }

    public void shareApp(View view) {
        int dialogTheme = isDarkTheme()
                ? R.style.ThemeOverlay_Earpiece_Dialog_Dark
                : R.style.ThemeOverlay_Earpiece_Dialog_Light;
        new AlertDialog.Builder(this, dialogTheme)
                .setTitle(R.string.share_dialog_title)
                .setMessage(R.string.share_dialog_message)
                .setNegativeButton(R.string.rate_not_now, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.share_dialog_action,
                        (dialog, which) -> launchShareChooser())
                .show();
    }

    private boolean isDarkTheme() {
        return Options.isDarkTheme(this);
    }

    private void launchShareChooser() {
        String appLink = "https://play.google.com/store/apps/details?id=" + getPackageName();
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_message, appLink));
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_app)));
    }

    @Override
    public void onBackPressed() {
        if (this.options.getInt(PREF_LAUNCH_COUNT, 0) >= 3
                && !this.options.getBoolean(PREF_RATE_PROMPT_SHOWN, false)) {
            this.options.edit().putBoolean(PREF_RATE_PROMPT_SHOWN, true).apply();
            CustumDialog_Rate rateDialog = new CustumDialog_Rate(this);
            rateDialog.show();
            return;
        }
        super.onBackPressed();

    }

}
