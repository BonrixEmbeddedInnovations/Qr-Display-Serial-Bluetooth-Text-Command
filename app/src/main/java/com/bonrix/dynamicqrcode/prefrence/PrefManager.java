package com.bonrix.dynamicqrcode.prefrence;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PrefManager {
    public static SharedPreferences mPrefs;
    public static String PREF_ISUPI="is_upi";
    public static String PREF_UPIID="upiid";
    public static String PREF_PAYEENAME="payeename";



    public static void savePref(Context context,String key, String value) {
        mPrefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        SharedPreferences.Editor e = mPrefs.edit();
        e.putString(key, value);
        e.commit();
    }

    public static void saveBoolPref(Context context, String key, Boolean value) {
        mPrefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        SharedPreferences.Editor e = mPrefs.edit();
        e.putBoolean(key, value);
        e.commit();
    }

    public static String getPref(Context context, String key) {
        mPrefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        String value = mPrefs.getString(
                key, "");
        return value;
    }

    public static Boolean getBoolPref(Context context, String key) {
        mPrefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        Boolean value = mPrefs.getBoolean(
                key, false);
        return value;
    }
}
