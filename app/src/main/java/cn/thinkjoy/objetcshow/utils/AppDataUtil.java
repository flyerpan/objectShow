package cn.thinkjoy.objetcshow.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by whz on 2017/3/27.
 */

public class AppDataUtil {
    private SharedPreferences sp;
    private SharedPreferences.Editor editor;
    private static AppDataUtil util = null;

    public static AppDataUtil newInstance(Context context) {
        if (util == null) {
            util = new AppDataUtil(context);
        }
        return util;
    }

    public AppDataUtil(Context context) {
        sp = PreferenceManager.getDefaultSharedPreferences(context);
        editor = sp.edit();
    }

    /**
     * 设置置信度
     *
     * @param confidence 置信度
     */
    public void setConfidence(float confidence) {
        editor.putFloat("confidence", confidence);
        editor.commit();
    }

    /**
     * 获取置信度
     *
     * @return
     */
    public float getCondidence() {
        return sp.getFloat("confidence", 0.5f);
    }

    /**
     * 设置返回数据数量
     *
     * @param num
     */
    public void setNum(int num) {
        editor.putInt("num", num);
        editor.commit();
    }

    /**
     * 获取返回数据数量
     *
     * @return
     */
    public int getNum() {
        return sp.getInt("num", 1);
    }


    /**
     * 设置摄像头IP
     *
     * @param cameraIp
     */
    public void setCameraIp(String cameraIp) {
        editor.putString("cameraIp", cameraIp);
        editor.commit();
    }

    /**
     * 获取IP
     *
     * @return
     */
    public String getCameraIp() {
        return sp.getString("cameraIp", "192.168.0.243");
    }

    /**
     * 设置摄像头IP端口
     *
     * @param cameraPort
     */
    public void setCameraPort(String cameraPort) {
        editor.putString("cameraPort", cameraPort);
        editor.commit();
    }

    /**
     * 获取IP端口
     *
     * @return
     */
    public String getCameraPort() {
        return sp.getString("cameraPort", "8000");
    }

    /**
     * 设置摄像头账号名
     *
     * @param userName
     */
    public void setUserName(String userName) {
        editor.putString("userName", userName);
        editor.commit();
    }

    /**
     * 获取账号名
     *
     * @return
     */
    public String getUserName() {
        return sp.getString("userName", "admin");
    }


    /**
     * 设置摄像头账号名
     *
     * @param password
     */
    public void setPassword(String password) {
        editor.putString("password", password);
        editor.commit();
    }

    /**
     * 获取账号名
     *
     * @return
     */
    public String getPassword() {
        return sp.getString("password", "Thinkjoy2015");
    }

    /**
     * 设置摄像头Channel
     *
     * @param channel
     */
    public void setChannel(String channel) {
        editor.putString("channel", channel);
        editor.commit();
    }

    /**
     * 获取摄像头Channel
     *
     * @return
     */
    public String getChannel() {
        return sp.getString("channel", "1");
    }

}
