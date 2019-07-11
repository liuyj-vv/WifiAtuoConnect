package com.changhong.wifi.auto.activity;

import android.app.Activity;
import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.util.ArrayMap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    String TAG = MainActivity.class.getPackage().getName();
    String BSSID = "BSSID";
    String SSID = "SSID ";
    String IMG = "IMG";

    String[] from = {SSID, BSSID, IMG};
    int[] to = {R.id.SSID, R.id.BSSID, R.id.img};

    List<Map<String, Object>> mapList;
    TextView textview_log;
    ListView listView;
    WifiManager wifiManager;
    WifiListAdapter wifiListAdapter;

    static MainActivity mainActivity;

    public MainActivity() {
        mainActivity = this;
    }

    public static MainActivity getMainActivity() {
        return mainActivity;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mapList = new ArrayList<>();
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        textview_log = findViewById(R.id.textview_log);
        listView = findViewById(R.id.listview);
        wifiListAdapter = new WifiListAdapter(this, mapList, R.layout.layout_listview_item, from, to);
        listView.setAdapter(wifiListAdapter);

        wifiRegister();
    }

    public void wifiRegister(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filter.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION);
        filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
//        filter.addAction(WifiManager.CONFIGURED_NETWORKS_CHANGED_ACTION);
//        filter.addAction(WifiManager.LINK_CONFIGURATION_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.RSSI_CHANGED_ACTION);

        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(new WifiReceiver(), filter);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public boolean mapListRefresh() {
        if(wifiManager.isWifiEnabled()) {
            mapList.clear();
            List<ScanResult> scanResultList = wifiManager.getScanResults();
            for (int i=0; i<scanResultList.size(); i++) {
                Map<String, Object> map = new ArrayMap<>();
                map.put(BSSID, scanResultList.get(i).BSSID);
                map.put(SSID, scanResultList.get(i).SSID);
                map.put(IMG, getResources().getDrawable(R.drawable.ic_launcher_background));
                mapList.add(map);
            }
            wifiListAdapter.notifyDataSetChanged();
        } else {
            wifiManager.setWifiEnabled(true);
        }
        return true;
    }
}
