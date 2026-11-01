package com.earspeakervolumebooster.callvolumeincreaser;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;


/**
 * Created by Magic Lenz on 6/26/2017.
 */

public class CustumDialog_Rate extends Dialog implements
        View.OnClickListener {

    public Activity c;
    public Dialog d;
    public TextView yes, no;

    public CustumDialog_Rate(Earpiece a) {
        super(a, Options.isDarkTheme(a)
                ? R.style.ThemeOverlay_Earpiece_Dialog_Dark
                : R.style.ThemeOverlay_Earpiece_Dialog_Light);
        this.c = a;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.custom_dialog_rate);
        yes = findViewById(R.id.yes);
        no = findViewById(R.id.no);
        yes.setOnClickListener(this);
        no.setOnClickListener(this);
        boolean dark = Options.isDarkTheme(c);
        int surface = c.getResources().getColor(dark ? R.color.alt_surface : R.color.surface);
        int outline = c.getResources().getColor(dark ? R.color.alt_outline : R.color.outline);
        int primaryText = c.getResources().getColor(
                dark ? R.color.alt_text_primary : R.color.text_primary);
        int secondaryText = c.getResources().getColor(
                dark ? R.color.alt_text_secondary : R.color.text_secondary);
        GradientDrawable card = new GradientDrawable();
        card.setColor(surface);
        card.setCornerRadius(22 * c.getResources().getDisplayMetrics().density);
        card.setStroke(Math.max(1, (int) c.getResources().getDisplayMetrics().density), outline);
        findViewById(R.id.rate_dialog_root).setBackground(card);
        ((TextView) findViewById(R.id.rate_dialog_title)).setTextColor(primaryText);
        ((TextView) findViewById(R.id.header_tv)).setTextColor(secondaryText);
        styleButton((Button) no, surface, outline, primaryText);
        styleButton((Button) yes, c.getResources().getColor(R.color.colorAccent),
                c.getResources().getColor(R.color.colorAccent), c.getResources().getColor(R.color.white));
        if (getWindow() != null) {
            getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            int width = (int) (c.getResources().getDisplayMetrics().widthPixels * 0.9f);
            getWindow().setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
        }
        setCanceledOnTouchOutside(true);
    }

    private void styleButton(Button button, int fill, int outline, int textColor) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(fill);
        background.setCornerRadius(14 * c.getResources().getDisplayMetrics().density);
        background.setStroke(Math.max(1, (int) c.getResources().getDisplayMetrics().density), outline);
        button.setBackground(background);
        button.setTextColor(textColor);

    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.yes) {
            // rate this app;
            rate();
            c.finish();
        } else if (id == R.id.no) {
            //no rating just exit APP
            c.finish();
        }
        dismiss();
    }

    private void rate() {
        Uri uri = Uri.parse("market://details?id=" + c.getPackageName());
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        // To count with Play market backstack, After pressing back button,
        // to taken back to our application, we need to add following flags to intent.
        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        try {
            c.startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            c.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=" + c.getPackageName())));
        }
    }

}
