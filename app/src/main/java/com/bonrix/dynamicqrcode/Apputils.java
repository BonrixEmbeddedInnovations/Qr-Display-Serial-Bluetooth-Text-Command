package com.bonrix.dynamicqrcode;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.Window;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Apputils {


    public static Dialog showDialogProgressBarNew(Context context) {
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.setContentView(R.layout.dialognew);
        if (!dialog.isShowing()) {
            dialog.show();
        }
        return dialog;
    }

    public static String getUpiString(String upi_payee, String payeename, String amt, String txid) {
        String sendstring = "";
        String upistring = "upi://pay?pa=<upi_payee>&pn=<payee_name>&am=<amt>&cu=INR&tn=<txid>&tr=<txid>&tid=<txid>";
//        String upistring = "upi://pay?pa=<upi_payee>&pn=<payee_name>&oobe=fos123&qrst=stk&tr=11805909345abcd&am=<amt>&cu=INR&tn=11805909345abcd&tid=11805909345abcd";
        upistring = upistring.replaceAll("<upi_payee>", upi_payee);
        upistring = upistring.replaceAll("<payee_name>", payeename);
        upistring = upistring.replaceAll("<amt>", amt);
        upistring = upistring.replaceAll("<txid>", txid);
        return upistring;
    }

    public static String getPhonepayUpiString(String upi_payee, String payeename, String amt, String txid) {
        String sendstring = "";
        String upistring = "upi://pay?pa=<upi_payee>&pn=<payee_name>&mc=0000&mode=02&purpose=00&am=<amt>&cu=INR";

        upistring = upistring.replaceAll("<upi_payee>", upi_payee);
        upistring = upistring.replaceAll("<payee_name>", payeename);
        upistring = upistring.replaceAll("<amt>", amt);
        upistring = upistring.replaceAll("<txid>", txid);
        return upistring;
    }

    public static String getCurrnetDateTime() {
//        SimpleDateFormat sdf = new SimpleDateFormat("dd/mm/yyyy HH:mm:ss", Locale.getDefault());
//        return sdf.format(new Date());

        return new SimpleDateFormat("dd-MM-yyyy hh:mm:ss", Locale.getDefault()).format(new Date());
//        return "01-11-2022 10:37:23";
    }

    public static String getCurrnetDateTime2() {
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
        return sdf.format(new Date());
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        int width = drawable.getIntrinsicWidth();
        width = width > 0 ? width : 1;
        int height = drawable.getIntrinsicHeight();
        height = height > 0 ? height : 1;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);


        return bitmap;
    }

    public static Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);

        if (resizedBitmap != bm) {
            bm.recycle();
        }
//        bm.recycle();
        return resizedBitmap;
    }

    public static Bitmap RotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public static Bitmap convert1Bit(Bitmap bitmap) {
        Bitmap bwBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        float[] hsv = new float[3];
        for (int col = 0; col < bitmap.getWidth(); col++) {
            for (int row = 0; row < bitmap.getHeight(); row++) {
                Color.colorToHSV(bitmap.getPixel(col, row), hsv);
                if (hsv[2] > 0.5f) {
                    bwBitmap.setPixel(col, row, 0xffffffff);
                } else {
                    bwBitmap.setPixel(col, row, 0xff000000);
                }
            }
        }
        return bwBitmap;
    }


    public static String byteArrayToHexString(byte[] bytes) {
        char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        char[] hexChars = new char[bytes.length * 2];
        int v;

        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String Right(String input, int length) {
        try {
            if (input == null) {
                return "";
            }
            if (input.length() <= length) {
                return input;
            }
            return input.substring(input.length() - length);
        } catch (Exception e) {
            return input;
        }
    }


}
