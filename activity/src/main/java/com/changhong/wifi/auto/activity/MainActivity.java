package com.changhong.wifi.auto.activity;

import android.app.Activity;
import android.content.Context;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    String BSSID = "BSSID";
    String SSID = "SSID ";
    String IMG = "IMG";

    String[] from = {SSID, BSSID, IMG};
    int[] to = {R.id.SSID, R.id.BSSID, R.id.img};

    List<Map<String, Object>> mapList;
    ListView listView;
    WifiManager wifiManager;
    WifiListAdapter wifiListAdapter;
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mapList = new ArrayList<>();
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        listView = findViewById(R.id.listview);
        wifiListAdapter = new WifiListAdapter(this, mapList, R.layout.layout_listview_item, from, to);
        listView.setAdapter(wifiListAdapter);

        mapListRefresh();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mapListRefresh();
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private boolean mapListRefresh() {
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
