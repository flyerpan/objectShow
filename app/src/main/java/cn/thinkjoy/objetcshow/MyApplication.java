package cn.thinkjoy.objetcshow;

import android.app.Application;

import cn.thinkjoy.sdk.SDKInitializer;

/**
 * Created by whz on 2016/11/10.
 */

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        SDKInitializer.init(this);
        SDKInitializer.setRunStatus(this,1);
    }
}
