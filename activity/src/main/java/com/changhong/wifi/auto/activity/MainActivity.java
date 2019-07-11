package com.changhong.wifi.auto.activity;

import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.util.ArrayMap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    String BSSID = "BSSID";
    String SSID = "SSID ";
    String IMG = "IMG";

    String[] from = {BSSID, SSID, IMG};
    int[] to = {R.id.BSSID, R.id.SSID, R.id.img};

    List<Map<String, Object>> mapList;
    ListView listView;

//    WifiManager wifiManager = (WifiManager) this.getApplication().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mapListInit();
        listView = findViewById(R.id.listview);
        listView.setAdapter(new WifiListAdapter(this, mapList, R.layout.layout_listview_item, from, to));
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private int mapListInit() {
        mapList = new ArrayList<>();

        for (int i=0; i<10; i++) {
            Map<String, Object> map = new ArrayMap<>();
            map.put(BSSID, "wifi BSSID"+i);
            map.put(SSID, "wifi SSID"+i);
            map.put(IMG, getResources().getDrawable(R.drawable.ic_launcher_background));
            mapList.add(map);
        }
        return 1;
    }
}
