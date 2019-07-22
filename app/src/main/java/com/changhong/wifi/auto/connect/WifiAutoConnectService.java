package com.changhong.wifi.auto.connect;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;

public class WifiAutoConnectService extends Service {
    String TAG = WifiAutoConnectService.class.getPackage().getName();
    WifiReceiver wifiReceiver;
    WifiManager wifiManager;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {

        Log.e(TAG, Thread.currentThread().getStackTrace()[2].getMethodName()+"["+Thread.currentThread().getStackTrace()[2].getLineNumber()+"]");
        super.onCreate();
        wifiManager = (WifiManager) getBaseContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        LedControl.ledWifiConnect_no();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        if(!wifiManager.isWifiEnabled()) {
                            LedControl.ledWifiConnect_no();
                            wifiManager.setWifiEnabled(true);
                        }
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();


        wifiRegister();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(wifiReceiver);
    }

    private void wifiRegister(){
        wifiReceiver = new WifiReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);    //用于监听Android Wifi打开或关闭的状态，
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filter.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION);
        filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
//        filter.addAction(WifiManager.CONFIGURED_NETWORKS_CHANGED_ACTION);
//        filter.addAction(WifiManager.LINK_CONFIGURATION_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);//用于判断是否连接到了有效wifi（不能用于判断是否能够连接互联网）
        filter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction("TEST_ACTION");
        registerReceiver(wifiReceiver, filter);
    }
}
