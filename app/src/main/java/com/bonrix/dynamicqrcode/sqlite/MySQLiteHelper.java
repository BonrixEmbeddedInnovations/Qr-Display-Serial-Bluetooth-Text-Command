package com.bonrix.dynamicqrcode.sqlite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MySQLiteHelper extends SQLiteOpenHelper {


    public static final String TABLE_CART = "tbl_cart";
    public static final String COLUMN_autoid = "autoid";
    public static final String COLUMN_dbitem_id = "dbitem_id";
    public static final String COLUMN_dbitemname = "dbitemname";
    public static final String COLUMN_dbitemdescription = "dbitemdescription";
    public static final String COLUMN_dbitemimage = "dbitemimage";
    public static final String COLUMN_dbitemprice = "dbitemprice";
    public static final String COLUMN_dbitemtaxrate = "dbitemtaxrate";
    public static final String COLUMN_dbbaseunit = "dbbaseunit";
    public static final String COLUMN_dbbaseqty = "dbbaseqty";
    public static final String COLUMN_dbcategory_id = "dbcategory_id";
    public static final String COLUMN_dbcartqty = "dbcartqty";
    public static final String COLUMN_dbextra1 = "dbextra1";
    public static final String COLUMN_dbextra2 = "dbextra2";
    public static final String COLUMN_dbextra3 = "dbextra3";
    public static final String COLUMN_dbextra4 = "dbextra4";
    public static final String COLUMN_dbbrand = "dbbrand";

    public static final String TABLE_GCM_MESSAGE = "tbl_fcm_message";
    public static final String TABLE_TRANSACTION_HISTORY = "tbl_transactionhistory";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_DATETIME = "date_time";
    public static final String COLUMN_MESSAGE = "message";
    public static final String COLUMN_UPI = "upi";
    public static final String COLUMN_SENDER = "sender";
    public static final String COLUMN_PAYMENT_STATUS = "payment_status";
    public static final String COLUMN_ICON = "icon";
    public static final String COLUMN_AMOUNT = "amount";
    public static final String COLUMN_EXTRA = "extra";
    public static final String COLUMN_TITLE = "title";
    private String TAG = "MySQLiteHelper";

    private static final String DATABASE_TRANSACTION = "create table "
            + TABLE_TRANSACTION_HISTORY + "(" + COLUMN_ID
            + " INTEGER primary key autoincrement, " + COLUMN_UPI
            + " TEXT ," + COLUMN_DATETIME + " DATE," + COLUMN_PAYMENT_STATUS + " TEXT," + COLUMN_AMOUNT + " TEXT,"
            + COLUMN_EXTRA + " TEXT" + ");";

    private static final String DATABASE_CREATE_PROCESSED = "create table "
            + TABLE_CART + "(" + COLUMN_autoid
            + " INTEGER primary key autoincrement, " + COLUMN_dbitem_id
            + " TEXT ," + COLUMN_dbitemname + " TEXT ," + COLUMN_dbitemdescription
            + " TEXT ," + COLUMN_dbitemimage + " TEXT ," + COLUMN_dbitemprice
            + " TEXT ," + COLUMN_dbitemtaxrate + " TEXT ," + COLUMN_dbbaseunit
            + " TEXT ," + COLUMN_dbbaseqty + " TEXT ," + COLUMN_dbcategory_id + " TEXT ,"
            + COLUMN_dbcartqty + " TEXT ," + COLUMN_dbextra1 + " TEXT ," + COLUMN_dbextra2 + " TEXT ,"
            + COLUMN_dbextra3 + " TEXT ,"
            + COLUMN_dbextra4 + " TEXT ,"
            + COLUMN_dbbrand + " TEXT" + ");";

    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table "
            + TABLE_GCM_MESSAGE + "(" + COLUMN_ID
            + " INTEGER primary key autoincrement, " + COLUMN_MESSAGE
            + " TEXT ," + COLUMN_DATETIME + " TEXT," + COLUMN_SENDER + " TEXT," + COLUMN_ICON + " TEXT,"
            + COLUMN_TITLE + " TEXT" + ");";


    private static final String DATABASE_NAME = "thinpcqrdisplay.db";
    private static final int DATABASE_VERSION = 1;

    public MySQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_TRANSACTION);
//		database.execSQL(DATABASE_CREATE_PROCESSED);
//		database.execSQL(DATABASE_CREATE);
        Log.e(TAG, "Table Created...." + DATABASE_CREATE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(MySQLiteHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSACTION_HISTORY);
        onCreate(db);
    }

}