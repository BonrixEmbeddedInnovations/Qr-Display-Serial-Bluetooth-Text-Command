package com.bonrix.dynamicqrcode;

import static android.content.Context.WINDOW_SERVICE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
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
import android.graphics.ColorSpace;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bonrix.dynamicqrcode.prefrence.PrefManager;
import com.bonrix.dynamicqrcode.sqlite.GcmMessageDataSource;
import com.google.zxing.WriterException;
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
import java.nio.ByteBuffer;
import java.util.EnumSet;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;

public class TerminalFragment extends Fragment implements ServiceConnection, SerialListener {

    private enum Connected {False, Pending, True}

    private final BroadcastReceiver broadcastReceiver;
    private int deviceId, portNum, baudRate;
    private UsbSerialPort usbSerialPort;
    private SerialService service;

    private TextView receiveText;
    private ImageView imageview;
    private TextView sendText;
    private ControlLines controlLines;
    private TextUtil.HexWatcher hexWatcher;

    private Connected connected = Connected.False;
    private boolean initialStart = true;
    private boolean hexEnabled = false;
    private boolean controlLinesEnabled = false;
    private boolean pendingNewline = false;
    private String newline = TextUtil.newline_crlf;
    private Button btnGenerateQr, btnshowqr;
    public int sendTime = -1;
    QRGEncoder qrgEncoder;

    public TerminalFragment() {
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
        deviceId = getArguments().getInt("device");
        portNum = getArguments().getInt("port");
        baudRate = getArguments().getInt("baud");
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

    /*
     * UI
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terminal, container, false);
        receiveText = view.findViewById(R.id.receive_text);                          // TextView performance decreases with number of spans
        imageview = view.findViewById(R.id.imageview);                          // TextView performance decreases with number of spans
        receiveText.setTextColor(getResources().getColor(R.color.colorRecieveText)); // set as default color to reduce number of spans
        receiveText.setMovementMethod(ScrollingMovementMethod.getInstance());

        sendText = view.findViewById(R.id.send_text);
        hexWatcher = new TextUtil.HexWatcher(sendText);
        hexWatcher.enable(hexEnabled);
        sendText.addTextChangedListener(hexWatcher);
        sendText.setHint(hexEnabled ? "HEX mode" : "");
        View sendBtn = view.findViewById(R.id.send_btn);
        sendBtn.setOnClickListener(v -> send(sendText.getText().toString(), true));
        controlLines = new ControlLines(view);
        btnshowqr = view.findViewById(R.id.btnshowqr);
        btnGenerateQr = view.findViewById(R.id.btnGenerateQr);
        btnshowqr.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    InputStream input1 = getActivity().getAssets().open("paytm_home.bmp");
                    Bitmap bitmap = BitmapFactory.decodeStream(input1);
                    Bitmap resize_bitmap = Apputils.getResizedBitmap(bitmap, 320, 480);
                    send1(ToBmp16(resize_bitmap), true);
                } catch (Exception e) {
                    Log.e("TAG", "Exception   " + e);
                }
            }
        });
        btnGenerateQr.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                displayTxnQr();
            }
        });
        return view;
    }

    private void displayTxnQr() {
        String orderid = Apputils.getCurrnetDateTime2();
        String upistring = Apputils.getUpiString("shraddhatradelink@yesbank", "Shraddha", "10".toString(), orderid);
//        String upistring = Apputils.getUpiString(PrefManager.getPref(getActivity(), PrefManager.PREF_UPIID).trim(), PrefManager.getPref(getActivity(), PrefManager.PREF_PAYEENAME).trim(), etAmount.getText().toString(), orderid);
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

                Bitmap bitmap_text = textAsBitmap("To Pay\n Rs 10", 10);
                Bitmap bitmap_topay = getBitmapToPay();
                Bitmap finalbmp = mergeToPin(bitmap_topay, bitmap_qr, bitmap_text);
                Bitmap resize_bitmap = Apputils.getResizedBitmap(finalbmp, 320, 480);
                send1(ToBmp16(resize_bitmap), true);

                imageview.setImageBitmap(resize_bitmap);

            } catch (Exception e) {
                Log.e("TAG", "Exception  " + e);
                e.printStackTrace();
            }
        }
    }

    public Bitmap mergeToPin(Bitmap bitmap_topay, Bitmap bitmap_qr, Bitmap bitmap_text) {
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

        Typeface tf = Typeface.createFromAsset(getActivity().getAssets(),"OpenSans-Bold.ttf");
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.BLUE);
        paint.setTextSize((35f));
        paint.setTypeface(tf);
        canvas.drawText("Rs 100", ((w_bitmap_topay - 100) / 2), 115f, paint);
        return result;
    }

    public Bitmap getBitmapToPay() throws IOException {
        AssetManager assetManager = getActivity().getAssets();

        InputStream istr = assetManager.open("topay.bmp");
        Bitmap bitmap = BitmapFactory.decodeStream(istr);
        istr.close();
        return bitmap;
    }

    public Bitmap textAsBitmap(String text, float textSize) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(textSize);
        paint.setColor(Color.BLACK);
        paint.setTextAlign(Paint.Align.CENTER);
        float baseline = -paint.ascent(); // ascent() is negative
        int width = (int) (paint.measureText(text) + 0.5f); // round
        int height = (int) (baseline + paint.descent() + 0.5f);
        Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        canvas.drawText(text, 0, baseline, paint);
        return image;
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
//    private byte[] ToBmp16(Bitmap bitmap) {
//        byte[] numArray = new byte[0];
//        try {
////            Bitmap bitmap = new Bitmap(filename);
//           numArray = new byte[bitmap.getWidth() * bitmap.getHeight() * 2];
//            short num = 0;
//            short num1 = 0;
//            int num2 = 0;
//            for (int i = 0; i < bitmap.getHeight(); i++) {
//                Log.e("TAG","i  "+i);
//                for (int j = 0; j < bitmap.getWidth(); j++) {
//                    Log.e("TAG","j  "+j);
//                    @SuppressLint({"NewApi", "LocalSuppress"})
//                    Color pixel = Color.valueOf(bitmap.getPixel(j, bitmap.getHeight() - i - 1));
//                    byte r = (byte) (pixel.RED >> 3 & 31);
//                    pixel = Color.valueOf(bitmap.getPixel(j, bitmap.getHeight() - i - 1));
//                    byte g = (byte) (pixel.GREEN >> 2 & 63);
//                    pixel = Color.valueOf(bitmap.getPixel(j, bitmap.getHeight() - i - 1));
//                    byte b = (byte) (pixel.BLUE >> 3 & 31);
//                    short num3 = (short) (r << 11);
//                    num1 = (short) (g << 5);
//                    num = (short) (num3 | num1 | b);
//                    numArray[num2] = (byte) (num >> 8);
//                    numArray[num2 + 1] = (byte) num;
//                    num2 += 2;
//
//                    Log.e("TAG","num  "+num);
//                    Log.e("TAG","num1  "+num1);
//                    Log.e("TAG","num2  "+num2);
//
//                }
//            }
//        } catch (Exception e) {
//            Log.e("TAG", "Exception  " + e);
//        }
////
////        bitmap.Dispose();
//        Log.e("TAG", "numArray  " + numArray.length);
//        return numArray;
//    }


    public Bitmap toBinary(Bitmap bmpOriginal) {
        int width, height, threshold;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();
        threshold = 127;
        Bitmap bmpBinary = Bitmap.createBitmap(bmpOriginal);

        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                // get one pixel color
                int pixel = bmpOriginal.getPixel(x, y);
                int red = Color.red(pixel);

                //get binary value
                if (red < threshold) {
                    bmpBinary.setPixel(x, y, 0xFF000000);
                } else {
                    bmpBinary.setPixel(x, y, 0xFFFFFFFF);
                }

            }
        }
        return bmpBinary;
    }

    public String convertByteArrayToString(byte[] byteArray) {
        String value = new String(byteArray);
        Log.e("TAG", value);

        return value;
    }

    public static byte[] convertBitmapToByteArray(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(bitmap.getByteCount());
        bitmap.copyPixelsToBuffer(byteBuffer);
        byteBuffer.rewind();
        return byteBuffer.array();
    }

    public String getURLForResource(int resourceId) {
        //use BuildConfig.APPLICATION_ID instead of R.class.getPackage().getName() if both are not same
        return Uri.parse("android.resource://" + R.class.getPackage().getName() + "/" + resourceId).toString();
    }


    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_terminal, menu);
        menu.findItem(R.id.hex).setChecked(hexEnabled);
        menu.findItem(R.id.controlLines).setChecked(controlLinesEnabled);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.clear) {
            receiveText.setText("");
            return true;
        } else if (id == R.id.newline) {
            String[] newlineNames = getResources().getStringArray(R.array.newline_names);
            String[] newlineValues = getResources().getStringArray(R.array.newline_values);
            int pos = java.util.Arrays.asList(newlineValues).indexOf(newline);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Newline");
            builder.setSingleChoiceItems(newlineNames, pos, (dialog, item1) -> {
                newline = newlineValues[item1];
                dialog.dismiss();
            });
            builder.create().show();
            return true;
        } else if (id == R.id.hex) {
            hexEnabled = !hexEnabled;
            sendText.setText("");
            hexWatcher.enable(hexEnabled);
            sendText.setHint(hexEnabled ? "HEX mode" : "");
            item.setChecked(hexEnabled);
            return true;
        } else if (id == R.id.controlLines) {
            controlLinesEnabled = !controlLinesEnabled;
            item.setChecked(controlLinesEnabled);
            if (controlLinesEnabled) {
                controlLines.start();
            } else {
                controlLines.stop();
            }
            return true;
        } else if (id == R.id.sendBreak) {
            try {
                usbSerialPort.setBreak(true);
                Thread.sleep(100);
                status("send BREAK");
                usbSerialPort.setBreak(false);
            } catch (Exception e) {
                status("send BREAK failed: " + e.getMessage());
            }
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
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

    private void send1(byte[] result, boolean type) {
        Log.e("TAG", "-----send------");
        if (connected != Connected.True) {
            Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String msg;
            byte[] data;
//            if (type) {
//                StringBuilder sb = new StringBuilder();
//                TextUtil.toHexString(sb, TextUtil.fromHexString(str));
//                msg = sb.toString();
//                data = TextUtil.fromHexString(msg);
//                Log.e("TAG", "data  " + data.length);
//            } else {
//                msg = str;
//                data = (str + newline).getBytes();
//            }
//            SpannableStringBuilder spn = new SpannableStringBuilder(msg + '\n');
//            spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSendText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//            receiveText.append(spn);
            service.write(result);
            Log.e("TAG", "sent.....");

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
