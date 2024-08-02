package com.bonrix.dynamicqrcode;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Window;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;

public class Apputils {
    public static boolean connectornot = false;


    public static String getCurrnetDateTime2() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
        return sdf.format(new Date());
    }
    public static String getUpiString2(String upi_payee, String payeename, String amt, String txid) {
        String upistring = "DisplayQRCodeScreen**upi://pay?pa=<upi_payee>&pn=<payee_name>&cu=INR&am=<amt>&pn=<payee_name>**<amt>**<upi_payee>";
        upistring = upistring.replaceAll("<upi_payee>", upi_payee);
        upistring = upistring.replaceAll("<payee_name>", payeename);
        upistring = upistring.replaceAll("<amt>", amt);
        upistring = upistring.replaceAll("<txid>", txid);
        Log.e("TAG","upistring  "+upistring);

        return upistring;
//        return "DisplayQRCodeScreen**upi://pay?pa=63270083167.payswiff@indus&pn=Bonrix&cu=INR&am=10&pn=Bonrix%20Software%20Systems**10**7418529631@icici";
    }



}
