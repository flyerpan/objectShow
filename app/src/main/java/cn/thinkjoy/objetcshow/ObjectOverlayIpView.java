// Copyright (c) Philipp Wagner. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package cn.thinkjoy.objetcshow;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;

import com.orhanobut.logger.Logger;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import cn.thinkjoy.object.model.ObjectInfo;


/**
 * Created by whz on 5/20/2016.
 */

/**
 * This class is a simple View to display the faces.
 */
public class ObjectOverlayIpView extends View {

    //    private Paint mPaint;
    private Paint mTextPaint;
    private int mDisplayOrientation;
    private int mOrientation;
    private int previewWidth;
    private int previewHeight;
    private List<ObjectInfo> objectList = new ArrayList<>();
    private double fps;
    private boolean isFront = false;
    private boolean isStart = false;

    public ObjectOverlayIpView(Context context) {
        super(context);
        initialize();
    }

    public ObjectOverlayIpView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public ObjectOverlayIpView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ObjectOverlayIpView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize();
    }


    private void initialize() {
        // We want a green box around the face:
        DisplayMetrics metrics = getResources().getDisplayMetrics();

//        int stroke = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, metrics);
//        mPaint = new Paint();
//        mPaint.setAntiAlias(true);
//        mPaint.setDither(true);
//        mPaint.setColor(Color.GREEN);
//        mPaint.setStrokeWidth(stroke);
//        mPaint.setStyle(Paint.Style.STROKE);
//
        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setDither(true);
        int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15, metrics);
        mTextPaint.setTextSize(size);
        mTextPaint.setColor(Color.GREEN);
        mTextPaint.setStyle(Paint.Style.FILL);
    }

    public void setFPS(double fps) {
        this.fps = fps;
    }

    public void setFaces(List<ObjectInfo> objectList) {
        this.objectList = objectList;
        invalidate();
    }

    public void setStart(boolean start){
        this.isStart = start;
    }

    public void setOrientation(int orientation) {
        mOrientation = orientation;
    }

    public void setDisplayOrientation(int displayOrientation) {
        mDisplayOrientation = displayOrientation;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (objectList != null && objectList.size() > 0 && isStart) {

            float scaleX = (float) getWidth() / (float) previewWidth;
            float scaleY = (float) getHeight() / (float) previewHeight;

            switch (mDisplayOrientation) {
                case 90:
                case 270:
                    scaleX = (float) getWidth() / (float) previewHeight;
                    scaleY = (float) getHeight() / (float) previewWidth;
                    break;
            }

            /**
             * 画框
             */
            Paint mPaint = new Paint();
            mPaint.setAntiAlias(true);
            mPaint.setColor(Color.GREEN);
            mPaint.setDither(true);
            mPaint.setStrokeWidth(5);
            mPaint.setStyle(Paint.Style.STROKE);

            Paint textPaint = new Paint();
            textPaint.setColor(Color.RED);
            textPaint.setStyle(Paint.Style.FILL);
            textPaint.setTextSize(20);
            textPaint.setDither(true);
            textPaint.setAntiAlias(true);

            for (ObjectInfo object : objectList) {
                //根据中心点的x,y,宽和高得出物体的左上右上四个坐标
                float left = (float) (object.getX() - object.getWidth() / 2) * this.getWidth();
                float right = (float) (object.getX() + object.getWidth() / 2) * this.getWidth();
                float top = (float) (object.getY() - object.getHeight() / 2) * this.getHeight();
                float bottom = (float) (object.getY() + object.getHeight() / 2) * this.getHeight();
                canvas.save();
                canvas.drawRect(left, top, right, bottom, mPaint);
                canvas.drawText(object.getAlias(),(left + right) / 2, (top + bottom) / 2, mTextPaint);
            }
        }
        canvas.drawText("分辨率：" + previewWidth + "x" + previewHeight, mTextPaint.getTextSize(), mTextPaint.getTextSize(), mTextPaint);
        canvas.restore();
    }

    public void setPreviewWidth(int previewWidth) {
        this.previewWidth = previewWidth;
    }

    public void setPreviewHeight(int previewHeight) {
        this.previewHeight = previewHeight;
    }

    public void setFront(boolean front) {
        isFront = front;
    }
}