package com.changhong.wifi.auto.connect;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.util.Dictionary;
import java.util.List;

import static android.net.wifi.SupplicantState.ASSOCIATING;
import static android.net.wifi.SupplicantState.COMPLETED;
import static android.net.wifi.SupplicantState.DISCONNECTED;
import static android.net.wifi.SupplicantState.SCANNING;

class WifiReceiver extends BroadcastReceiver {
    String TAG = WifiReceiver.class.getPackage().getName();
    WifiManager wifiManager;
    ConnectivityManager connectivityManager;
    static WifiAutoConnectHelper wifiAutoConnectHelper = null;

    static boolean isBootFristRun = true;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        connectivityManager = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        if (null == wifiAutoConnectHelper) {
            wifiAutoConnectHelper = new WifiAutoConnectHelper();
        }

        if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) { // 这个监听wifi的打开与关闭，与wifi的连接无关
            int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);//当前的状态
            int wifiPreviousState =  intent.getIntExtra(WifiManager.EXTRA_PREVIOUS_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);//之前的状态
            Log.i(TAG, "硬件状态(WIFI_STATE_CHANGED_ACTION): " + wifiState + ", " + isBootFristRun);

            if (WifiManager.WIFI_STATE_ENABLED != wifiState) {
                LedControl.ledWifiNo();
            }

        } else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
            //扫描到一个热点, 并且此热点达可用状态会触发此广播
            //你可以从intent中取出一个boolean值; 如果此值为true, 代表着扫描热点已完全成功; 为false, 代表此次扫描不成功, ScanResult 距离上次扫描并未得到更新（可能存在）;
            if (null != wifiManager.getConnectionInfo()) {
                Log.i(TAG, "扫描结果(SCAN_RESULTS_AVAILABLE_ACTION) " + wifiManager.getConnectionInfo().getBSSID()
                        + ", " + wifiManager.getConnectionInfo().getSSID()
                        + ", " + Utils.ipMultipleStringToSignleString(Utils.hisiIpLongToString(wifiManager.getConnectionInfo().getIpAddress())));
            } else {
                Log.i(TAG, "扫描结果(SCAN_RESULTS_AVAILABLE_ACTION) ");
            }

        } else if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(action)) {
            SupplicantState supplicantState = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE); //// 获取当前网络新状态.
            int error = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, 0);      //// 获取当前网络连接状态码.
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            Log.i(TAG, "正在验证身份(SUPPLICANT_STATE_CHANGED_ACTION): " + supplicantState + ", WifiNum: " + wifiManager.getScanResults().size() + ",  SSID: " + wifiInfo.getSSID() + ", BSSID: " + wifiInfo.getBSSID());

            if (DISCONNECTED == supplicantState || ASSOCIATING == supplicantState) {
                //1.DISCONNECTED： 断开后需要重新连接
                //2.ASSOCIATING ： 开机时启动连接
                wifiAutoConnectHelper.connectConfigWifi(wifiManager);
            }

            if (DISCONNECTED == supplicantState && true == wifiAutoConnectHelper.isPingTestRunging) {
                //断开时，关闭ping命令操做【在相同的ssid切换时】
                Log.e(TAG, "身份验证--连接断开, SSID: " + wifiInfo.getSSID() + ", BSSID: " + wifiInfo.getBSSID());
                wifiAutoConnectHelper.destroyPingTest();
            }

            if (DISCONNECTED == supplicantState) {
                LedControl.ledWifiNo();
            }

            if (ASSOCIATING == supplicantState) {
                LedControl.ledWifiConnecting();
            }

            if (COMPLETED == supplicantState && false == wifiAutoConnectHelper.isPingTestRunging) {
                //连接好时启动ping命令操做【在相同的ssid切换时】
                Log.e(TAG, "身份验证--连接完成, SSID: " + wifiInfo.getSSID() + ", BSSID: " + wifiInfo.getBSSID());
                wifiAutoConnectHelper.startPingTest(wifiManager, connectivityManager);
            }

        } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
            //这三个方法能够获取手机当前连接的Wifi信息，注意在wifi断开时Intent中不包含WifiInfo对象，却包含bssid。
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            String bssid = intent.getStringExtra(WifiManager.EXTRA_BSSID);
            WifiInfo wifiInfo = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);

            DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
            Log.i(TAG, "正在获取网络ip信息(NETWORK_STATE_CHANGED_ACTION): "
                    + networkInfo.getDetailedState() + ", "
                    + networkInfo.getState()
                    + ", bssid: " + bssid + ", "
                    + ", isAvailable: " + networkInfo.isAvailable() + ", "
                    + ", isConnected: " + networkInfo.isConnected() + ", "
                    + networkInfo.getTypeName()
                    + "(" + networkInfo.getType()+")");

            if (NetworkInfo.DetailedState.DISCONNECTED == networkInfo.getDetailedState() && true == wifiAutoConnectHelper.isPingTestRunging) {
                //连接断开，停止ping命令的线程
                Log.e(TAG, "连接断开");
                wifiAutoConnectHelper.destroyPingTest();
            } else if (NetworkInfo.DetailedState.OBTAINING_IPADDR == networkInfo.getDetailedState()) {
                //正在dhcp获取ip
//                Log.e(TAG, "正在dhcp获取ip");
            } else if (NetworkInfo.DetailedState.CONNECTED == networkInfo.getDetailedState() && null != wifiInfo && false == wifiAutoConnectHelper.isPingTestRunging) {
                //已成功获取到ip，启动ping命令的线程
                Log.e(TAG, "已成功获取到ip");
                wifiAutoConnectHelper.startPingTest(wifiManager, connectivityManager);
                LedControl.ledWifiConnected();
            }


        } else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
            //ConnectivityManager.EXTRA_NO_CONNECTIVITY 返回true，代表未连接
            boolean b = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
            NetworkInfo networkInfo = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
//            Log.i(TAG, "上层连接变化(CONNECTIVITY_ACTION),: " + networkInfo.getDetailedState());

        } else if (WifiManager.RSSI_CHANGED_ACTION.equals(action)) {
            int rssi = intent.getIntExtra(wifiManager.EXTRA_NEW_RSSI, 0);
            Log.i(TAG, "信号强度(RSSI_CHANGED_ACTION): " + rssi );
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
