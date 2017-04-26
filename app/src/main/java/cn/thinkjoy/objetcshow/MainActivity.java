package cn.thinkjoy.objetcshow;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import cn.thinkjoy.sdk.SDKInitializer;

public class MainActivity extends AppCompatActivity {
    private ImageView img_welcome;
    private final int PERMISSIONS_CAMERA = 112;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        img_welcome = (ImageView) findViewById(R.id.img_welcome);
        Glide.with(this).load(R.drawable.bg_welcome).centerCrop().into(img_welcome);
        img_welcome.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.CAMERA},
                            PERMISSIONS_CAMERA);
                } else {
                    startActivityForResult(new Intent(MainActivity.this, FaceCameraActivity.class), 108);
                    MainActivity.this.finish();
                }
            }
        }, 3000);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startActivityForResult(new Intent(this, FaceCameraActivity.class), 108);
                    MainActivity.this.finish();
                }
                break;
        }
    }

    @Override
    protected void onResume() {
        SDKInitializer.onResume(getApplicationContext());
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
