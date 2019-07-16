package com.changhong.wifi.auto.connect;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.util.Date;
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
        if (null == scanResultList) {
            scanResultList = wifiManager.getScanResults();
            if (null == scanResultList) {
                Log.e(TAG, "scanResultList为空，连接" + "---->"+ ssid + ", wifiType:" + wifiType);
                int netId = wifiManager.addNetwork(WifiHelper.createWifiConfig(wifiManager, ssid, passwd, Integer.parseInt(wifiType)));
                boolean enable = wifiManager.enableNetwork(netId, true);
                boolean reconnect = wifiManager.reconnect();
                return true;
            }
        }

        if (true == isReadFlagOk && null != scanResultList) {
            for (int i=0; i<scanResultList.size(); i++) {
                if (scanResultList.get(i).SSID.equals(ssid)) {
                    isMachOk = true;
                    break;
                }
            }
        }

//        Log.e(TAG, "重新连接指定wifi" +wifiType +" "+ ssid +" "+ passwd);

        if (true == isMachOk) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (null != wifiInfo && null != wifiInfo.getSSID()) {
                if (!wifiInfo.getSSID().equals("\"" + ssid + "\"")) {
                    Log.e(TAG, "重新连接指定wifi: " + wifiInfo.getSSID() +"---->"+ ssid + ", wifiType:" + wifiType);
                    int netId = wifiManager.addNetwork(WifiHelper.createWifiConfig(wifiManager, ssid, passwd, Integer.parseInt(wifiType)));
                    boolean enable = wifiManager.enableNetwork(netId, true);
                    boolean reconnect = wifiManager.reconnect();
                    return true;
                }
            }
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public boolean startPingTest(WifiManager wifiManager) {
        wifiType = FileKeyValueOP.readFileKeyValue(configFile, "wifiType", "");
        ssid = FileKeyValueOP.readFileKeyValue(configFile, "ssid", "");

        if (wifiType.equals("") || ssid.equals("")) {
            Log.e(TAG, Thread.currentThread().getStackTrace()[2].getMethodName()+"["+Thread.currentThread().getStackTrace()[2].getLineNumber()+"]");
            return false;
        } else {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (null != wifiInfo && null != wifiInfo.getSSID()) {
                if (wifiInfo.getSSID().equals("\"" + ssid + "\"")) {
                    String strGateway = Utils.ipMultipleStringToSignleString(Utils.hisiIpLongToString(wifiManager.getDhcpInfo().gateway));
                    if (!strGateway.equals("0.0.0.0") && !execCommand.isRuning()) {
                        Log.e(TAG, "指定wifi(" + ssid + ")获取到ip，"+Utils.getCurrDate()+ ": ping " + strGateway);
                        FileKeyValueOP.writeAddLineToFile(logFile, Utils.getCurrDate() + ": start ping " + strGateway);
                        execCommand.exitLog(logFile, "end ping " + strGateway);
//                        ProcessBuilder processBuilder = new ProcessBuilder("sh", "-c", "ping " + strGateway + " > " + " /mnt/sda/sda1/ch_auto_test_result.txt");
                        ProcessBuilder processBuilder = new ProcessBuilder("ping", strGateway);
                        process = execCommand.run(processBuilder);
                        execCommand.printMessage(process.getInputStream(), "stdout");
                        execCommand.printMessage(process.getErrorStream(), "stderr");
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
