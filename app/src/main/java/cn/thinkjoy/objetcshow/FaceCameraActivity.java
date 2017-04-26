package cn.thinkjoy.objetcshow;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import cn.thinkjoy.object.imp.ObjectCheckListener;
import cn.thinkjoy.object.manage.ObjectManage;
import cn.thinkjoy.object.model.ErrorMsg;
import cn.thinkjoy.object.model.ObjectInfo;
import cn.thinkjoy.objetcshow.utils.IPCameraManager;
import cn.thinkjoy.objetcshow.utils.ImageUtils;
import cn.thinkjoy.objetcshow.utils.Util;
import cn.thinkjoy.sdk.SDKInitializer;


/**
 * Created by whz on 2016/11/11.
 */

public class FaceCameraActivity extends AppCompatActivity implements SurfaceHolder.Callback, Camera.PreviewCallback, View.OnClickListener {
    private SurfaceView mSurfaceView;
    private ObjectOverlayView mFaceView;  //物体界面
    private String BUNDLE_CAMERA_ID = "ObjectDemo";
    private List<ObjectInfo> faces = new ArrayList<>();  //物体识别结果
    private int numberOfCameras;   //摄像头数量
    private Camera mCamera;   //摄像头
    private Handler handler;
    private int cameraId = 0;
    private int previewHeight;
    private int previewWidth;
    private int prevSettingWidth;
    private int prevSettingHeight;
    private int mDisplayRotation;
    private int mDisplayOrientation;
    private int counter = 0;   //用于记录截取的图片数量，计算每秒的帧数
    private boolean isThreadWorking;    //标识人脸位置描绘线程是否正在进行
    private Thread detectThread;  //区域检测描绘线程
    private boolean isPost = false;
    private boolean isStart = false;  //是否开始识别
    private Button btn_start;
    private Button btn_camera;
    private Button btn_set;
    private PopupWindow window = null;

    //快速预览data -->bitmap argb565
    private RenderScript rs;
    private ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
    private Type.Builder yuvType, rgbaType;
    private Allocation in, out;

    //设置属性
    private float confidence = 0.5f;
    private int count_num = 100;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_face);

        mSurfaceView = (SurfaceView) findViewById(R.id.surface);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mFaceView = (ObjectOverlayView) findViewById(R.id.overlay);
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
            mFaceView.setStart(isStart);
        }

        handler = new Handler();

        if (savedInstanceState != null) {
            cameraId = savedInstanceState.getInt(BUNDLE_CAMERA_ID, 0);
        }

        rs = RenderScript.create(this);
        yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8(rs));
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        SurfaceHolder holder = mSurfaceView.getHolder();
        holder.addCallback(this);
        holder.setFormat(ImageFormat.NV21);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(BUNDLE_CAMERA_ID, cameraId);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startPreview();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }

    @Override
    protected void onDestroy() {
        SDKInitializer.onDestroy(getApplicationContext());
        super.onDestroy();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        numberOfCameras = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                if (cameraId == 0) cameraId = i;
            }
        }

        mCamera = Camera.open(cameraId);

        Camera.getCameraInfo(cameraId, cameraInfo);
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            mFaceView.setFront(true);
        }

        try {
            mCamera.setPreviewDisplay(mSurfaceView.getHolder());
        } catch (Exception e) {
            Log.e("test", "预览图像失败", e);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        //没有预览结果
        if (holder.getSurface() == null) {
            return;
        }
        //停止当前预览
        try {
            mCamera.stopPreview();
        } catch (Exception e) {

        }
        configureCamera(width, height);
        setDisplayOrientation();
        //启动摄像头预览
        startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mCamera.setPreviewCallbackWithBuffer(null);
        mCamera.setErrorCallback(null);
        mCamera.release();
        mCamera = null;
    }

    private void configureCamera(int width, int height) {
        Camera.Parameters parameters = mCamera.getParameters();
        //设置预览界面尺寸和自动对焦:
        setOptimalPreviewSize(parameters, width, height);
        setAutoFocus(parameters);
        //设置相机参数:
        mCamera.setParameters(parameters);
    }

    private void setOptimalPreviewSize(Camera.Parameters cameraParameters, int width, int height) {
        List<Camera.Size> previewSizes = cameraParameters.getSupportedPreviewSizes();
        float targetRatio = (float) width / height;
        Camera.Size previewSize = Util.getOptimalPreviewSize(this, previewSizes, targetRatio);
        previewWidth = previewSize.width;
        previewHeight = previewSize.height;

        /**
         * 计算大小，把全帧位图缩放到更小的位图，在缩放位图中检测到的面部比完整位图的高性能
         * 较小的图像大小- >检测速度更快，但距离检测面较短，所以计算尺寸遵循你的目的
         */
        if (previewWidth / 4 > 360) {
            prevSettingWidth = 360;
            prevSettingHeight = 270;
        } else if (previewWidth / 4 > 320) {
            prevSettingWidth = 320;
            prevSettingHeight = 240;
        } else if (previewWidth / 4 > 240) {
            prevSettingWidth = 240;
            prevSettingHeight = 160;
        } else {
            prevSettingWidth = 160;
            prevSettingHeight = 120;
        }

        cameraParameters.setPreviewSize(previewSize.width, previewSize.height);

//        cameraParameters.setPreviewFpsRange(1, 1);                               // 设置预览照片时每秒显示多少帧的最小值和最大值
        mFaceView.setPreviewWidth(previewWidth);
        mFaceView.setPreviewHeight(previewHeight);
    }

    private void setAutoFocus(Camera.Parameters cameraParameters) {
        List<String> focusModes = cameraParameters.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
            cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
    }

    private void setDisplayOrientation() {
        //设置显示方向
        mDisplayRotation = Util.getDisplayRotation(FaceCameraActivity.this);
        mDisplayOrientation = Util.getDisplayOrientation(mDisplayRotation, cameraId);

        mCamera.setDisplayOrientation(mDisplayOrientation);

        if (mFaceView != null) {
            mFaceView.setDisplayOrientation(mDisplayOrientation);
        }
    }


    private void startPreview() {
        if (mCamera != null) {
            isThreadWorking = false;
            mCamera.startPreview();
            mCamera.setPreviewCallback(this);
            counter = 0;
        }
    }

    private void waitForFdetThreadComplete() {
        if (detectThread == null) {
            return;
        }

        if (detectThread.isAlive()) {
            try {
                detectThread.join();
                detectThread = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }


    long start, end;  //开始检测时间与结束时间
    double fps;
    private int Id = 0;
    private Bitmap faceCroped;  //截取人脸图片
    private Bitmap bmp;

    @Override
    public void onPreviewFrame(final byte[] data, final Camera camera) {
        if (!isThreadWorking && isStart) {
            if (counter == 0)
                start = System.currentTimeMillis();

            isThreadWorking = true;
            waitForFdetThreadComplete();
            detectThread = new Thread(new Runnable() {
                @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                @Override
                public void run() {

                    /**
                     * 快速data-->bitmap
                     */
                    if (yuvType == null) {
                        yuvType = new Type.Builder(rs, Element.YUV(rs)).setX(data.length);
                        in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);

                        rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(previewWidth).setY(previewHeight);
                        out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);
                    }
                    in.copyFrom(data);
                    yuvToRgbIntrinsic.setInput(in);
                    yuvToRgbIntrinsic.forEach(out);

                    Bitmap bitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
                    out.copyTo(bitmap);

                    float aspect = (float) previewHeight / (float) previewWidth;
                    int w = prevSettingWidth;
                    int h = (int) (prevSettingWidth * aspect);

//                    Bitmap bitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.RGB_565);
//                    //将图像从nv21转到rgb_565
//                    YuvImage yuv = new YuvImage(data, ImageFormat.NV21,
//                            bitmap.getWidth(), bitmap.getHeight(), null);
//                    Rect rectImage = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
//                    ByteArrayOutputStream baout = new ByteArrayOutputStream();
//                    if (!yuv.compressToJpeg(rectImage, 100, baout)) {
//                        Log.e("CreateBitmap", "compressToJpeg failed");
//                    }
//
//                    BitmapFactory.Options bfo = new BitmapFactory.Options();
//                    bfo.inPreferredConfig = Bitmap.Config.RGB_565;
//                    bitmap = BitmapFactory.decodeStream(
//                            new ByteArrayInputStream(baout.toByteArray()), null, bfo);
                    bmp = Bitmap.createScaledBitmap(bitmap, 448, 448, false);
                    if (cameraId != 0) {
                        bmp = ImageUtils.reverseBitmap(bitmap, 0);
                    }

                    Camera.CameraInfo info = new Camera.CameraInfo();
                    Camera.getCameraInfo(cameraId, info);
                    int rotate = mDisplayOrientation;
                    if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT && mDisplayRotation % 180 == 0) {
                        if (rotate + 180 > 360) {
                            rotate = rotate - 180;
                        } else
                            rotate = rotate + 180;
                    }

                    switch (rotate) {
                        case 90:
                            bmp = ImageUtils.rotate(bmp, 90);
                            break;
                        case 180:
                            bmp = ImageUtils.rotate(bmp, 180);
                            break;
                        case 270:
                            bmp = ImageUtils.rotate(bmp, 270);
                            break;
                    }
                    handler.post(new Runnable() {
                        public void run() {
                            if (!isPost) {

                                isPost = true;
                                final long time = System.currentTimeMillis();
                                ObjectManage.newInstance(FaceCameraActivity.this).getObjectRegion(bmp, count_num, confidence, new ObjectCheckListener() {
                                    @Override
                                    public void onObjectCheck(List<ObjectInfo> objectInfos, ErrorMsg error) {
                                        Log.i("test", "time:" + (System.currentTimeMillis() - time));
                                        isPost = false;
                                        if (error.getCode() == 0) {
                                            //传输数据绘制矩形
                                            faces = objectInfos;
                                            mFaceView.setFaces(faces);
                                        } else {
                                            mFaceView.setFaces(null);
                                        }
                                        //计算FPS
                                        end = System.currentTimeMillis();
                                        counter++;
                                        double time = (double) (end - start) / 1000;
                                        if (time != 0)
                                            fps = counter / time;

                                        mFaceView.setFPS(fps);

                                        if (counter == (Integer.MAX_VALUE - 1000))
                                            counter = 0;

                                        isThreadWorking = false;
                                    }
                                });
                            }
                        }
                    });
                }
            });
            detectThread.start();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_camera:
//                if (numberOfCameras == 1) {
//                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
//                    builder.setTitle("选择摄像头").setMessage("您的设备只有一个摄像头").setNeutralButton("确定", null);
//                    AlertDialog alert = builder.create();
//                    alert.show();
//                    return;
//                }
//
//                cameraId = (cameraId + 1) % numberOfCameras;
//                recreate();
                Intent intent = new Intent(this,IPCameraActivity.class);
                intent.putExtra("start",isStart);
                intent.putExtra("confidence",confidence);
                intent.putExtra("count_num",count_num);
                startActivity(intent);
                this.finish();
                break;
            case R.id.btn_start:
                if (isStart) {
                    mFaceView.setFaces(null);
                    mFaceView.setStart(false);
                    isStart = false;
                    btn_start.setText("开始识别");
                } else {
                    isStart = true;
                    mFaceView.setStart(true);
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
        window.showAtLocation(mSurfaceView, Gravity.CENTER, 0, 0);

    }

}
