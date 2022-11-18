package com.bonrix.dynamicqrcode.activity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;


import com.bonrix.dynamicqrcode.R;

import java.net.URLEncoder;

public class AboutActivity extends AppCompatActivity implements View.OnClickListener {

    String TAG = "SupportActivity";
    Toolbar toolbar;
    ImageView backarrow;
    private TextView tvCustomerSupport1, tvSalesSupport1, tvWhatsAppSupport, tvSupportEmail, tvSalesEmail, tvWhatsapp;
    private ImageView ivFacebook,ivYoutube;
    LinearLayout linear1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        initComponent();


    }

    private void initComponent() {
        backarrow = findViewById(R.id.backarrow);
        tvWhatsapp = findViewById(R.id.tvWhatsapp);
        ivFacebook = findViewById(R.id.ivFacebook);
        ivYoutube = findViewById(R.id.ivYoutube);
//        tvCustomerSupport1 = findViewById(R.id.tvCustomerSupport1);
//        tvSalesSupport1 = findViewById(R.id.tvSalesSupport1);
////        tvWhatsAppSupport = findViewById(R.id.tvWhatsAppSupport);
//        tvSupportEmail = findViewById(R.id.tvSupportEmail);
//        tvSalesEmail = findViewById(R.id.tvSalesEmail);
//
//        backarrow.setOnClickListener(this);
        tvWhatsapp.setOnClickListener(this);
        ivFacebook.setOnClickListener(this);
        ivYoutube.setOnClickListener(this);
//        tvCustomerSupport1.setOnClickListener(this);
//        tvSalesSupport1.setOnClickListener(this);
//        tvSupportEmail.setOnClickListener(this);
//        tvSalesEmail.setOnClickListener(this);
    }


    @Override
    public void onClick(View view) {
        if (view == backarrow) {
            finish();
        }
        if (view == tvWhatsapp) {
            openWhatsapp(tvWhatsapp.getText().toString(), "", this);
        }
        if (view == tvCustomerSupport1) {
            openDial(tvCustomerSupport1.getText().toString());
        }
        if (view == tvSalesSupport1) {
            openDial(tvSalesSupport1.getText().toString());
        }
        if (view == tvSupportEmail) {
            openEmail(tvSupportEmail.getText().toString());
        }
        if (view == tvSalesEmail) {
            openEmail(tvSalesEmail.getText().toString());
        }
        if (view == ivFacebook) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/BonrixDynamicQRCodeTechnologies"));
            startActivity(browserIntent);
        }
        if (view == ivYoutube) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://youtube.com/@BonrixSoftwareSystems"));
            startActivity(browserIntent);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    private void openDial(String number) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + number));
        startActivity(intent);
    }

    private void openEmail(String email) {
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setData(Uri.parse("mailto:"));
        emailIntent.setType("text/plain");


        emailIntent.putExtra(Intent.EXTRA_EMAIL, email);
        //emailIntent.putExtra(Intent.EXTRA_CC, CC);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Write your FeedBack:\n");

        try {
            startActivity(Intent.createChooser(emailIntent, "Send mail..."));
            finish();
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(AboutActivity.this,
                    "There is no email client installed.", Toast.LENGTH_SHORT).show();
        }
    }

    public static boolean whatsappInstalledOrNot(String uri, Context context) {
        PackageManager pm = context.getPackageManager();
        boolean app_installed = false;
        try {
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            app_installed = true;
        } catch (PackageManager.NameNotFoundException e) {
            app_installed = false;
        }
        return app_installed;
    }

    public static void openWhatsapp(String number, String msg, Context context) {
        String defultapp = "WhatsApp";

        boolean isWhatsappInstalled = whatsappInstalledOrNot("com.whatsapp", context);
        boolean isWhatsappBusinessInstalled = whatsappInstalledOrNot("com.whatsapp.w4b", context);
        if (isWhatsappBusinessInstalled || isWhatsappInstalled) {

            try {
                String url = "https://api.whatsapp.com/send?phone=+91" + number +
                        "&text=" + URLEncoder.encode(msg, "UTF-8");

                Intent sendIntent = new Intent("android.intent.action.MAIN");
                sendIntent.setAction(Intent.ACTION_VIEW);

                if (defultapp.equalsIgnoreCase("WhatsApp")) {

                    if (isWhatsappInstalled) {
                        sendIntent.setPackage("com.whatsapp");
                        Log.e("message", "WhatsApp");
                    } else if (isWhatsappBusinessInstalled) {
                        sendIntent.setPackage("com.whatsapp.w4b");
                        Log.e("message", "Business WhatsApp");
                    }

                }

                sendIntent.setData(Uri.parse(url));
                context.startActivity(sendIntent);

            } catch (Exception e) {
                Log.e("message", e.getMessage());
            }

        } else {
            Toast.makeText(context, "WhatsApp Not Install!", Toast.LENGTH_SHORT).show();
        }
    }
}
