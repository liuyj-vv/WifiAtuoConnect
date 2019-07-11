package com.changhong.wifi.auto.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

public class WifiReceiver extends BroadcastReceiver {
    String allAction = "";
    String TAG = WifiReceiver.class.getPackage().getName();
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.e(TAG, Thread.currentThread().getStackTrace()[2].getMethodName()+"["+Thread.currentThread().getStackTrace()[2].getLineNumber()+"]"
                + " action: "+ action);



        if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) { // 这个监听wifi的打开与关闭，与wifi的连接无关
            int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
            switch (wifiState) {
                case WifiManager.WIFI_STATE_DISABLING:
                    allAction += action + ", 正在关闭"+"\n";
                    break;
                case WifiManager.WIFI_STATE_DISABLED:
                    allAction += action + ", 已关闭"+"\n";
                    break;
                case WifiManager.WIFI_STATE_ENABLING:
                    allAction += action + ", 正在打开"+"\n";
                    break;
                case WifiManager.WIFI_STATE_ENABLED:
                    allAction += action + ", 已打开"+"\n";
                    break;
                case WifiManager.WIFI_STATE_UNKNOWN:
                    allAction += action + ", 未知"+"\n";
                    break;
            }
        } else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
            allAction += action + ", 扫描变化"+"\n";
            MainActivity.mainActivity.mapListRefresh();
        } else {
            allAction += action + "\n";
        }














        MainActivity.mainActivity.textview_log.setText(allAction);
    }
}
