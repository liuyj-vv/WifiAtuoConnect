package com.changhong.wifi.auto.connect;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.util.List;

class WifiReceiver extends BroadcastReceiver {
    String TAG = WifiReceiver.class.getPackage().getName();
    WifiManager wifiManager;
    ConnectivityManager connectivityManager;
    static WifiAutoConnectHelper wifiAutoConnectHelper = new WifiAutoConnectHelper();

    static boolean isBootFristRun = true;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        connectivityManager = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) { // 这个监听wifi的打开与关闭，与wifi的连接无关
            Log.i(TAG, "硬件状态(WIFI_STATE_CHANGED_ACTION)");
            wifiAutoConnectHelper.handlerWifiState(wifiManager);

        } else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
            //扫描到一个热点, 并且此热点达可用状态会触发此广播
            //你可以从intent中取出一个boolean值; 如果此值为true, 代表着扫描热点已完全成功; 为false, 代表此次扫描不成功, ScanResult 距离上次扫描并未得到更新（可能存在）;
            Log.i(TAG, "扫描结果(SCAN_RESULTS_AVAILABLE_ACTION) ");
            wifiAutoConnectHelper.handlerScanResults(wifiManager, connectivityManager);

        } else if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(action)) {
            SupplicantState supplicantState = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE); //// 获取当前网络新状态.
            int error = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1);      //// 获取当前网络连接状态码.
            Log.i(TAG, "正在验证身份(SUPPLICANT_STATE_CHANGED_ACTION): " + supplicantState + " " + error);

            wifiAutoConnectHelper.handlerSupplicant(context, wifiManager, connectivityManager, supplicantState, error);

        } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
            //这三个方法能够获取手机当前连接的Wifi信息，注意在wifi断开时Intent中不包含WifiInfo对象，却包含bssid。
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            String bssid = intent.getStringExtra(WifiManager.EXTRA_BSSID);
            WifiInfo wifiInfo = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
            Log.i(TAG, "正在获取网络信息(SUPPLICANT_STATE_CHANGED_ACTION)");
            wifiAutoConnectHelper.handlefNetwork(wifiManager, connectivityManager, networkInfo, wifiInfo, bssid);

        } else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
            //ConnectivityManager.EXTRA_NO_CONNECTIVITY 返回true，代表未连接
            boolean b = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
            NetworkInfo networkInfo = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
//            Log.i(TAG, "上层连接变化(CONNECTIVITY_ACTION),: " + networkInfo.getDetailedState());

        } else if (WifiManager.RSSI_CHANGED_ACTION.equals(action)) {
            int rssi = intent.getIntExtra(wifiManager.EXTRA_NEW_RSSI, 0);
            Log.i(TAG, "信号强度(RSSI_CHANGED_ACTION): " + rssi );
            wifiAutoConnectHelper.handlerScanResults(wifiManager, connectivityManager);
        } else if("TEST_ACTION".equals(action)) {
            Log.i(TAG, "测试广播处理 " +action);
            int index;
            List<ScanResult> scanResultList = wifiManager.getScanResults();

            for (index=0; index<scanResultList.size(); index++) {
                Log.i(TAG, scanResultList.get(index).BSSID
                        + ",  " + scanResultList.get(index).frequency
                        + ",  " + scanResultList.get(index).level
                        + ",  " + scanResultList.get(index).describeContents()
                        + ",  " + scanResultList.get(index).SSID
                        + ",  " + scanResultList.get(index).capabilities);
            }

        } else {
            Log.i(TAG, "错误匹配, 未处理的广播: " +action);
        }
        new WifiConfiguration();
    }

}
