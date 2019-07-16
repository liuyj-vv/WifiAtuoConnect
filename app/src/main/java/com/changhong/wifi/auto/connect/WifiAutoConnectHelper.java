package com.changhong.wifi.auto.connect;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.util.List;

public class WifiAutoConnectHelper {
    String TAG = WifiAutoConnectHelper.class.getPackage().getName();
    Process process;
    String wifiType;
    String ssid;
    String passwd;

    boolean isMachOk = false;
    boolean isReadFlagOk = true;
    String configFile = "/mnt/sda/sda1/ch_auto_test_wifi.cfg";
    String logFile =  "/mnt/sda/sda1/ch_auto_test_result.txt";
    ExecCommand execCommand = new ExecCommand();

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public boolean autoConnect(WifiManager wifiManager, List<ScanResult> scanResultList) {
        wifiType = FileKeyValueOP.readFileKeyValue(configFile, "wifiType", "");
        ssid = FileKeyValueOP.readFileKeyValue(configFile, "ssid", "");
        passwd = FileKeyValueOP.readFileKeyValue(configFile, "passwd", "");
        if (wifiType.equals("") || ssid.equals("")) {
            isReadFlagOk = false;
        } else {
            isReadFlagOk = true;
        }

        isMachOk = false;
        if (true == isReadFlagOk) {
            for (int i=0; i<scanResultList.size(); i++) {
//                Log.i(TAG,  "SSID: " + scanResultList.get(i).SSID + ", capabilities:  " + scanResultList.get(i).capabilities);
                if (scanResultList.get(i).SSID.equals(ssid)) {
                    isMachOk = true;
                    break;
                }
            }
        }

        if (true == isMachOk) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (null != wifiInfo && null != wifiInfo.getSSID()) {
                if (!wifiInfo.getSSID().equals("\"" + ssid + "\"")) {
                    Log.e(TAG, "重新连接指定wifi: " + wifiInfo.getSSID() +"---->"+ ssid);
                    int netId = wifiManager.addNetwork(WifiHelper.createWifiConfig(wifiManager, ssid, passwd, Integer.parseInt(wifiType)));
                    boolean enable = wifiManager.enableNetwork(netId, true);
                    boolean reconnect = wifiManager.reconnect();
                }
            }
        }
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public boolean startPingTest(WifiManager wifiManager) {
        wifiType = FileKeyValueOP.readFileKeyValue(configFile, "wifiType", "");
        ssid = FileKeyValueOP.readFileKeyValue(configFile, "ssid", "");

        if (wifiType.equals("") || ssid.equals("")) {
            return false;
        } else {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (null != wifiInfo && null != wifiInfo.getSSID()) {
                if (!wifiInfo.getSSID().equals("\"" + ssid + "\"")) {
                    String strGateway = Utils.hisiIpLongToString(wifiManager.getDhcpInfo().gateway);
                    if (!strGateway.equals("000.000.000.000") && !execCommand.isRuning()) {
                        Log.e(TAG, "指定wifi(" + ssid + ")获取到ip，进行ping测试");
                        process = execCommand.run("ping " + strGateway);
                        execCommand.printMessage(process.getInputStream(), "stdout");
                        execCommand.printMessage(process.getErrorStream(), "stderr");
                    }
                }
            }
        }
        return true;
    }
}
