package com.earspeakervolumebooster.callvolumeincreaser;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
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
        super(a);
        // TODO Auto-generated constructor stub
        this.c = a;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.custom_dialog_rate);
        yes = (TextView) findViewById(R.id.yes);
        no = (TextView) findViewById(R.id.no);
        yes.setOnClickListener(this);
        no.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.yes:


                // rate this app;
                rate();
                c.finish();
                break;
            case R.id.no:

                //no rating just exit APP
                c.finish();

                break;
            default:
                break;
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
