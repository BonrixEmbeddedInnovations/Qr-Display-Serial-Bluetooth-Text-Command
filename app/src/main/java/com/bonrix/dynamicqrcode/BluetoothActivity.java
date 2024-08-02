package com.bonrix.dynamicqrcode;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.StrictMode;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import java.util.ArrayDeque;

public class BluetoothActivity extends AppCompatActivity implements View.OnClickListener, ServiceConnection, SerialListener {
    String TAG = "BluetoothActivity";
    Toolbar toolbar;
    ImageView backarrow;
    private Button btn_start, btnGenerateQr, btnWelcome, btnSuccess, btnFail, btnPending;
    private TextView receiveText;
    static Activity activity;
    private enum Connected {False, Pending, True}
    private String deviceAddress;
    private SerialService service;
    private Connected connected = Connected.False;
    private boolean initialStart = true;
    private boolean hexEnabled = false;
    private boolean pendingNewline = false;
    private String newline = TextUtil.newline_crlf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        initComponent();
    }

    private void initComponent() {
        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            deviceAddress = extras.getString("device");
        }
        activity = this;
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                .permitAll().build();
        StrictMode.setThreadPolicy(policy);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        backarrow = findViewById(R.id.backarrow);

        receiveText = findViewById(R.id.tv_bt_status);
        btnGenerateQr = findViewById(R.id.btnGenerateQr);
        btnWelcome = findViewById(R.id.btnWelcome);
        btnSuccess = findViewById(R.id.btnSuccess);
        btnFail = findViewById(R.id.btnFail);
        btnPending = findViewById(R.id.btnPending);

        backarrow.setOnClickListener(this);
        btnGenerateQr.setOnClickListener(this);
        btnWelcome.setOnClickListener(this);
        btnSuccess.setOnClickListener(this);
        btnFail.setOnClickListener(this);
        btnPending.setOnClickListener(this);

    }


    @Override
    public void onClick(View view) {
        if (view == backarrow) {
            finish();
        }

        if (view == btnGenerateQr) {
            if (deviceAddress.isEmpty()) {
                Toast.makeText(activity, "Please connect bluetooth device.", Toast.LENGTH_SHORT).show();
                return;
            }
            send(Apputils.getUpiString2("abc@icici", "testuser", "10", "432423"));
        }
        if (view == btnWelcome) {
            try {
                send(Constants.WELCOME_SCREEN);
            } catch (Exception e) {
                Log.e("TAG", "Exception   " + e);
            }
        }
        if (view == btnSuccess) {
            try {
                send(Constants.SUCCESS_SCREEN
                        .replace("<bankreff>", "31231231")
                        .replace("<orderid>", "ord231231")
                        .replace("<date>", "02-08-2024")
                );
            } catch (Exception e) {
                Log.e("TAG", "Exception   " + e);
            }
        }
        if (view == btnFail) {
            try {
                send(Constants.FAIL_SCREEN
                        .replace("<bankreff>", "31231231")
                        .replace("<orderid>", "ord231231")
                        .replace("<date>", "02-08-2024")
                );
            } catch (Exception e) {
                Log.e("TAG", "Exception   " + e);
            }
        }
        if (view == btnPending) {
            try {
                send(Constants.CANCEL_SCREEN
                        .replace("<bankreff>", "31231231")
                        .replace("<orderid>", "ord231231")
                        .replace("<date>", "02-08-2024")
                );
            } catch (Exception e) {
                Log.e("TAG", "Exception   " + e);
            }
        }
    }

    private void send(String str) {
        if (connected != Connected.True) {
            Toast.makeText(this, "not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String msg;
            byte[] data;
            if (hexEnabled) {
                StringBuilder sb = new StringBuilder();
                TextUtil.toHexString(sb, TextUtil.fromHexString(str));
                TextUtil.toHexString(sb, newline.getBytes());
                msg = sb.toString();
                data = TextUtil.fromHexString(msg);
            } else {
                msg = str;
                data = (str + newline).getBytes();
            }
            SpannableStringBuilder spn = new SpannableStringBuilder(msg + '\n');
            spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSendText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//            receiveText.append(spn);
            service.write(data);
        } catch (Exception e) {
            onSerialIoError(e);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public void onDestroy() {
        if (connected != Connected.False)
            disconnect();
        stopService(new Intent(this, SerialService.class));
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();


        Log.e(TAG, "===onStart=====");
        if (service != null) {
            service.attach(this);
        } else {
            bindService(new Intent(this, SerialService.class), this, Context.BIND_AUTO_CREATE);
            startService(new Intent(this, SerialService.class));
        }
    }

    @Override
    public void onStop() {
        if (service != null && !isChangingConfigurations())
            service.detach();
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (initialStart && service != null) {
            initialStart = false;
            runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
        service.attach(this);
        if (initialStart) {
            initialStart = false;
            runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }

    private void connect() {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            status("connecting...");
            connected = Connected.Pending;
            SerialSocket socket = new SerialSocket(getApplicationContext(), device);
            service.connect(socket);
        } catch (Exception e) {
            onSerialConnectError(e);
        }
    }

    private void receive(ArrayDeque<byte[]> datas) {
        SpannableStringBuilder spn = new SpannableStringBuilder();
        for (byte[] data : datas) {
            if (hexEnabled) {
                spn.append(TextUtil.toHexString(data)).append('\n');
            } else {
                String msg = new String(data);
                if (newline.equals(TextUtil.newline_crlf) && msg.length() > 0) {
                    // don't show CR as ^M if directly before LF
                    msg = msg.replace(TextUtil.newline_crlf, TextUtil.newline_lf);
                    // special handling if CR and LF come in separate fragments
                    if (pendingNewline && msg.charAt(0) == '\n') {
                        if (spn.length() >= 2) {
                            spn.delete(spn.length() - 2, spn.length());
                        } else {
                            Editable edt = receiveText.getEditableText();
                            if (edt != null && edt.length() >= 2)
                                edt.delete(edt.length() - 2, edt.length());
                        }
                    }
                    pendingNewline = msg.charAt(msg.length() - 1) == '\r';
                }
                spn.append(TextUtil.toCaretString(msg, newline.length() != 0));
            }
        }
        receiveText.append(spn);
    }

    private void status(String str) {
        receiveText.setText("");
        Log.e(TAG, "status   " + str);
        SpannableStringBuilder spn = new SpannableStringBuilder(str + '\n');
        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        receiveText.append(spn);
    }

    @Override
    public void onSerialConnect() {
        Log.e(TAG, "onSerialConnect");
        status("connected");
        connected = Connected.True;
    }

    @Override
    public void onSerialConnectError(Exception e) {
        Log.e(TAG, "onSerialConnectError   " + e);
        status("connection failed: ");
        disconnect();
    }

    @Override
    public void onSerialRead(byte[] data) {
        ArrayDeque<byte[]> datas = new ArrayDeque<>();
        datas.add(data);
        receive(datas);
    }

    public void onSerialRead(ArrayDeque<byte[]> datas) {
        receive(datas);
    }

    @Override
    public void onSerialIoError(Exception e) {
        Log.e(TAG, "onSerialIoError");
        status("connection lost: " + e.getMessage());
        disconnect();
    }

    private void disconnect() {
        connected = Connected.False;
        service.disconnect();
    }


}
