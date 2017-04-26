package cn.thinkjoy.objetcshow;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;

import com.orhanobut.logger.Logger;

import cn.thinkjoy.objetcshow.utils.AppDataUtil;
import cn.thinkjoy.objetcshow.utils.IPCameraManager;
import cn.thinkjoy.objetcshow.utils.ScreenUtil;
import entity.DeviceInfo;

/**
 * Created by whz on 2017/4/6.
 */

public class IPCameraActivity extends Activity implements View.OnClickListener {
    private SurfaceView surfaceView;
    private ObjectOverlayIpView overlayView;
    private Button btn_start;
    private Button btn_camera;
    private Button btn_set;
    private final StartRenderingReceiver receiver = new StartRenderingReceiver();
    private boolean isStart = false;
    private PopupWindow window;

    //设置属性
    private float confidence = 0.5f;
    private int count_num = 100;


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ScreenUtil.setFullScreen(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//保持屏幕常亮
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_ip);

        surfaceView = (SurfaceView) findViewById(R.id.surface);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        overlayView = (ObjectOverlayIpView) findViewById(R.id.overlay);
        btn_start = (Button) findViewById(R.id.btn_start);
        btn_camera = (Button) findViewById(R.id.btn_camera);
        btn_set = (Button) findViewById(R.id.btn_set);

        btn_start.setOnClickListener(this);
        btn_camera.setOnClickListener(this);
        btn_set.setOnClickListener(this);

        if (getIntent() != null){
            isStart = getIntent().getBooleanExtra("start",false);
            confidence = getIntent().getFloatExtra("confidence",0.5f);
            count_num = getIntent().getIntExtra("count_num",100);
            if (isStart){
                btn_start.setText("停止识别");
            }else {
                btn_start.setText("开始识别");
            }
        }


        // 设置用于发广播的上下文
        IPCameraManager.getInstance().setContext(getApplicationContext());
        IPCameraManager.getInstance().setActivity(IPCameraActivity.this);


        DisplayMetrics displayMetrics = ScreenUtil.getScreenWH(this);
        Logger.i("当前显示设备 w  = " + displayMetrics.widthPixels + "   h = " + displayMetrics.heightPixels);

        overlayView.setPreviewWidth(1920);
        overlayView.setPreviewHeight(1080);

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {

            @Override
            public void surfaceCreated(SurfaceHolder holder) {

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                startPlay();
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                surfaceView.destroyDrawingCache();
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        IPCameraManager.getInstance().clearFaceCorp();
    }

    @Override
    public void onBackPressed() {

        super.onBackPressed();
//        new Thread() {
//            @Override
//            public void run() {
//                IPCameraManager.getInstance().stopPlay();
//            }
//        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        new Thread() {
            @Override
            public void run() {
                isStart = false;
                IPCameraManager.getInstance().setStart(isStart);
                IPCameraManager.getInstance().stopPlay();
                IPCameraManager.getInstance().logoutDevice();
                IPCameraManager.getInstance().freeSDK();
                IPCameraManager.getInstance().stopTimer();
                IPCameraManager.getInstance().clearHandler();
            }
        }.start();

    }

    private void startPlay() {

        IntentFilter filter = new IntentFilter();
        filter.addAction(IPCameraManager.ACTION_START_RENDERING);
        filter.addAction(IPCameraManager.ACTION_DVR_OUTLINE);
        registerReceiver(receiver, filter);

        new Thread() {
            @Override
            public void run() {
                IPCameraManager.getInstance().setDeviceBean(getDeviceBean());
                IPCameraManager.getInstance().setSurfaceHolder(surfaceView.getHolder());
                IPCameraManager.getInstance().setOverlayView(overlayView);
                IPCameraManager.getInstance().setStart(isStart);
                IPCameraManager.getInstance().initSDK();
                IPCameraManager.getInstance().loginDevice();
                IPCameraManager.getInstance().realPlay();
            }
        }.start();
    }


    private DeviceInfo getDeviceBean() {
        DeviceInfo bean = new DeviceInfo();
        AppDataUtil data = AppDataUtil.newInstance(this);
        bean.setIP(data.getCameraIp());
        bean.setPort(data.getCameraPort());
        bean.setUserName(data.getUserName());
        bean.setPassWord(data.getPassword());
        bean.setChannel(data.getChannel());
        return bean;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_camera:
                Intent intent = new Intent(this,FaceCameraActivity.class);
                intent.putExtra("start",isStart);
                intent.putExtra("confidence",confidence);
                intent.putExtra("count_num",count_num);
                startActivity(intent);
                this.finish();
                break;
            case R.id.btn_start:
                if (isStart) {
                    isStart = false;
                    IPCameraManager.getInstance().setStart(isStart);
                    overlayView.setFaces(null);
                    btn_start.setText("开始识别");
                } else {
                    isStart = true;
                    IPCameraManager.getInstance().setStart(isStart);
                    btn_start.setText("停止识别");
                }

                break;
            case R.id.btn_set:
                showSetWindow();
                break;
        }
    }

    public void showSetWindow() {
        if (window == null) {
            View view = LayoutInflater.from(this).inflate(R.layout.window_set, null);
            window = new PopupWindow(view, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        }

        window.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                window.dismiss();
                window = null;
            }
        });

        final SeekBar sb_confidence = (SeekBar) window.getContentView().findViewById(R.id.sb_confidence);
        final SeekBar sb_num = (SeekBar) window.getContentView().findViewById(R.id.sb_num);
        final TextView tv_confidence = (TextView) window.getContentView().findViewById(R.id.tv_confidence);
        final TextView tv_num = (TextView) window.getContentView().findViewById(R.id.tv_num);
        Button btn_commit = (Button) window.getContentView().findViewById(R.id.btn_commit);

        sb_confidence.setProgress((int) (confidence * 10) - 1);
        sb_num.setProgress(count_num);
        tv_confidence.setText("置信度：" + ((float) (sb_confidence.getProgress() + 1) / 10));
        tv_num.setText("最大返回数量：" + (sb_num.getProgress() + 1));
        sb_confidence.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int min_progress = progress + 1;
                tv_confidence.setText("置信度：" + ((float) min_progress / 10));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        sb_num.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int min_progress = progress + 1;
                tv_num.setText("最大返回数量：" + min_progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        btn_commit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confidence = ((float) (sb_confidence.getProgress() + 1) / 10);
                count_num = sb_num.getProgress() + 1;
                IPCameraManager.getInstance().setConfidence(confidence,count_num);
                window.dismiss();
                window = null;
            }
        });


        // 使其聚集
        window.setFocusable(true);
        // 设置允许在外点击消失
        window.setOutsideTouchable(true);
        // 这个是为了点击“返回Back”也能使其消失，并且并不会影响你的背景
        window.setBackgroundDrawable(new BitmapDrawable());
        // 显示的位置为:屏幕的宽度的一半-PopupWindow的高度的一半
//        popupWindow.showAsDropDown(parent, 0, 10);// 下移10dp
        window.showAtLocation(surfaceView, Gravity.CENTER, 0, 0);

    }


}
