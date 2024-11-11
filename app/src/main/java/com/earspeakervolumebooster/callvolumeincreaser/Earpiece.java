package com.earspeakervolumebooster.callvolumeincreaser;

import static com.earspeakervolumebooster.callvolumeincreaser.MyApplication.adUnitId;
import static com.earspeakervolumebooster.callvolumeincreaser.Options.PREF_BOOST;
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
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.suke.widget.SwitchButton;
import com.unity3d.ads.IUnityAdsLoadListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.UnityAdsShowOptions;
import com.unity3d.services.banners.BannerView;
import com.unity3d.services.banners.UnityBannerSize;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


public class Earpiece extends Activity implements ServiceConnection {
    private static boolean DEBUG = true;
    static final int NOTIFICATION_ID = 1;
    private int SLIDER_MAX = 10000;
    private SeekBar boostBar;
    SwitchButton earpieceBox;
    SwitchButton equalizerBox;
    private View equalizerContainer;
    private LinearLayout main;
    private Messenger messenger;
    private SharedPreferences options;
    SwitchButton proximityBox;
    private Settings settings;
    private int versionCode;

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


    class C00034 implements OnClickListener {
        C00034() {
        }

        public void onClick(DialogInterface dialog, int which) {
            Earpiece.this.finish();
        }
    }

    class C00045 implements OnCancelListener {
        C00045() {
        }

        public void onCancel(DialogInterface dialog) {
            Earpiece.this.finish();
        }
    }

    class C00056 implements SwitchButton.OnCheckedChangeListener {
        C00056() {
        }

        public void onCheckedChanged(SwitchButton button, boolean value) {
            loadAdd();

            Earpiece.this.settings.equalizerActive = value;
            Earpiece.this.settings.save(Earpiece.this.options);
            Earpiece.this.updateEqualizerDisplay();
            Earpiece.this.updateService();
        }
    }


    private void loadAdd() {
        Log.e("yash", " loadAdd");
    //    showLoadAdMob();
        //  loadUnityAdd();

    }

    private InterstitialAd mInterstitialAd;

    private void showLoadAdMob() {

        if (mInterstitialAd != null) {
            mInterstitialAd.show(Earpiece.this);
        } else {
            Log.d("TAG", "The interstitial ad wasn't ready yet.");
        }

        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(this, MyApplication.ADMOB_INTERSTICIAL, adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        mInterstitialAd = interstitialAd;
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        mInterstitialAd = null;
                    }
                });
    }


    private void loadUnityAdd() {
        UnityAds.load(adUnitId, new IUnityAdsLoadListener() {
            @Override
            public void onUnityAdsAdLoaded(String placementId) {
                UnityAds.show(Earpiece.this, adUnitId, new UnityAdsShowOptions(), null);
            }

            @Override
            public void onUnityAdsFailedToLoad(String placementId, UnityAds.UnityAdsLoadError error, String message) {
                Log.e("UnityAdsExample", "Unity Ads failed to load ad for " + placementId + " with error: [" + error + "] " + message);
            }
        });
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
            Log.e("ch", "checked_chnges");
            loadAdd();
            Earpiece.this.settings.earpieceActive = value;
            Earpiece.this.settings.save(Earpiece.this.options);
            Earpiece.this.settings.setEarpiece();
            if (value && Earpiece.this.settings.haveProximity()) {
                Earpiece.this.proximityBox.setVisibility(0);
            } else {
                Earpiece.this.proximityBox.setVisibility(4);
            }
            Earpiece.this.updateService();

        }
    }

    class C00089 implements SwitchButton.OnCheckedChangeListener {
        C00089() {
        }

        public void onCheckedChanged(SwitchButton button, boolean value) {
            loadAdd();
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
        int i;
        int i2 = 8;
        super.onCreate(savedInstanceState);
        try {
            this.versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (NameNotFoundException e) {
            this.versionCode = 0;
        }
        setVolumeControlStream(3);
        this.main = (LinearLayout) getLayoutInflater().inflate(R.layout.main, null);
        setContentView(this.main);
        FrameLayout frameLayout = findViewById(R.id.frameBanner);
        BannerView banner = new BannerView(this, "Banner_Android", new UnityBannerSize(320, 50));
        banner.load();
        frameLayout.addView(banner);

        loadAdd();
        this.options = PreferenceManager.getDefaultSharedPreferences(this);
        this.settings = new Settings(this, false);
        this.boostBar = (SeekBar) findViewById(R.id.boost);
        this.earpieceBox = (SwitchButton) findViewById(R.id.earpiece);
        SwitchButton SwitchButton = this.earpieceBox;
        if (this.settings.haveTelephony()) {
            i = 0;
        } else {
            i = 8;
        }
        SwitchButton.setVisibility(i);
        this.proximityBox = (SwitchButton) findViewById(R.id.proximity);
        SwitchButton checkBox2 = this.proximityBox;
        if (this.settings.haveProximity()) {
            i2 = 0;
        }
        checkBox2.setVisibility(i2);
        this.equalizerBox = (SwitchButton) findViewById(R.id.equalizer);
        this.equalizerContainer = findViewById(R.id.equalizer_inside);


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
        ((TextView) findViewById(R.id.boost_value)).setText("Boost: " + (((progress * 100) + (this.SLIDER_MAX / 2)) / this.SLIDER_MAX) + "%");
    }

    private void updateEqualizerDisplay() {
        if (this.settings.isEqualizerActive()) {
            this.equalizerContainer.setVisibility(0);
        } else {
            this.equalizerContainer.setVisibility(8);
        }
    }

    void setupEqualizer() {
        log("setupEqualizer");
        if (this.settings.haveEqualizer()) {
            this.equalizerBox.setVisibility(0);
            this.equalizerBox.setOnCheckedChangeListener(new C00056());
            this.equalizerBox.setChecked(this.settings.equalizerActive);
            this.boostBar.setOnSeekBarChangeListener(new C00067());
            int progress = toSlider(this.options.getInt(PREF_BOOST, 0), 0, this.settings.rangeHigh);
            this.boostBar.setProgress(progress);
            updateEqualizerDisplay();
            updateBoostText(progress);
            return;
        }
        log("no equalizer");
        this.equalizerBox.setVisibility(8);
    }

    protected void reloadSettings() {
        sendMessage(2, 0, 0);
    }

    private int fromSlider(int value, int min, int max) {
        return ((((this.SLIDER_MAX - value) * min) + (max * value)) + (this.SLIDER_MAX / 2)) / this.SLIDER_MAX;
    }

    private int toSlider(int value, int min, int max) {
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
            this.boostBar.setVisibility(8);
            this.equalizerBox.setVisibility(8);
            updateEqualizerDisplay();
            updateService();
        } else {
            updateEqualizerDisplay();
            updateService();
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
            this.proximityBox.setVisibility(0);
            return;
        }
        log("hide proximity box");
        this.proximityBox.setVisibility(4);
    }


    @Override
    protected void onResume() {
        super.onResume();

    }

    public void onPause() {
        super.onPause();
        if (this.messenger != null) {
            log("unbind");
            try {
                unbindService(this);
            } catch (Exception e) {
            }
            this.messenger = null;
        }

    }


    void stopService() {
        log("stop service");
        if (this.messenger != null) {
            unbindService(this);
            this.messenger = null;
        }
        stopService(new Intent(this, EarpieceService.class));
    }

    void saveSettings() {
    }

    void bind() {
        log("bind");
        bindService(new Intent(this, EarpieceService.class), this, 0);
    }

    void restartService(boolean bind) {
        stopService();
        saveSettings();
        log("starting service");
        startService(new Intent(this, EarpieceService.class));
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
        openOptionsMenu();
    }

    @Override
    public void onBackPressed() {
        CustumDialog_Rate custumDialog_rate = new CustumDialog_Rate(this);
        custumDialog_rate.show();

    }

}
