package com.bonrix.dynamicqrcode.sqlite;

import static com.bonrix.dynamicqrcode.sqlite.MySQLiteHelper.COLUMN_ID;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import com.bonrix.dynamicqrcode.model.DateWiseTransactionModel;
import com.bonrix.dynamicqrcode.model.TransactionModel;


public class GcmMessageDataSource {

    private static final String tag = "GcmMessageDataSource";
    private SQLiteDatabase database;
    private MySQLiteHelper dbHelper;

    public GcmMessageDataSource(Context context) {
        dbHelper = new MySQLiteHelper(context);
    }

    public void open() throws SQLException {
        dbHelper.onOpen(database);
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    private String[] allColumns = {COLUMN_ID,
            MySQLiteHelper.COLUMN_UPI, MySQLiteHelper.COLUMN_DATETIME,
            MySQLiteHelper.COLUMN_PAYMENT_STATUS, MySQLiteHelper.COLUMN_AMOUNT, MySQLiteHelper.COLUMN_EXTRA};


    public void updateFilterCart(int tid, String dbcartqty) {
        ContentValues values = new ContentValues();
        values.put(MySQLiteHelper.COLUMN_dbcartqty, dbcartqty);
        database.update(MySQLiteHelper.TABLE_CART, values,
                MySQLiteHelper.COLUMN_autoid + "=" + tid, null);
    }

    public void deletemsgCart(int idd) {
        Log.i(tag, "delete:message:" + idd);
        database.delete(MySQLiteHelper.TABLE_CART, MySQLiteHelper.COLUMN_autoid
                + "=" + idd, null);
    }

    public void deleteCartTable() {
        database.delete(MySQLiteHelper.TABLE_CART, null, null);
    }


    public List<TransactionModel> get(String limit) {
        List<TransactionModel> gcmMessages = new ArrayList<TransactionModel>();
        Cursor cursor = database.query(MySQLiteHelper.TABLE_TRANSACTION_HISTORY,
                allColumns, null, null, null, null, "id DESC", limit);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            TransactionModel gcmMessage = cursorToGcmMessage(cursor);
            gcmMessages.add(gcmMessage);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return gcmMessages;
    }

    public List<TransactionModel> getSameDate() {
        List<TransactionModel> gcmMessages = new ArrayList<TransactionModel>();
        Cursor cursor = database.query(MySQLiteHelper.TABLE_TRANSACTION_HISTORY,
                allColumns, null, null, null, null, "id DESC", null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            TransactionModel gcmMessage = cursorToGcmMessage(cursor);
            gcmMessages.add(gcmMessage);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return gcmMessages;
    }

    //
    private TransactionModel cursorToGcmMessage(Cursor cursor) {
        TransactionModel gcmMessage = new TransactionModel();
        gcmMessage.setId(cursor.getInt(0));
        gcmMessage.setUpi(cursor.getString(1));
        gcmMessage.setDate(cursor.getString(2));
        gcmMessage.setPaymentstatus(cursor.getString(3));
        gcmMessage.setAmount(cursor.getString(4));
        gcmMessage.setOrderid(cursor.getString(5));
        return gcmMessage;
    }


    public void saveTransaction(String upi, String dateTime, String status, String amount,
                                String extra) {
        ContentValues values = new ContentValues();
        values.put(MySQLiteHelper.COLUMN_UPI, upi);
        values.put(MySQLiteHelper.COLUMN_DATETIME, dateTime);
        values.put(MySQLiteHelper.COLUMN_PAYMENT_STATUS, status);
        values.put(MySQLiteHelper.COLUMN_AMOUNT, amount);
        values.put(MySQLiteHelper.COLUMN_EXTRA, extra);
        database.insert(MySQLiteHelper.TABLE_TRANSACTION_HISTORY, null, values);
        Log.e("TAG", "Insert Success");
    }

    public void updateTransactionValue(int tid, String status) {
        ContentValues values = new ContentValues();
        values.put(MySQLiteHelper.COLUMN_PAYMENT_STATUS, status);
        database.update(MySQLiteHelper.TABLE_TRANSACTION_HISTORY, values,
                COLUMN_ID + "=" + tid, null);
    }

    public String getSingleColumnId(String extra) {
        String id = "";
        String selectQuery = "SELECT id FROM tbl_transactionhistory WHERE extra=?";
        Log.e("TAG", "selectQuery   " + selectQuery);
        Cursor c = database.rawQuery(selectQuery, new String[]{extra});
        if (c.moveToFirst()) {
            id = c.getString(0);
        }
        c.close();
        return id;

    }


    public List<DateWiseTransactionModel> getDateWiseData() {
        List<DateWiseTransactionModel> gcmMessages = new ArrayList<DateWiseTransactionModel>();
//        String selectQuery = "select substr(date_time,7,4) || '-' || substr(date_time,4,2) || '-' || substr(date_time,1,2), Total(case when trim(lower(payment_status)) = 'success' then 1 else 0 end) as 'success', Total(case when trim(lower(payment_status)) = 'fail' then 1 else 0 end) as 'fail', Total(case when trim(lower(payment_status)) = 'pending' then 1 else 0 end) as 'pending' from tbl_transactionhistory group by substr(date_time,7,4) || '-' || substr(date_time,4,2) || '-' || substr(date_time,1,2)";
        String selectQuery = "select substr(date_time,7,4) || '-' || substr(date_time,4,2) || '-' || substr(date_time,1,2) as 'date', Total(case when trim(lower(payment_status)) = 'success' then 1 else 0 end) as 'success', Total(case when trim(lower(payment_status)) = 'success' then amount else 0 end) as 'successsum',Total(case when trim(lower(payment_status)) = 'fail' then amount else 0 end) as 'failsum', Total(case when trim(lower(payment_status)) = 'fail' then 1 else 0 end) as 'fail', Total(case when trim(lower(payment_status)) = 'pending' then 1 else 0 end) as 'pending',Total(case when trim(lower(payment_status)) = 'pending' then amount else 0 end) as 'pendingsum' from tbl_transactionhistory group by substr(date_time,7,4) || '-' || substr(date_time,4,2) || '-' || substr(date_time,1,2)";
        Cursor cursor = database.rawQuery(selectQuery, new String[]{});
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            DateWiseTransactionModel gcmMessage = cursorToDateWise(cursor);
            gcmMessages.add(gcmMessage);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return gcmMessages;
    }

    public List<DateWiseTransactionModel> getDateWiseFilterData(String sdate, String edate) {
        List<DateWiseTransactionModel> gcmMessages = new ArrayList<DateWiseTransactionModel>();
        String selectQuery = "select substr(date_time,7,4) || '-' || substr(date_time,4,2) || '-' || substr(date_time,1,2) as 'date', Total(case when trim(lower(payment_status)) = 'success' then 1 else 0 end) as 'success', Total(case when trim(lower(payment_status)) = 'success' then amount else 0 end) as 'successsum', Total(case when trim(lower(payment_status)) = 'fail' then amount else 0 end) as 'failsum', Total(case when trim(lower(payment_status)) = 'fail' then 1 else 0 end) as 'fail', Total(case when trim(lower(payment_status)) = 'pending' then 1 else 0 end) as 'pending', Total(case when trim(lower(payment_status)) = 'pending' then amount else 0 end) as 'pendingsum' from tbl_transactionhistory WHERE substr(date_time,1,2) || '-' || substr(date_time,4,2) || '-' || substr(date_time,7,4) BETWEEN '" + sdate + "' AND '" + edate + "' group by substr(date_time,7,4) || '-' || substr(date_time,4,2) || '-' || substr(date_time,1,2)";
        Cursor cursor = database.rawQuery(selectQuery, new String[]{});
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            DateWiseTransactionModel gcmMessage = cursorToDateWise(cursor);
            gcmMessages.add(gcmMessage);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return gcmMessages;
    }

    public List<TransactionModel> getTransactionFilter(String sdate, String edate) {
        List<TransactionModel> gcmMessages = new ArrayList<TransactionModel>();
        String selectQuery = "select * from tbl_transactionhistory WHERE substr(date_time,1,2)  || '-' || substr(date_time,4,2) || '-' ||substr(date_time,7,4)  BETWEEN '" + sdate + "' AND '" + edate + "'";

        Cursor cursor = database.rawQuery(selectQuery, new String[]{});
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            TransactionModel gcmMessage = cursorToGcmMessage(cursor);
            gcmMessages.add(gcmMessage);
            cursor.moveToNext();
        }
        cursor.close();
        return gcmMessages;
    }

    private DateWiseTransactionModel cursorToDateWise(Cursor cursor) {
        DateWiseTransactionModel gcmMessage = new DateWiseTransactionModel();
        gcmMessage.setDate(cursor.getString(0));
        gcmMessage.setSuccess(cursor.getString(1));
        gcmMessage.setSuccess_sum(cursor.getString(2));
        gcmMessage.setFail_sum(cursor.getString(3));
        gcmMessage.setFail(cursor.getString(4));
        gcmMessage.setPending(cursor.getString(5));
        gcmMessage.setPending_sum(cursor.getString(6));
        return gcmMessage;
    }

}