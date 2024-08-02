package com.bonrix.dynamicqrcode;

class Constants {

    // values have to be globally unique
    static final String INTENT_ACTION_DISCONNECT = BuildConfig.APPLICATION_ID + ".Disconnect";
    static final String NOTIFICATION_CHANNEL = BuildConfig.APPLICATION_ID + ".Channel";
    static final String INTENT_CLASS_MAIN_ACTIVITY = BuildConfig.APPLICATION_ID + ".MainActivity";
    public static String WELCOME_SCREEN="WelcomeScreen**";
    public static String SUCCESS_SCREEN="DisplaySuccessQRCodeScreen**<bankreff>**<orderid>**<date>";
    public static String FAIL_SCREEN="DisplayFailQRCodeScreen**<bankreff>**<orderid>**<date>";
    public static String CANCEL_SCREEN="DisplayCancelQRCodeScreen**<bankreff>**<orderid>**<date>";
    // values have to be unique within each app
    static final int NOTIFY_MANAGER_START_FOREGROUND_SERVICE = 1001;

    private Constants() {}
}
