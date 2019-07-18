package com.changhong.wifi.auto.connect;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.File;
import java.util.Date;
import java.util.List;

public class WifiAutoConnectHelper {
    String TAG = WifiAutoConnectHelper.class.getPackage().getName();
    Process process;


    String  wifiType, ssid, passwd, repeate, host,count,timeout,datasize;


    boolean isMachOk = false;
    boolean isReadFlagOk = true;
    static String keyWifiType = "wifiType_2.4G";
    static String keySsid = "ssid_2.4G";
    static String keyPasswd = "passwd_2.4G";
    static String keyRepeate = "ping_repeate";
    static String keyparameter = "ping_parameter";
    static String configFile = "/mnt/sda/sda1/ch_auto_test_wifi.cfg";
    static String logFile =  "/mnt/sda/sda1/ch_auto_test_result.txt";
    ExecCommand execCommand = new ExecCommand();

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
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public boolean autoConnect(WifiManager wifiManager, List<ScanResult> scanResultList) {
        if(!readConfig()) {
            Log.e(TAG, Thread.currentThread().getStackTrace()[2].getMethodName()+"["+Thread.currentThread().getStackTrace()[2].getLineNumber()+"] 配置文件读取错误");
            return false;
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

        Log.e(TAG, Thread.currentThread().getStackTrace()[2].getMethodName()+"["+Thread.currentThread().getStackTrace()[2].getLineNumber()+"] 条件不满足，没有重连");
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public boolean startPingTest(WifiManager wifiManager) {
        if(!readConfig()) {
            Log.e(TAG, Thread.currentThread().getStackTrace()[2].getMethodName()+"["+Thread.currentThread().getStackTrace()[2].getLineNumber()+"] 配置文件读取错误");
            return false;
        }

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (null != wifiInfo && null != wifiInfo.getSSID()) {
            if (wifiInfo.getSSID().equals("\"" + ssid + "\"")) {
                String strGateway = Utils.ipMultipleStringToSignleString(Utils.hisiIpLongToString(wifiManager.getDhcpInfo().gateway));
                if (!strGateway.equals("0.0.0.0") && !execCommand.isRuning()) {

                    final int repeate_time = Integer.parseInt(repeate);
//                    if (0 != repeate_time) {
//                        new Thread(new Runnable() {
//                            @Override
//                            public void run() {
//                                while (true) {
//                                    try {
//                                        String cmd = "ping "+ " -c " + count + " -s " + datasize + " -W " + timeout + " " + host;
//                                        Log.e(TAG, "指定wifi(" + ssid + ")获取到ip，"+Utils.getCurrDate() + " 开始测试: " + cmd);
//                                        FileKeyValueOP.writeAddLineToFile(logFile, Utils.getCurrDate() + ": " + cmd);
//                                        execCommand.exitLog(logFile, "end " + cmd);
//                                        ProcessBuilder processBuilder = new ProcessBuilder("sh", "-c" , cmd);
//                                        process = execCommand.run(processBuilder);
//                                        execCommand.printMessage(process.getInputStream(), "stdout");
//                                        execCommand.printMessage(process.getErrorStream(), "stderr");
//                                        Thread.sleep(repeate_time);
//                                    } catch (InterruptedException e) {
//                                        e.printStackTrace();
//                                    }
//                                }
//                            }
//                        }).start();
//                        return true;
//                    } else
                    {
                        String cmd = "ping "+ " -c " + count + " -s " + datasize + " -W " + timeout + " " + host;
                        Log.e(TAG, "指定wifi(" + ssid + ")获取到ip，"+Utils.getCurrDate() + " 开始测试: " + cmd);
                        FileKeyValueOP.writeAddLineToFile(logFile, Utils.getCurrDate() + ": " + cmd);
                        execCommand.exitLog(logFile, "end " + cmd);
//                        ProcessBuilder processBuilder = new ProcessBuilder("sh", "-c", "ping " + strGateway + " > " + " /mnt/sda/sda1/ch_auto_test_result.txt");
                        ProcessBuilder processBuilder = new ProcessBuilder("sh", "-c" , cmd);
                        process = execCommand.run(processBuilder);
                        execCommand.printMessage(process.getInputStream(), "stdout");
                        execCommand.printMessage(process.getErrorStream(), "stderr");
                        return true;
                    }
                }
            }
        }

        Log.e(TAG, Thread.currentThread().getStackTrace()[2].getMethodName()+"["+Thread.currentThread().getStackTrace()[2].getLineNumber()+"] 条件不满足，没有进行ping测试");
        return false;
    }
}
