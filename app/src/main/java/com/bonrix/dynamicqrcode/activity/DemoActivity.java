package com.bonrix.dynamicqrcode.activity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.bonrix.dynamicqrcode.Apputils;
import com.bonrix.dynamicqrcode.AsyncResponse;
import com.bonrix.dynamicqrcode.BitmapConvertor;
import com.bonrix.dynamicqrcode.R;

public class DemoActivity extends AppCompatActivity {
    ImageView imageView;
    Button submit;
    AsyncResponse asyncResponse;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);
        imageView = findViewById(R.id.imageview);
        submit = findViewById(R.id.submit);

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bitmap bitmap = Apputils.drawableToBitmap(getResources().getDrawable(R.drawable.ic_new_qrcode));
                Bitmap resize_bitmap = Apputils.getResizedBitmap(bitmap, 192, 192);
                Bitmap rotate_bitmap = Apputils.RotateBitmap(resize_bitmap, 180);
                BitmapConvertor bitmapConvertor = new BitmapConvertor(DemoActivity.this);
//                bitmapConvertor.convertBitmap(rotate_bitmap,);
                Log.e("TAG", String.valueOf(bitmapConvertor.mRawBitmapData.length));

//
//                Log.e("TAG", "dest " + dest);

            }
        });
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
//                    send("0207190202", true);
                    InputStream input = getAssets().open("bbbbb.bmp");
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    byte[] buffer = new byte[65536];
                    int l;
                    while ((l = input.read(buffer)) > 0)
                        output.write(buffer, 0, l);
                    input.close();
                    output.close();
                    Log.e("TAG", String.valueOf(output.toByteArray().length));

                    byte[] result = new byte[output.size()];
                    byte[] main = output.toByteArray();
                    for (int i = 0; i < main.length; i++) {
                        result[i] = (byte) (main[i] ^ (byte) 255);
                    }
                    Log.e("TAG", "byteArrayToHexString " + Apputils.byteArrayToHexString(output.toByteArray()));
                    Log.e("TAG", "byteArrayToHexString 1     " + Apputils.byteArrayToHexString(result));

//
//
//                    send(Apputils.byteArrayToHexString(output.toByteArray()), true);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });


    }


}
