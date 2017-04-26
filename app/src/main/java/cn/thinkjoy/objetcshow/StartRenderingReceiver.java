package cn.thinkjoy.objetcshow;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.orhanobut.logger.Logger;
import cn.thinkjoy.objetcshow.utils.IPCameraManager;

/**
 * Created by hebin
 * on 2017/3/16 0016.
 */

public class StartRenderingReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (IPCameraManager.ACTION_START_RENDERING.equals(intent.getAction())) {
            Logger.i("接受广播");

        }
        if (IPCameraManager.ACTION_DVR_OUTLINE.equals(intent.getAction())) {
            Logger.i("预览离线");

        }
    }

}
