package com.bonrix.dynamicqrcode.activity;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.bonrix.dynamicqrcode.R;
import com.bonrix.dynamicqrcode.model.DateWiseTransactionModel;
import com.bonrix.dynamicqrcode.sqlite.GcmMessageDataSource;

public class DateWiseTransactionActivity extends AppCompatActivity {

    private ImageView backarrow;
    public static GcmMessageDataSource gcmMessageDataSource;
    private Button buttonrereg;
    private Button buttonclearall;
    private String TAG = "FCMmessageActivity";
    List<DateWiseTransactionModel> finallist = new ArrayList<DateWiseTransactionModel>();
    private RecyclerView rv_notification;
    private RelativeLayout linearLayout;
    private TransactionAdapter transactionAdapter;
    private EditText edtStartDT, edtEndDT;
    private String[] arrDay = {"01", "02", "03", "04", "05", "06", "07", "08",
            "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19",
            "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30",
            "31"};
    private String[] arrMonth = {"01", "02", "03", "04", "05", "06", "07",
            "08", "09", "10", "11", "12"};
    private int mYear1, mYear2;
    private int mMonth1, mMonth2;
    private int mDay1, mDay2;
    private Button btnSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_datewisetransaction);
        initComponent();
        getData();


    }

    private void getData() {
        gcmMessageDataSource = new GcmMessageDataSource(this);
        gcmMessageDataSource.open();
        finallist.clear();
        finallist = gcmMessageDataSource.getDateWiseData();
        Log.e(TAG, "Size..." + finallist.size());

        try {
            if (finallist.size() <= 0) {
                Toast.makeText(this, "Data not Found", Toast.LENGTH_SHORT).show();
            } else {
                transactionAdapter = new TransactionAdapter(DateWiseTransactionActivity.this, finallist);
                rv_notification.setLayoutManager(new LinearLayoutManager(rv_notification.getContext()));
                rv_notification.setAdapter(transactionAdapter);
                rv_notification.scrollToPosition(finallist.size());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(DateWiseTransactionActivity.this, "Data not Found", Toast.LENGTH_SHORT).show();
        }
    }

    private void initComponent() {

        backarrow = (ImageView) findViewById(R.id.backarrow);
        buttonrereg = (Button) findViewById(R.id.buttonrereg);
        buttonclearall = (Button) findViewById(R.id.buttonclearall);
        rv_notification = (RecyclerView) findViewById(R.id.rv_notofication);

        edtStartDT = findViewById(R.id.txtFromDate);
        edtEndDT = findViewById(R.id.txtToDate);
        btnSubmit = findViewById(R.id.btnSearch);
        edtStartDT.setText("");
        edtEndDT.setText("");
        Calendar c = Calendar.getInstance();
        mYear1 = c.get(Calendar.YEAR);
        mMonth1 = c.get(Calendar.MONTH);
        mDay1 = c.get(Calendar.DAY_OF_MONTH);
        mYear2 = c.get(Calendar.YEAR);
        mMonth2 = c.get(Calendar.MONTH);
        mDay2 = c.get(Calendar.DAY_OF_MONTH);
        edtStartDT.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog dialog = new DatePickerDialog(
                        DateWiseTransactionActivity.this, datePickerListener1,
                        mYear1, mMonth1, mDay1);
                dialog.show();
            }
        });
        edtEndDT.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog dialog = new DatePickerDialog(
                        DateWiseTransactionActivity.this, datePickerListener2,
                        mYear2, mMonth2, mDay2);
                dialog.show();
            }
        });
        btnSubmit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(edtStartDT.getText())){
                    Toast.makeText(DateWiseTransactionActivity.this, "Enter From Date.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(edtEndDT.getText())) {
                    Toast.makeText(DateWiseTransactionActivity.this, "Enter To Date.", Toast.LENGTH_SHORT).show();
                    return;
                }
                finallist.clear();
                finallist = gcmMessageDataSource.getDateWiseFilterData(edtStartDT.getText().toString(), edtEndDT.getText().toString());
                Log.e("TAG", "gMessagesList  " + finallist.size());
            }
        });
        backarrow.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

    }
    private DatePickerDialog.OnDateSetListener datePickerListener1 = new DatePickerDialog.OnDateSetListener() {

        public void onDateSet(DatePicker view, int selectedYear,
                              int selectedMonth, int selectedDay) {
            mYear1 = selectedYear;
            mMonth1 = selectedMonth;
            mDay1 = selectedDay;
            if (edtStartDT != null)
                edtStartDT.setText(
                        new StringBuilder()
                                .append(arrDay[mDay1 - 1]).append("-")
                                .append(arrMonth[mMonth1]).append("-")
                                .append(mYear1)
                );

        }
    };
    private DatePickerDialog.OnDateSetListener datePickerListener2 = new DatePickerDialog.OnDateSetListener() {

        public void onDateSet(DatePicker view, int selectedYear,
                              int selectedMonth, int selectedDay) {
            mYear2 = selectedYear;
            mMonth2 = selectedMonth;
            mDay2 = selectedDay;
            if (edtEndDT != null)
                edtEndDT.setText(
                        new StringBuilder()
                                .append(arrDay[mDay1 - 1]).append("-")
                                .append(arrMonth[mMonth1]).append("-")
                                .append(mYear1));
        }
    };

    @Override
    public void onBackPressed() {
        finish();
    }

    public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.CustomViewHolder> {
        private List<DateWiseTransactionModel> notificationList;
        private Context mContext;
        private String TAG = "NotificationAdapter";
        GcmMessageDataSource gcmMessageDataSource;

        public TransactionAdapter(Activity mContext, List<DateWiseTransactionModel> notificationList) {
            this.mContext = mContext;
            this.notificationList = notificationList;
        }


        @Override
        public CustomViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.row_datewisetransaction, viewGroup, false);
            CustomViewHolder viewHolder = new CustomViewHolder(view);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(CustomViewHolder customViewHolder, final int i) {

            final DateWiseTransactionModel model = notificationList.get(i);
            customViewHolder.tvDate.setText("Date: " + model.getDate());
            customViewHolder.tvsuccess_counter.setText("Total: " + model.getSuccess());
            customViewHolder.tvsuccess.setText("\u20B9 " + model.getSuccess_sum());
            customViewHolder.tvfail_counter.setText("Total: " + model.getFail());
            customViewHolder.tvFaild.setText("\u20B9 " + model.getFail_sum());
            customViewHolder.tvpending_counter.setText("Total: " + model.getPending());
            customViewHolder.tvPending.setText( "\u20B9 " + model.getPending_sum());
        }

        @Override
        public int getItemCount() {
            return (null != notificationList ? notificationList.size() : 0);
        }


        public class CustomViewHolder extends RecyclerView.ViewHolder {
            private TextView tvDate, tvsuccess, tvFaild, tvPending, tvsuccess_counter, tvfail_counter, tvpending_counter;

            public CustomViewHolder(View view) {
                super(view);
                this.tvDate = (TextView) view.findViewById(R.id.tvDate);
                this.tvsuccess = (TextView) view.findViewById(R.id.tvsuccess);
                this.tvFaild = (TextView) view.findViewById(R.id.tvFaild);
                this.tvPending = (TextView) view.findViewById(R.id.tvPending);
                this.tvsuccess_counter = (TextView) view.findViewById(R.id.tvsuccess_counter);
                this.tvfail_counter = (TextView) view.findViewById(R.id.tvfail_counter);
                this.tvpending_counter = (TextView) view.findViewById(R.id.tvpending_counter);
            }
        }

    }
}

