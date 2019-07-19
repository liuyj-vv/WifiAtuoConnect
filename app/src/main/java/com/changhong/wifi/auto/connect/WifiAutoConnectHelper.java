package com.changhong.wifi.auto.connect;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.SimpleAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;

public class WifiAutoConnectHelper {
    String TAG = WifiAutoConnectHelper.class.getPackage().getName();
    boolean isPingTestRunging = false;

    String  wifiType, ssid, passwd, repeate, host,count,timeout,datasize;

    static String keyWifiType = "wifiType_2.4G";
    static String keySsid = "ssid_2.4G";
    static String keyPasswd = "passwd_2.4G";
    static String keyRepeate = "ping_repeate";
    static String keyparameter = "ping_parameter";
    static String configFile = "/mnt/sda/sda1/ch_auto_test_wifi.cfg";
    static String logFile =  "/mnt/sda/sda1/ch_auto_test_result.txt";
//    ExecCommand execCommand = new ExecCommand();
    List<ExecCommand> execCommandList = new ArrayList<>();
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private boolean readConfig() {
        String[] wifiType = new String[1];
        String[] ssid = new String[1];
        String[] passwd = new String[1];
        String[] ping_repeate= new String[1];
        String[] ping_parameter = new String[1];
        File file = new File(configFile);
        if (!file.exists()) {
            Log.i(TAG, "文件 "+ configFile +" 不存在");
            return false;
        }

        if (false == FileKeyValueOP.readFileKeyValue(configFile, keyWifiType, wifiType)) {
            return false;
        }
        this.wifiType = wifiType[0];

        if (false == FileKeyValueOP.readFileKeyValue(configFile, keySsid, ssid)) {
            return false;
        }
        this.ssid = ssid[0];

        if (false == FileKeyValueOP.readFileKeyValue(configFile, keyPasswd, passwd)) {
            if (0 != Integer.parseInt(wifiType[0])) {
                return false;   //必须有密码时，没有密码
            }
        }
        this.passwd = passwd[0];

        if (false == FileKeyValueOP.readFileKeyValue(configFile, keyRepeate, ping_repeate)) {
            ping_repeate[0] = "";
        }
        this.repeate = ping_repeate[0];

        if (false == FileKeyValueOP.readFileKeyValue(configFile, keyparameter, ping_parameter)) {
            return false;
        } else {
            String parameter[] = ping_parameter[0].split(",");
            if(4 != parameter.length) {
                return false;
            }
            this.host = parameter[0];
            if (0 ==  Integer.parseInt(parameter[1])) {
                this.count = ""+1;  //零不能工作，修改为1
            } else {
                this.count = parameter[1];
            }
            this.timeout = parameter[2];
            this.datasize = parameter[3];
        }

        Log.e(TAG,  "配置信息，wifiType: " + this.wifiType
                + ", ssid: " + this.ssid
                + ", passwd: " + this.passwd
                + ", repeate: " + this.repeate
                + ", host: " + this.host
                + ", count: " + this.count
                + ", timeout: " + this.timeout
                + ", datasize: " + this.datasize
        );
        return true;
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void connectConfigWifi(WifiManager wifiManager) {
        if(!readConfig()) {
            Log.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName()+"["+Thread.currentThread().getStackTrace()[2].getLineNumber()+"] 配置文件读取错误");
            return;
        }

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        List<ScanResult> scanResultList = wifiManager.getScanResults();

        if (null != wifiInfo && null != wifiInfo.getSSID()) {
            if (wifiInfo.getSSID().equals("\"" + ssid + "\"")) {
                //正在连接的热点就是配置文件中的热点，直接退出
                Log.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName()+"["+Thread.currentThread().getStackTrace()[2].getLineNumber()+"] 正在连接的热点就是配置文件中的热点");
                return;
            }
        }

        if (null != scanResultList && 0 != scanResultList.size()) {
            int index;
            for(index=0; index<scanResultList.size(); index++) {
                if (scanResultList.get(index).SSID.equals(ssid)) {
                    break;
                }
                if(scanResultList.size() == index) {
                    //扫描到的热点中没有要连接的热点，直接退出不做处理
                    Log.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName()+"["+Thread.currentThread().getStackTrace()[2].getLineNumber()+"] 扫描到的热点中没有要连接的热点");
                    return;
                }
            }
        }

        if (!wifiManager.isWifiEnabled()) {
            Log.i(TAG, "wifi功能被关闭了!!!!");
            return;
        }

        Log.i(TAG, "wifi连接到配置文件指定的热点 1, " + "ssid： " + ssid +  ", wifiType:" + wifiType);
        int netId = wifiManager.addNetwork(WifiHelper.createWifiConfig(wifiManager, ssid, passwd, Integer.parseInt(wifiType)));
        if (-1 == netId) {
            Log.i(TAG, "添加新的网络描述失败!!!");
            return;
        }
        boolean enable = wifiManager.enableNetwork(netId, true); //true连接新的网络
        if (false == enable) {
            Log.i(TAG, "将新的网络描述,使能失败!!!");
            return;
        }
        boolean reconnect = wifiManager.reconnect();
        if (false == reconnect) {
            Log.i(TAG, "重新连接新的网络失败!!!");
            return;
        }

        Log.i(TAG, "完成重新连接网络到 ----> " + ssid);
    }

    public boolean destroyPingTest() {
        for (ExecCommand execCommand : execCommandList) {
            execCommand.destroy();
        }
        execCommandList.clear();
        isPingTestRunging = false;
        Log.i(TAG, "网络断开，ping测试停止!!!");
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public boolean startPingTest(WifiManager wifiManager, ConnectivityManager connectivityManager) {
        final WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        List<ScanResult> scanResultList = wifiManager.getScanResults();
        ScanResult scanResult = null;

        if (NetworkInfo.DetailedState.CONNECTED != networkInfo.getDetailedState()) {
            Log.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName()+"["+Thread.currentThread().getStackTrace()[2].getLineNumber()+"] 未获取到ip，不能进行ping测试");
            return false;
        }

        if(!readConfig()) {
            Log.e(TAG, Thread.currentThread().getStackTrace()[2].getMethodName()+"["+Thread.currentThread().getStackTrace()[2].getLineNumber()+"] 配置文件读取错误");
            return false;
        }
        final int iRrepeateTime = Integer.parseInt(this.repeate);
        if (null != wifiInfo && null != wifiInfo.getSSID()) {
            if (wifiInfo.getSSID().equals("\"" + ssid + "\"")) {
                int index;
                for (index=0; index<scanResultList.size(); index++) {
                    if(wifiInfo.getBSSID().equals(scanResultList.get(index).BSSID)) {
                        scanResult = scanResultList.get(index);
                        break;
                    }
                }
                if (scanResultList.size() == index) {
                    Log.e(TAG, "不应该到这儿来");
                    return false;
                }

                final String currIP = Utils.ipMultipleStringToSignleString(Utils.hisiIpLongToString(wifiInfo.getIpAddress()));
                final String currWifiFrequency = ""+scanResult.frequency;
                final String cmd = "ping"+ " -c " + count + " -s " + datasize + " -W " + timeout + " " + host;
                if (0 == iRrepeateTime && null != scanResult) {
                    isPingTestRunging = true;
                    ExecCommand execCommand = new ExecCommand();
                    execCommandList.add(execCommand);
                    execCommand.writeLogToFile(logFile, cmd, currIP , wifiInfo.getBSSID(), currWifiFrequency);
                    ProcessBuilder processBuilder = new ProcessBuilder("sh", "-c" , cmd);
                    execCommand.run(processBuilder);
                    execCommand.printStdoutMessage(logFile, "stdout");
                    execCommand.printStderrMessage(logFile, "stderr");
                    Log.i(TAG, "开始进行ping测试, iRrepeateTime: " + iRrepeateTime);
                    return true;
                } else {
                    isPingTestRunging = true;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while (true) {
                                try {
                                    ExecCommand execCommand = new ExecCommand();
                                    execCommandList.add(execCommand);
                                    execCommand.writeLogToFile(logFile, cmd, currIP , wifiInfo.getBSSID(), currWifiFrequency);
                                    ProcessBuilder processBuilder = new ProcessBuilder("sh", "-c" , cmd);
                                    execCommand.run(processBuilder);
                                    execCommand.printStdoutMessage(logFile, "stdout");
                                    execCommand.printStderrMessage(logFile, "stderr");

                                    Thread.sleep(iRrepeateTime*1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }).start();
                    Log.i(TAG, "开始进行ping测试, iRrepeateTime: " + iRrepeateTime);
                    return true;
                }
            }
        }

        Log.e(TAG, Thread.currentThread().getStackTrace()[2].getMethodName()+"["+Thread.currentThread().getStackTrace()[2].getLineNumber()+"] 条件不满足，没有进行ping测试");
        return false;
    }
}
