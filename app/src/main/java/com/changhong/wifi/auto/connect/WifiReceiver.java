package com.changhong.wifi.auto.connect;

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
import android.util.Log;

import java.util.List;

class WifiReceiver extends BroadcastReceiver {
    String TAG = WifiReceiver.class.getPackage().getName();
    WifiManager wifiManager;
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) { // 这个监听wifi的打开与关闭，与wifi的连接无关
            int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);//当前的状态
            int wifiPreviousState =  intent.getIntExtra(WifiManager.EXTRA_PREVIOUS_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);//之前的状态

            Log.i(TAG, "硬件状态(WIFI_STATE_CHANGED_ACTION): " + wifiState);
        } else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
            //扫描到一个热点, 并且此热点达可用状态 会触发此广播
            //你可以从intent中取出一个boolean值; 如果此值为true, 代表着扫描热点已完全成功; 为false, 代表此次扫描不成功, ScanResult 距离上次扫描并未得到更新;
            List<ScanResult> scanResultList = wifiManager.getScanResults();
            for (int i=0; i<scanResultList.size(); i++) {
//                Log.i(TAG,  "SSID: " + scanResultList.get(i).SSID + ", capabilities:  " + scanResultList.get(i).capabilities);
            }
            Log.i(TAG, "扫描结果(SCAN_RESULTS_AVAILABLE_ACTION) "  + intent);
        } else if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(action)) {
            SupplicantState supplicantState = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE); //// 获取当前网络新状态.
            int error = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, 0); //// 获取当前网络连接状态码.

            Log.i(TAG, "连接验证(SUPPLICANT_STATE_CHANGED_ACTION): " + supplicantState + ", error: " + error);
        } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
            //这三个方法能够获取手机当前连接的Wifi信息，注意在wifi断开时Intent中不包含WifiInfo对象，却包含bssid。
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            WifiInfo wifiInfo = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
            String bssid = intent.getStringExtra(WifiManager.EXTRA_BSSID);

            Log.i(TAG, "网络状态(NETWORK_STATE_CHANGED_ACTION), " + networkInfo + "。 " + wifiInfo + "。 bssid: " + bssid + ", " + intent);
        } else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
            //ConnectivityManager.EXTRA_NO_CONNECTIVITY 返回true，代表未连接
            boolean b = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
            NetworkInfo networkInfo = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
            Log.i(TAG, "连接变化(CONNECTIVITY_ACTION), b: " + b + "。 " + networkInfo);
        } else if (WifiManager.RSSI_CHANGED_ACTION.equals(action)) {

            Log.i(TAG, "信号强度(RSSI_CHANGED_ACTION), " + intent );
        } else if("TEST_ACTION".equals(action)) {

            Log.i(TAG, "测试广播接收: " +action);

            int netId = wifiManager.addNetwork(createWifiConfig("test991", "123456789", 2));
            Log.d(TAG, "netId: " + netId);
            boolean enable = wifiManager.enableNetwork(netId, true);
            Log.d(TAG, "enable: " + enable);
            boolean reconnect = wifiManager.reconnect();
            Log.d(TAG, "reconnect: " + reconnect);

            Log.i(TAG, "测试广播处理完成: " +action);

        } else {
            Log.i(TAG, "错误匹配, 未处理的广播: " +action);
        }
        new WifiConfiguration();
    }

    private static final int WIFICIPHER_NOPASS = 0;
    private static final int WIFICIPHER_WEP = 1;
    private static final int WIFICIPHER_WPA = 2;

    private WifiConfiguration createWifiConfig(String ssid, String password, int type) {
        //初始化WifiConfiguration
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();

        //指定对应的SSID
        config.SSID = "\"" + ssid + "\"";

        //如果之前有类似的配置
        WifiConfiguration tempConfig = isExist(ssid);
        if(tempConfig != null) {
            //则清除旧有配置
            wifiManager.removeNetwork(tempConfig.networkId);
        }

        //不需要密码的场景
        if(type == WIFICIPHER_NOPASS) {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            //以WEP加密的场景
        } else if(type == WIFICIPHER_WEP) {
            config.hiddenSSID = true;
            config.wepKeys[0]= "\""+password+"\"";
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
            //以WPA加密的场景，自己测试时，发现热点以WPA2建立时，同样可以用这种配置连接
        } else if(type == WIFICIPHER_WPA) {
            config.preSharedKey = "\""+password+"\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
        }

        return config;
    }

    private WifiConfiguration isExist(String ssid) {
        List<WifiConfiguration> configs = wifiManager.getConfiguredNetworks();

        for (WifiConfiguration config : configs) {
            if (config.SSID.equals("\""+ssid+"\"")) {
                return config;
            }
        }
        return null;
    }
}
