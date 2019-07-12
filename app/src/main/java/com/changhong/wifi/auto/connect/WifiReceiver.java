package com.changhong.wifi.auto.connect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

class WifiReceiver extends BroadcastReceiver {
    String TAG = WifiReceiver.class.getPackage().getName();
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) { // 这个监听wifi的打开与关闭，与wifi的连接无关
            int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);//当前的状态
            int wifiPreviousState =  intent.getIntExtra(WifiManager.EXTRA_PREVIOUS_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);//之前的状态

            Log.i(TAG, "硬件状态(WIFI_STATE_CHANGED_ACTION): " + wifiState);
        } else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
            //扫描到一个热点, 并且此热点达可用状态 会触发此广播
            //你可以从intent中取出一个boolean值; 如果此值为true, 代表着扫描热点已完全成功; 为false, 代表此次扫描不成功, ScanResult 距离上次扫描并未得到更新;
            boolean b = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);

            Log.i(TAG, "扫描结果(SCAN_RESULTS_AVAILABLE_ACTION): " + b);
        } else if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(action)) {
            SupplicantState supplicantState = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE); //// 获取当前网络新状态.
            int error = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, 0); //// 获取当前网络连接状态码.

            Log.i(TAG, "连接验证(SUPPLICANT_STATE_CHANGED_ACTION): " + supplicantState + ", error: " + error);
        } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
            //这三个方法能够获取手机当前连接的Wifi信息，注意在wifi断开时Intent中不包含WifiInfo对象，却包含bssid。
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            WifiInfo wifiInfo = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
            String bssid = intent.getStringExtra(WifiManager.EXTRA_BSSID);


            Log.i(TAG, "网络状态(NETWORK_STATE_CHANGED_ACTION), " + networkInfo + "。 " + wifiInfo + "。 bssid: " + bssid);
        } else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
            //ConnectivityManager.EXTRA_NO_CONNECTIVITY 返回true，代表未连接
            boolean b = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
            NetworkInfo networkInfo = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);


            Log.i(TAG, "连接变化(CONNECTIVITY_ACTION), b: " + b + "。 " + networkInfo);
        } else {
            Log.i(TAG, "错误匹配, 未处理的广播: " +action);

        }
    }
}
