package com.bonrix.dynamicqrcode;

import static android.content.Context.WINDOW_SERVICE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.TimeUtils;
import androidx.core.view.MotionEventCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.bonrix.dynamicqrcode.activity.AboutActivity;
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver;
import com.hoho.android.usbserial.driver.ProbeTable;
import com.hoho.android.usbserial.driver.SerialTimeoutException;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;

import com.bonrix.dynamicqrcode.activity.DateWiseTransactionActivity;
import com.bonrix.dynamicqrcode.activity.TransactionActivity;
import com.bonrix.dynamicqrcode.prefrence.PrefManager;
import com.bonrix.dynamicqrcode.sqlite.GcmMessageDataSource;


public class NewHomeFragment extends Fragment implements ServiceConnection, SerialListener {


    private enum Connected {False, Pending, True}

    private final BroadcastReceiver broadcastReceiver;
    private int deviceId, portNum, baudRate;
    private UsbSerialPort usbSerialPort;
    private SerialService service;

    private TextView receiveText;
    private ImageView imageview;
    private TextView sendText, tvAbout;
    private ControlLines controlLines;
    private TextUtil.HexWatcher hexWatcher;

    private Connected connected = Connected.False;
    private boolean initialStart = true;
    private boolean hexEnabled = false;
    private boolean controlLinesEnabled = false;
    private boolean pendingNewline = false;
    private String newline = TextUtil.newline_crlf;
    private Button btnDateWiseHistory, btnWelcome, btnHistory, btnGenerateQr;
    private EditText etAmount;
    public int sendTime = -1;
    QRGEncoder qrgEncoder;
    GcmMessageDataSource gcmMessageDataSource;

    public NewHomeFragment() {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Constants.INTENT_ACTION_GRANT_USB.equals(intent.getAction())) {
                    Boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
                    connect(granted);
                }
            }
        };
    }

    /*
     * Lifecycle
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);

        try {
            deviceId = getArguments().getInt("device");
            portNum = getArguments().getInt("port");
            baudRate = getArguments().getInt("baud");
        } catch (Exception e) {
        }
    }

    @Override
    public void onDestroy() {
        if (connected != Connected.False) disconnect();
        getActivity().stopService(new Intent(getActivity(), SerialService.class));
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (service != null) service.attach(this);
        else
            getActivity().startService(new Intent(getActivity(), SerialService.class)); // prevents service destroy on unbind from recreated activity caused by orientation change
    }

    @Override
    public void onStop() {
        if (service != null && !getActivity().isChangingConfigurations()) service.detach();
        super.onStop();
    }

    @SuppressWarnings("deprecation")
    // onAttach(context) was added with API 23. onAttach(activity) works for all API versions
    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        getActivity().bindService(new Intent(getActivity(), SerialService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDetach() {
        try {
            getActivity().unbindService(this);
        } catch (Exception ignored) {
        }
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(broadcastReceiver, new IntentFilter(Constants.INTENT_ACTION_GRANT_USB));
        if (initialStart && service != null) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
        if (controlLinesEnabled && controlLines != null && connected == Connected.True)
            controlLines.start();
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver(broadcastReceiver);
        if (controlLines != null) controlLines.stop();
        super.onPause();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
        service.attach(this);
        if (initialStart && isResumed()) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_newhome, container, false);
        receiveText = view.findViewById(R.id.receive_text);                          // TextView performance decreases with number of spans
        imageview = view.findViewById(R.id.imageview);                          // TextView performance decreases with number of spans
        btnGenerateQr = view.findViewById(R.id.btnGenerateQr);                          // TextView performance decreases with number of spans
        btnDateWiseHistory = view.findViewById(R.id.btnDateWiseHistory);                          // TextView performance decreases with number of spans
        btnHistory = view.findViewById(R.id.btnHistory);                          // TextView performance decreases with number of spans
        btnWelcome = view.findViewById(R.id.btnWelcome);                          // TextView performance decreases with number of spans
        etAmount = view.findViewById(R.id.etAmount);                          // TextView performance decreases with number of spans
        receiveText.setTextColor(getResources().getColor(R.color.colorRecieveText)); // set as default color to reduce number of spans
        receiveText.setMovementMethod(ScrollingMovementMethod.getInstance());
        sendText = view.findViewById(R.id.send_text);
        tvAbout = view.findViewById(R.id.tvAbout);
        hexWatcher = new TextUtil.HexWatcher(sendText);
        hexWatcher.enable(hexEnabled);
        sendText.addTextChangedListener(hexWatcher);
        sendText.setHint(hexEnabled ? "HEX mode" : "");

        View sendBtn = view.findViewById(R.id.send_btn);
        sendBtn.setOnClickListener(v -> send(sendText.getText().toString(), true));
        controlLines = new ControlLines(view);

        btnHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getActivity(), TransactionActivity.class));
            }
        });
        btnDateWiseHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getActivity(), DateWiseTransactionActivity.class));
            }
        });
        btnWelcome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {

                    if (connected.toString().equalsIgnoreCase("False")) {
                        Toast.makeText(getActivity(), "First Connect USB Device.", Toast.LENGTH_SHORT).show();
                        replaceFragment(new DevicesFragment(), R.id.fragment, DevicesFragment.class.getName());
                        return;
                    }

                    InputStream input1 = getActivity().getAssets().open("home.bmp");
                    Bitmap bitmap = BitmapFactory.decodeStream(input1);
                    Bitmap resize_bitmap = Apputils.getResizedBitmap(bitmap, 320, 480);
                    send1(ToBmp16(resize_bitmap), true);
                } catch (Exception e) {
                    Log.e("TAG", "Exception   " + e);
                }
            }
        });
        btnGenerateQr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!PrefManager.getBoolPref(getActivity(), PrefManager.PREF_ISUPI)) {
                    dialogUpiSetting();
                    return;
                }
                if (TextUtils.isEmpty(etAmount.getText())) {
                    Toast.makeText(getActivity(), "Enter Valid Amount...", Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.e("TAG", "connected   " + connected);
                if (connected.toString().equalsIgnoreCase("False")) {
                    Toast.makeText(getActivity(), "First Connect USB Device.", Toast.LENGTH_SHORT).show();
                    replaceFragment(new DevicesFragment(), R.id.fragment, DevicesFragment.class.getName());
                    return;
                }
                String orderid = Apputils.getCurrnetDateTime2();
//                String upistring = Apputils.getUpiString("shraddhatradelink@yesbank", "Shraddha", etAmount.getText().toString(), orderid);
                String upistring = Apputils.getUpiString(PrefManager.getPref(getActivity(), PrefManager.PREF_UPIID).trim(), PrefManager.getPref(getActivity(), PrefManager.PREF_PAYEENAME).trim(), etAmount.getText().toString(), orderid);
                Log.e("TAG", "upistring  " + upistring);

                if (TextUtils.isEmpty(upistring)) {
                    Toast.makeText(getActivity(), "Invalid UPI Data", Toast.LENGTH_SHORT).show();
                } else {
                    displayTxnQr();
                }

            }
        });
        tvAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getActivity(), AboutActivity.class));
            }
        });
        return view;
    }

    private void displayTxnQr() {
        String orderid = Apputils.getCurrnetDateTime2();
//        String upistring = Apputils.getUpiString("shraddhatradelink@yesbank", "Shraddha", "10".toString(), orderid);
        String upistring = Apputils.getUpiString(PrefManager.getPref(getActivity(), PrefManager.PREF_UPIID).trim(), PrefManager.getPref(getActivity(), PrefManager.PREF_PAYEENAME).trim(), etAmount.getText().toString(), orderid);
        if (TextUtils.isEmpty(upistring)) {
            Toast.makeText(getActivity(), "Invalid UPI Data", Toast.LENGTH_SHORT).show();
        } else {
            try {
                WindowManager manager = (WindowManager) getActivity().getSystemService(WINDOW_SERVICE);
                Display display = manager.getDefaultDisplay();
                Point point = new Point();
                display.getSize(point);
                int width = 280;
                int height = 280;
                int smallerDimension = width < height ? width : height;
                smallerDimension = smallerDimension * 3 / 4;
                Log.e("TAG", "smallerDimension  " + smallerDimension);

                qrgEncoder = new QRGEncoder(upistring, null, QRGContents.Type.TEXT, 280);
                Bitmap bitmap_qr = qrgEncoder.encodeAsBitmap();
                Log.e("TAG", "bitmap_qr  " + bitmap_qr.getWidth());
                Log.e("TAG", "bitmap_qr  " + bitmap_qr.getHeight());
                Bitmap bitmap_topay = getBitmapToPay();
                Bitmap finalbmp = mergeToPin(bitmap_topay, bitmap_qr, etAmount.getText().toString());
                Bitmap resize_bitmap = Apputils.getResizedBitmap(finalbmp, 320, 480);
                send1(ToBmp16(resize_bitmap), true);
                try {
                    if (gcmMessageDataSource == null) {
                        gcmMessageDataSource = new GcmMessageDataSource(getActivity());
                        gcmMessageDataSource.open();
                    }
                    Log.e("TAG", "getCurrnetDateTime   " + Apputils.getCurrnetDateTime());

                    gcmMessageDataSource.saveTransaction(upistring, Apputils.getCurrnetDateTime(), "pending", etAmount.getText().toString(), orderid);
                } catch (Exception e) {
                    Log.e("TAG", "DB Exception   " + e);
                }
                dialogQr(bitmap_qr, orderid);
            } catch (Exception e) {
                Log.e("TAG", "Exception  " + e);
                e.printStackTrace();
            }
        }
    }

    public Bitmap mergeToPin(Bitmap bitmap_topay, Bitmap bitmap_qr, String amount) {
        Bitmap result = null;
        if (result != null && !result.isRecycled()) {
            result.recycle();
            result = null;
        }
        result = Bitmap.createBitmap(bitmap_topay.getWidth(), bitmap_topay.getHeight(), bitmap_topay.getConfig());
        Canvas canvas = new Canvas(result);

        int w_bitmap_topay = bitmap_topay.getWidth();


        int w_bitmap_qr = bitmap_qr.getWidth();


        float x = (w_bitmap_topay - w_bitmap_qr) / 2;
        float y = (w_bitmap_topay - w_bitmap_qr) / 2 + 160;


        canvas.drawBitmap(bitmap_topay, 0f, 0f, null);
        canvas.drawBitmap(bitmap_qr, x, y, null);

        Typeface tf = Typeface.createFromAsset(getActivity().getAssets(), "OpenSans-Bold.ttf");
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.BLUE);
        paint.setTextSize((35f));
        paint.setTypeface(tf);
        canvas.drawText("\u20B9 " + amount, ((w_bitmap_topay - 90) / 2), 115f, paint);
        return result;
    }

    public Bitmap getBitmapToPay() throws IOException {
        AssetManager assetManager = getActivity().getAssets();

        InputStream istr = assetManager.open("topay.bmp");
        Bitmap bitmap = BitmapFactory.decodeStream(istr);
        istr.close();
        return bitmap;
    }

    @SuppressLint("NewApi")
    private byte[] ToBmp16(Bitmap bitmap) {
        byte[] numArray = new byte[0];
        try {
            numArray = new byte[bitmap.getWidth() * bitmap.getHeight() * 2];
            short num = 0;
            short num1 = 0;
            int num2 = 0;
            for (int i = 0; i < bitmap.getHeight(); i++) {
                for (int j = 0; j < bitmap.getWidth(); j++) {
                    @SuppressLint({"NewApi", "LocalSuppress"}) int pixel = bitmap.getPixel(j, bitmap.getHeight() - i - 1);
                    byte r = (byte) (Color.red(pixel) >> 3 & 31);
                    byte g = (byte) (Color.green(pixel) >> 2 & 63);
                    byte b = (byte) (Color.blue(pixel) >> 3 & 31);
                    short num3 = (short) (r << 11);
                    num1 = (short) (g << 5);
                    num = (short) (num3 | num1 | b);
                    numArray[num2] = (byte) (num >> 8);
                    numArray[num2 + 1] = (byte) num;
                    num2 += 2;
                }
            }
        } catch (Exception e) {
            Log.e("TAG", "Exception  " + e);
        }
        return numArray;
    }

    private void send1(byte[] result, boolean type) {
        Log.e("TAG", "-----send------");
        if (connected != Connected.True) {
            Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            service.write(result);
        } catch (SerialTimeoutException e) {
            Log.e("TAG", "e  " + e);
            status("write timeout: " + e.getMessage());
        } catch (Exception e) {
            Log.e("TAG", "Exception  " + e);

            onSerialIoError(e);
        }
    }

    void replaceFragment(Fragment mFragment, int id, String tag) {
        FragmentTransaction mTransaction = getActivity().getSupportFragmentManager().beginTransaction();
        mTransaction.replace(id, mFragment);
        mTransaction.addToBackStack(mFragment.toString());
        mTransaction.commit();

    }

    private void dialogQr(Bitmap rotate_bitmap, String orderid) {
        Dialog viewDialog112 = new Dialog(getActivity());
        viewDialog112.getWindow().setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND, WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        viewDialog112.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LayoutInflater lin1 = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialogView112 = lin1.inflate(R.layout.dialog_qr, null);
        viewDialog112.setContentView(dialogView112);
        viewDialog112.setCancelable(false);
        viewDialog112.getWindow().setLayout(android.view.ViewGroup.LayoutParams.FILL_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        viewDialog112.show();
        ImageView iv_close = dialogView112.findViewById(R.id.iv_close);
        ImageView imageView = dialogView112.findViewById(R.id.imageview);
        Button btnSuccess = dialogView112.findViewById(R.id.btnSuccess);
        Button btnpending = dialogView112.findViewById(R.id.btnpending);
        Button btnFail = dialogView112.findViewById(R.id.btnFail);
        TextView tv_amount = dialogView112.findViewById(R.id.tv_amount);
        tv_amount.setText("Amount: \u20B9" + etAmount.getText());
        imageView.setImageBitmap(rotate_bitmap);
        iv_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewDialog112.dismiss();
                etAmount.setText("");
            }
        });
        btnSuccess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewDialog112.dismiss();
                etAmount.setText("");
                try {
                    if (gcmMessageDataSource == null) {
                        gcmMessageDataSource = new GcmMessageDataSource(getActivity());
                        gcmMessageDataSource.open();
                    }
                    String id = gcmMessageDataSource.getSingleColumnId(orderid);
                    gcmMessageDataSource.updateTransactionValue(Integer.parseInt(id), "success");
                } catch (Exception e) {
                    Log.e("TAG", "DB Exception update   " + e);
                }
                Toast.makeText(service, "Transaction SuccessFull.", Toast.LENGTH_SHORT).show();
                try {
                    InputStream input1 = getActivity().getAssets().open("success.bmp");
                    Bitmap bitmap = BitmapFactory.decodeStream(input1);
                    Bitmap resize_bitmap = Apputils.getResizedBitmap(bitmap, 320, 480);
                    send1(ToBmp16(resize_bitmap), true);
                } catch (Exception e) {
                    Log.e("TAG", "Exception   " + e);
                }
            }
        });
        btnFail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewDialog112.dismiss();
                etAmount.setText("");
                try {
                    if (gcmMessageDataSource == null) {
                        gcmMessageDataSource = new GcmMessageDataSource(getActivity());
                        gcmMessageDataSource.open();
                    }
                    String id = gcmMessageDataSource.getSingleColumnId(orderid);
                    gcmMessageDataSource.updateTransactionValue(Integer.parseInt(id), "fail");
                } catch (Exception e) {
                    Log.e("TAG", "DB Exception update   " + e);
                }
                Toast.makeText(service, "Transaction Failed.", Toast.LENGTH_SHORT).show();
                try {
                    InputStream input1 = getActivity().getAssets().open("fail.bmp");
                    Bitmap bitmap = BitmapFactory.decodeStream(input1);
                    Bitmap resize_bitmap = Apputils.getResizedBitmap(bitmap, 320, 480);
                    send1(ToBmp16(resize_bitmap), true);
                } catch (Exception e) {
                    Log.e("TAG", "Exception   " + e);
                }

            }
        });
        btnpending.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewDialog112.dismiss();
                etAmount.setText("");
                try {
                    if (gcmMessageDataSource == null) {
                        gcmMessageDataSource = new GcmMessageDataSource(getActivity());
                        gcmMessageDataSource.open();
                    }
                    String id = gcmMessageDataSource.getSingleColumnId(orderid);
                    gcmMessageDataSource.updateTransactionValue(Integer.parseInt(id), "pending");
                } catch (Exception e) {
                    Log.e("TAG", "DB Exception update   " + e);
                }
                Toast.makeText(service, "Transaction Pending.", Toast.LENGTH_SHORT).show();

                try {
                    InputStream input1 = getActivity().getAssets().open("pending.bmp");
                    Bitmap bitmap = BitmapFactory.decodeStream(input1);
                    Bitmap resize_bitmap = Apputils.getResizedBitmap(bitmap, 320, 480);
                    send1(ToBmp16(resize_bitmap), true);
                } catch (Exception e) {
                    Log.e("TAG", "Exception   " + e);
                }
            }
        });
    }

    private void dialogUpiSetting() {
        Dialog viewDialog112 = new Dialog(getActivity());
        viewDialog112.getWindow().setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND, WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        viewDialog112.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LayoutInflater lin1 = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialogView112 = lin1.inflate(R.layout.dialog_upisetting, null);
        viewDialog112.setContentView(dialogView112);
        viewDialog112.setCancelable(false);
        viewDialog112.getWindow().setLayout(android.view.ViewGroup.LayoutParams.FILL_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        viewDialog112.show();
        ImageView iv_close = dialogView112.findViewById(R.id.iv_close);
        Button btn_submit = dialogView112.findViewById(R.id.btn_submit);
        EditText et_upi = dialogView112.findViewById(R.id.et_upi);
        EditText et_payeename = dialogView112.findViewById(R.id.et_payeename);

        try {
            et_upi.setText(PrefManager.getPref(getActivity(), PrefManager.PREF_UPIID));
            et_payeename.setText(PrefManager.getPref(getActivity(), PrefManager.PREF_PAYEENAME));
        } catch (Exception e) {

        }
        iv_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewDialog112.dismiss();
            }
        });
        btn_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewDialog112.dismiss();
                if (TextUtils.isEmpty(et_upi.getText())) {
                    Toast.makeText(getActivity(), "Enter Valid UPI Id", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(et_payeename.getText())) {
                    Toast.makeText(getActivity(), "Enter Valid Payee Name", Toast.LENGTH_SHORT).show();
                    return;
                }
                PrefManager.saveBoolPref(getActivity(), PrefManager.PREF_ISUPI, true);
                PrefManager.savePref(getActivity(), PrefManager.PREF_UPIID, et_upi.getText().toString());
                PrefManager.savePref(getActivity(), PrefManager.PREF_PAYEENAME, et_payeename.getText().toString());
            }
        });
    }


    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_home, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.setting) {
            dialogUpiSetting();
        }
        return super.onOptionsItemSelected(item);
    }

    /*
     * Serial + UI
     */
    private void connect() {
        connect(null);
    }

    private void connect(Boolean permissionGranted) {
        UsbDevice device = null;
        UsbManager usbManager = (UsbManager) getActivity().getSystemService(Context.USB_SERVICE);
        for (UsbDevice v : usbManager.getDeviceList().values())
            if (v.getDeviceId() == deviceId) device = v;
        if (device == null) {
            status("connection failed: device not found");
            return;
        }
        UsbSerialDriver driver = UsbSerialProber.getDefaultProber().probeDevice(device);
        if (driver == null) {
            driver = CustomProber.getCustomProber().probeDevice(device);
        }
        if (driver == null) {
            ProbeTable customTable = new ProbeTable();
            customTable.addProduct(device.getVendorId(), device.getProductId(), CdcAcmSerialDriver.class);
            UsbSerialProber prober = new UsbSerialProber(customTable);
            driver = prober.probeDevice(device);
        }
        if (driver == null) {
            status("connection failed: no driver for device");
            return;
        }
        if (driver.getPorts().size() < portNum) {
            status("connection failed: not enough ports at device");
            return;
        }
        usbSerialPort = driver.getPorts().get(portNum);
        UsbDeviceConnection usbConnection = usbManager.openDevice(driver.getDevice());
        if (usbConnection == null && permissionGranted == null && !usbManager.hasPermission(driver.getDevice())) {
            int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
            PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(getActivity(), 0, new Intent(Constants.INTENT_ACTION_GRANT_USB), flags);
            usbManager.requestPermission(driver.getDevice(), usbPermissionIntent);
            return;
        }
        if (usbConnection == null) {
            if (!usbManager.hasPermission(driver.getDevice()))
                status("connection failed: permission denied");
            else status("connection failed: open failed");
            return;
        }

        connected = Connected.Pending;
        try {
            usbSerialPort.open(usbConnection);
            usbSerialPort.setParameters(baudRate, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            SerialSocket socket = new SerialSocket(getActivity().getApplicationContext(), usbConnection, usbSerialPort);
            service.connect(socket);
            // usb connect is not asynchronous. connect-success and connect-error are returned immediately from socket.connect
            // for consistency to bluetooth/bluetooth-LE app use same SerialListener and SerialService classes
            onSerialConnect();
        } catch (Exception e) {
            onSerialConnectError(e);
        }
    }

    private void disconnect() {
        connected = Connected.False;
        controlLines.stop();
        service.disconnect();
        usbSerialPort = null;
    }

    //    private void send(String str) {
//        if (connected != Connected.True) {
//            Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        try {
//            String msg;
//            byte[] data;
//            if (hexEnabled) {
//                StringBuilder sb = new StringBuilder();
//                TextUtil.toHexString(sb, TextUtil.fromHexString(str));
//                TextUtil.toHexString(sb, newline.getBytes());
//                msg = sb.toString();
//                data = TextUtil.fromHexString(msg);
//            } else {
//                msg = str;
//                data = (str + newline).getBytes();
//            }
//            SpannableStringBuilder spn = new SpannableStringBuilder(msg + '\n');
//            spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSendText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//            receiveText.append(spn);
//            service.write(data);
//        } catch (SerialTimeoutException e) {
//            status("write timeout: " + e.getMessage());
//        } catch (Exception e) {
//            onSerialIoError(e);
//        }
//    }
    private void send(String str, boolean type) {
        Log.e("TAG", "-----send------");
        if (connected != Connected.True) {
            Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String msg;
            byte[] data;
            if (type) {
                StringBuilder sb = new StringBuilder();
                TextUtil.toHexString(sb, TextUtil.fromHexString(str));
//                TextUtil.toHexString(sb, newline.getBytes());
                msg = sb.toString();
                data = TextUtil.fromHexString(msg);
                Log.e("TAG", "data  " + data.length);
            } else {
                msg = str;
                data = (str + newline).getBytes();
            }
            SpannableStringBuilder spn = new SpannableStringBuilder(msg + '\n');
            spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSendText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            receiveText.append(spn);
            service.write(data);
        } catch (SerialTimeoutException e) {
            Log.e("TAG", "e  " + e);
            status("write timeout: " + e.getMessage());
        } catch (Exception e) {
            Log.e("TAG", "Exception  " + e);

            onSerialIoError(e);
        }
    }


    private void receive(byte[] data) {
        if (hexEnabled) {
            receiveText.append(TextUtil.toHexString(data) + '\n');
        } else {
            String msg = new String(data);
            if (newline.equals(TextUtil.newline_crlf) && msg.length() > 0) {
                // don't show CR as ^M if directly before LF
                msg = msg.replace(TextUtil.newline_crlf, TextUtil.newline_lf);
                // special handling if CR and LF come in separate fragments
                if (pendingNewline && msg.charAt(0) == '\n') {
                    Editable edt = receiveText.getEditableText();
                    if (edt != null && edt.length() > 1)
                        edt.replace(edt.length() - 2, edt.length(), "");
                }
                pendingNewline = msg.charAt(msg.length() - 1) == '\r';
            }
            receiveText.append(TextUtil.toCaretString(msg, newline.length() != 0));
        }
    }

    void status(String str) {
        SpannableStringBuilder spn = new SpannableStringBuilder(str + '\n');
        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        receiveText.append(spn);
    }

    /*
     * SerialListener
     */
    @Override
    public void onSerialConnect() {
        status("connected");
        connected = Connected.True;
        if (controlLinesEnabled) controlLines.start();
    }

    @Override
    public void onSerialConnectError(Exception e) {
        status("connection failed: " + e.getMessage());
        disconnect();
    }

    @Override
    public void onSerialRead(byte[] data) {
        receive(data);
    }

    @Override
    public void onSerialIoError(Exception e) {
        status("connection lost: " + e.getMessage());
        disconnect();
    }

    class ControlLines {
        private static final int refreshInterval = 200; // msec

        private final Handler mainLooper;
        private final Runnable runnable;
        private final LinearLayout frame;
        private final ToggleButton rtsBtn, ctsBtn, dtrBtn, dsrBtn, cdBtn, riBtn;

        ControlLines(View view) {
            mainLooper = new Handler(Looper.getMainLooper());
            runnable = this::run; // w/o explicit Runnable, a new lambda would be created on each postDelayed, which would not be found again by removeCallbacks

            frame = view.findViewById(R.id.controlLines);
            rtsBtn = view.findViewById(R.id.controlLineRts);
            ctsBtn = view.findViewById(R.id.controlLineCts);
            dtrBtn = view.findViewById(R.id.controlLineDtr);
            dsrBtn = view.findViewById(R.id.controlLineDsr);
            cdBtn = view.findViewById(R.id.controlLineCd);
            riBtn = view.findViewById(R.id.controlLineRi);
            rtsBtn.setOnClickListener(this::toggle);
            dtrBtn.setOnClickListener(this::toggle);
        }

        private void toggle(View v) {
            ToggleButton btn = (ToggleButton) v;
            if (connected != Connected.True) {
                btn.setChecked(!btn.isChecked());
                Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
                return;
            }
            String ctrl = "";
            try {
                if (btn.equals(rtsBtn)) {
                    ctrl = "RTS";
                    usbSerialPort.setRTS(btn.isChecked());
                }
                if (btn.equals(dtrBtn)) {
                    ctrl = "DTR";
                    usbSerialPort.setDTR(btn.isChecked());
                }
            } catch (IOException e) {
                status("set" + ctrl + " failed: " + e.getMessage());
            }
        }

        private void run() {
            if (connected != Connected.True) return;
            try {
                EnumSet<UsbSerialPort.ControlLine> controlLines = usbSerialPort.getControlLines();
                rtsBtn.setChecked(controlLines.contains(UsbSerialPort.ControlLine.RTS));
                ctsBtn.setChecked(controlLines.contains(UsbSerialPort.ControlLine.CTS));
                dtrBtn.setChecked(controlLines.contains(UsbSerialPort.ControlLine.DTR));
                dsrBtn.setChecked(controlLines.contains(UsbSerialPort.ControlLine.DSR));
                cdBtn.setChecked(controlLines.contains(UsbSerialPort.ControlLine.CD));
                riBtn.setChecked(controlLines.contains(UsbSerialPort.ControlLine.RI));
                mainLooper.postDelayed(runnable, refreshInterval);
            } catch (IOException e) {
                status("getControlLines() failed: " + e.getMessage() + " -> stopped control line refresh");
            }
        }

        void start() {
            frame.setVisibility(View.VISIBLE);
            if (connected != Connected.True) return;
            try {
                EnumSet<UsbSerialPort.ControlLine> controlLines = usbSerialPort.getSupportedControlLines();
                if (!controlLines.contains(UsbSerialPort.ControlLine.RTS))
                    rtsBtn.setVisibility(View.INVISIBLE);
                if (!controlLines.contains(UsbSerialPort.ControlLine.CTS))
                    ctsBtn.setVisibility(View.INVISIBLE);
                if (!controlLines.contains(UsbSerialPort.ControlLine.DTR))
                    dtrBtn.setVisibility(View.INVISIBLE);
                if (!controlLines.contains(UsbSerialPort.ControlLine.DSR))
                    dsrBtn.setVisibility(View.INVISIBLE);
                if (!controlLines.contains(UsbSerialPort.ControlLine.CD))
                    cdBtn.setVisibility(View.INVISIBLE);
                if (!controlLines.contains(UsbSerialPort.ControlLine.RI))
                    riBtn.setVisibility(View.INVISIBLE);
                run();
            } catch (IOException e) {
                Toast.makeText(getActivity(), "getSupportedControlLines() failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

        void stop() {
            frame.setVisibility(View.GONE);
            mainLooper.removeCallbacks(runnable);
            rtsBtn.setChecked(false);
            ctsBtn.setChecked(false);
            dtrBtn.setChecked(false);
            dsrBtn.setChecked(false);
            cdBtn.setChecked(false);
            riBtn.setChecked(false);
        }
    }

}
