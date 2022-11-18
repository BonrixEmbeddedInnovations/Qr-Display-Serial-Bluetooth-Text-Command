package com.bonrix.dynamicqrcode.activity;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import com.bonrix.dynamicqrcode.R;
import com.bonrix.dynamicqrcode.model.TransactionModel;
import com.bonrix.dynamicqrcode.sqlite.GcmMessageDataSource;

public class TransactionActivity extends AppCompatActivity {

    private ImageView backarrow;
    public static GcmMessageDataSource gcmMessageDataSource;
    private Button buttonrereg;
    private Button buttonclearall;
    private String TAG = "FCMmessageActivity";
    List<TransactionModel> gMessagesList = new ArrayList<>();
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
        setContentView(R.layout.activity_transaction);
        initComponent();
        getData();


    }

    private void getData() {
        gcmMessageDataSource = new GcmMessageDataSource(this);
        gcmMessageDataSource.open();
        gMessagesList.clear();
        gMessagesList = gcmMessageDataSource.get("1000");
        Log.e(TAG, "Size..." + gMessagesList.size());


        try {

            if (gMessagesList.size() <= 0) {
                Toast.makeText(this, "Data not Found", Toast.LENGTH_SHORT).show();
            } else {
                transactionAdapter = new TransactionAdapter(TransactionActivity.this, gMessagesList);
                rv_notification.setLayoutManager(new LinearLayoutManager(rv_notification.getContext()));
                rv_notification.setAdapter(transactionAdapter);
                rv_notification.scrollToPosition(gMessagesList.size());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(TransactionActivity.this, "Data not Found", Toast.LENGTH_SHORT).show();
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
                        TransactionActivity.this, datePickerListener1,
                        mYear1, mMonth1, mDay1);
                dialog.show();
            }
        });
        edtEndDT.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog dialog = new DatePickerDialog(
                        TransactionActivity.this, datePickerListener2,
                        mYear2, mMonth2, mDay2);
                dialog.show();
            }
        });
        btnSubmit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(edtStartDT.getText())){
                    Toast.makeText(TransactionActivity.this, "Enter From Date.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(edtEndDT.getText())) {
                    Toast.makeText(TransactionActivity.this, "Enter To Date.", Toast.LENGTH_SHORT).show();
                    return;
                }
                gMessagesList.clear();
                gMessagesList = gcmMessageDataSource.getTransactionFilter(edtStartDT.getText().toString(), edtEndDT.getText().toString());
                Log.e("TAG", "gMessagesList  " + gMessagesList.size());
            }
        });
        backarrow.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
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
    protected void onDestroy() {
        super.onDestroy();

    }


    @Override
    public void onBackPressed() {
        finish();
    }

    public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.CustomViewHolder> {
        private List<TransactionModel> notificationList;
        private Context mContext;
        private String TAG = "NotificationAdapter";
        GcmMessageDataSource gcmMessageDataSource;

        public TransactionAdapter(Activity mContext, List<TransactionModel> notificationList) {
            this.mContext = mContext;
            this.notificationList = notificationList;
        }


        @Override
        public CustomViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.row_transaction, viewGroup, false);
            CustomViewHolder viewHolder = new CustomViewHolder(view);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(CustomViewHolder customViewHolder, final int i) {

            final TransactionModel model = notificationList.get(i);
            customViewHolder.tv_date.setText("Date: " + model.getDate());
            customViewHolder.tv_title.setText("\u20B9 " + model.getAmount());
            customViewHolder.tv_msg.setText(model.getPaymentstatus().toUpperCase(Locale.ROOT));

            if (model.getPaymentstatus().equalsIgnoreCase("success")) {
                customViewHolder.tv_msg.setTextColor(mContext.getResources().getColor(R.color.green));
                customViewHolder.iv_notification.setBackground(mContext.getResources().getDrawable(R.drawable.ic_success));
            } else if (model.getPaymentstatus().equalsIgnoreCase("fail")) {
                customViewHolder.tv_msg.setTextColor(mContext.getResources().getColor(R.color.red));
                customViewHolder.iv_notification.setBackground(mContext.getResources().getDrawable(R.drawable.ic_fail));
            } else if (model.getPaymentstatus().equalsIgnoreCase("pending")) {
                customViewHolder.btn_update.setVisibility(View.VISIBLE);
                customViewHolder.tv_msg.setTextColor(mContext.getResources().getColor(R.color.yellow));
                customViewHolder.iv_notification.setBackground(mContext.getResources().getDrawable(R.drawable.ic_pending));
            }

            customViewHolder.btn_update.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Dialog viewDialog112 = new Dialog(mContext);
                    viewDialog112.getWindow().setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND, WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                    viewDialog112.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    LayoutInflater lin1 = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    View dialogView112 = lin1.inflate(R.layout.dialog_update, null);
                    viewDialog112.setContentView(dialogView112);
                    viewDialog112.setCancelable(false);
                    viewDialog112.getWindow().setLayout(android.view.ViewGroup.LayoutParams.FILL_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
                    viewDialog112.show();

                    ImageView iv_close = viewDialog112.findViewById(R.id.iv_close);
                    RadioGroup radio = viewDialog112.findViewById(R.id.radio);
                    RadioButton rb_success = viewDialog112.findViewById(R.id.rb_success);
                    RadioButton rb_failed = viewDialog112.findViewById(R.id.rb_failed);
                    Button btn_update = viewDialog112.findViewById(R.id.btn_update);
                    iv_close.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            viewDialog112.dismiss();
                        }
                    });

                    btn_update.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            viewDialog112.dismiss();
                            try {
                                if (gcmMessageDataSource == null) {
                                    gcmMessageDataSource = new GcmMessageDataSource(mContext);
                                    gcmMessageDataSource.open();
                                }
                                if (rb_success.isChecked()) {
                                    gcmMessageDataSource.updateTransactionValue(model.getId(), "success");

                                } else {
                                    gcmMessageDataSource.updateTransactionValue(model.getId(), "fail");
                                }
                                getData();
                                Toast.makeText(mContext, "Status Update Successfully.", Toast.LENGTH_SHORT).show();
                            } catch (Exception e) {
                                Log.e("TAG", "DB Exception update   " + e);
                            }
                        }
                    });
                }
            });

        }

        @Override
        public int getItemCount() {
            return (null != notificationList ? notificationList.size() : 0);
        }


        public class CustomViewHolder extends RecyclerView.ViewHolder {
            private TextView tv_title, tv_msg, tv_date, tv_viewmore;
            private ImageView iv_notification;
            private Button btn_update;

            public CustomViewHolder(View view) {
                super(view);
                this.tv_title = (TextView) view.findViewById(R.id.tvNotificationTitle);
                this.tv_msg = (TextView) view.findViewById(R.id.tvNotificationBody);
                this.tv_date = (TextView) view.findViewById(R.id.tvNotificationDate);
                this.iv_notification = (ImageView) view.findViewById(R.id.imageView);
                this.btn_update = (Button) view.findViewById(R.id.btn_update);
            }
        }

    }
}

