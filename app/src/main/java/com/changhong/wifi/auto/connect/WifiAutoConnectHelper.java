package com.changhong.wifi.auto.connect;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static android.net.wifi.SupplicantState.ASSOCIATING;
import static android.net.wifi.SupplicantState.COMPLETED;
import static android.net.wifi.SupplicantState.DISCONNECTED;
import static android.net.wifi.WifiManager.WIFI_STATE_ENABLED;

public class WifiAutoConnectHelper {
    String TAG = this.getClass().getPackage().getName();
    boolean isPingTestRunging = false;
    Thread cyclePingThread = null;

    String  wifiType, ssid, passwd, repeate, host,count,timeout,datasize;

    static String keyWifiType = "wifiType";
    static String keySsid = "ssid";
    static String keyPasswd = "passwd";
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
    public void handlerWifiState(WifiManager wifiManager) {
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        if (WIFI_STATE_ENABLED != wifiManager.getWifiState()) {
            return;
        }

        if(!readConfig()) {
            Log.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName()+"["+Thread.currentThread().getStackTrace()[2].getLineNumber()+"] 配置文件读取错误");
            return;
        }
        Log.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName()+"["+Thread.currentThread().getStackTrace()[2].getLineNumber()+"] ");

        if(wifiIsConfigssid(wifiInfo)) {
            return;
        }

       connectConfigWIFI(wifiManager);
    }

    private boolean wifiIsConfigssid(WifiInfo wifiInfo) {
        if (null != wifiInfo && null != wifiInfo.getSSID()) {
            if (wifiInfo.getSSID().equals("\"" + ssid + "\"")
                    && ("\"" + ssid + "\"").equals(wifiInfo.getSSID())) {
                //连接上的热点就是配置文件中的热点，直接退出
                Log.i(TAG, Thread.currentThread().getStackTrace()[3].getMethodName()+"["+Thread.currentThread().getStackTrace()[2].getLineNumber()+"] 正在连接的热点就是配置文件中的热点: " + wifiInfo.getSSID());
                return true;
            }
        }
        return false;
    }

    private boolean scanResultsIsContainConfigssid(List<ScanResult> scanResultList) {
        if (null != scanResultList) {
            int index;
            for (index=0; index<scanResultList.size(); index++) {
                if (scanResultList.get(index).SSID.equals(ssid)) {
//                    Log.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName()+"["+Thread.currentThread().getStackTrace()[2].getLineNumber()+"] 扫描到到热点: "+ scanResultList.get(index).SSID);
                    break;
                }
                if (scanResultList.size() == index+1) {
                    //扫描到得热点，没有配置文件中的配置
//                    Log.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName()+"["+Thread.currentThread().getStackTrace()[2].getLineNumber()+"] 扫描得到热点，没有配置文件中的配置");
                    LedControl.ledWifiConnect_no();
                    return false;
                }
            }
        }
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void handlerScanResults(WifiManager wifiManager) {
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        List<ScanResult> scanResultList = wifiManager.getScanResults();
        if (null != wifiInfo && null != wifiInfo.getSSID()) {
            if(!readConfig()){
                return;
            }

            if (!scanResultsIsContainConfigssid(scanResultList)) {
                Log.e(TAG, "扫描到的热点没有配置文件中的ssid");
                return;
            }

            if (wifiIsConfigssid(wifiInfo)) {
                //连上的wifi是配置文件中的wifi，直接返回，不再连接
                Log.e(TAG, "连上的wifi是配置文件中的wifi，直接返回，不再重新连接");
                return;
            }

            connectConfigWIFI(wifiManager);
        } else {
            //没有连接wifi
            if(!readConfig()){
                return;
            }
            connectConfigWIFI(wifiManager);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public  void handlerSupplicant(WifiManager wifiManager, ConnectivityManager connectivityManager, SupplicantState supplicantState, int errNo) {
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        if (DISCONNECTED == supplicantState) {
            LedControl.ledWifiConnect_no();
            destroyPingTest();
        }
        if (ASSOCIATING == supplicantState) {
            LedControl.ledWifiConnect_ing();
        }
         if (COMPLETED == supplicantState) {
            startPingTest(wifiManager, connectivityManager);
        }

        Log.i(TAG, "" + supplicantState + ", ssid: " + wifiInfo.getSSID());
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public  void handlefNetwork(WifiManager wifiManager, ConnectivityManager connectivityManager, NetworkInfo networkInfo, WifiInfo wifiInfo, String bssid) {

        Log.i(TAG, "" + networkInfo);

        if (NetworkInfo.DetailedState.DISCONNECTED == networkInfo.getDetailedState()) {
            destroyPingTest();
            LedControl.ledWifiConnect_no();
        }
        if (NetworkInfo.DetailedState.CONNECTED == networkInfo.getDetailedState()) {
            startPingTest(wifiManager, connectivityManager);
        }
    }


    public boolean destroyPingTest() {
        for (ExecCommand execCommand : execCommandList) {
            execCommand.destroy();
        }
        execCommandList.clear();
        Log.i(TAG, "网络断开，循环的ping测试停止!!!");
        try {
            if (null != cyclePingThread) {
                cyclePingThread.interrupt();
                Log.i(TAG, "cyclePingThread interrput输入");
                cyclePingThread.join();
                Log.i(TAG, "cyclePingThread 线程结束");
                cyclePingThread = null;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        Log.i(TAG, "本次连接的ping测试已全部退出!!!");
        isPingTestRunging = false;
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public boolean startPingTest(WifiManager wifiManager, ConnectivityManager connectivityManager) {
        final WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        List<ScanResult> scanResultList = wifiManager.getScanResults();
        ScanResult scanResult = null;

        if (true == isPingTestRunging) {
            return false;
        }

        if (NetworkInfo.DetailedState.CONNECTED != networkInfo.getDetailedState()) {
            Log.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName()+"["+Thread.currentThread().getStackTrace()[2].getLineNumber()+"] 未获取到ip，不能进行ping测试: " + networkInfo.getDetailedState());
            return false;
        }

        if(!readConfig()) {
            Log.e(TAG, Thread.currentThread().getStackTrace()[2].getMethodName()+"["+Thread.currentThread().getStackTrace()[2].getLineNumber()+"] 配置文件读取错误");
            return false;
        }
        final int iRrepeateTime = Integer.parseInt(this.repeate);
        if (null != wifiInfo && null != wifiInfo.getSSID()) {
            Log.i(TAG, Thread.currentThread().getStackTrace()[3].getMethodName()+"["+Thread.currentThread().getStackTrace()[2].getLineNumber()+"] ");

            if (wifiIsConfigssid(wifiInfo)) {
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
                    execCommand.writeLogToFile(logFile, cmd, currIP , wifiInfo.getBSSID(), currWifiFrequency, wifiInfo.getSSID(), repeate);
                    ProcessBuilder processBuilder = new ProcessBuilder("sh", "-c" , cmd);
                    execCommand.run(processBuilder);
                    execCommand.printStdoutMessage(logFile, "stdout");
                    execCommand.printStderrMessage(logFile, "stderr");
                    LedControl.ledWifiConnect_dhcp_succesful();
                    Log.i(TAG, "开启单独的一次ping测试, iRrepeateTime: " + iRrepeateTime);
                    return true;
                } else {
                    isPingTestRunging = true;
                    cyclePingThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while (true) {
                                try {
                                    ExecCommand execCommand = new ExecCommand();
                                    execCommandList.add(execCommand);
                                    execCommand.writeLogToFile(logFile, cmd, currIP , wifiInfo.getBSSID(), currWifiFrequency, wifiInfo.getSSID(), repeate);
                                    ProcessBuilder processBuilder = new ProcessBuilder("sh", "-c" , cmd);
                                    execCommand.run(processBuilder);
                                    execCommand.printStdoutMessage(logFile, "stdout");
                                    execCommand.printStderrMessage(logFile, "stderr");
                                    LedControl.ledWifiConnect_dhcp_succesful();

                                    Thread.sleep(iRrepeateTime*1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                    Log.i(TAG, "收到中断，停止定时ping测试的线程！！！");
                                    break;
                                }
                            }
                        }
                    });
                    cyclePingThread.start();
                    Log.i(TAG, "开启定时启动的ping测试, iRrepeateTime: " + iRrepeateTime);
                    return true;
                }
            }
        }

        Log.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName()+"["+Thread.currentThread().getStackTrace()[2].getLineNumber()+"] 条件不满足，没有进行ping测试");
        return false;
    }

    private boolean connectConfigWIFI(WifiManager wifiManager) {
        Log.i(TAG, "wifi连接到配置文件指定的热点 1, " + "ssid： " + ssid +  ", wifiType:" + wifiType);
        int netId = wifiManager.addNetwork(WifiHelper.createWifiConfig(wifiManager, ssid, passwd, Integer.parseInt(wifiType)));
        if (-1 == netId) {
            Log.i(TAG, "添加新的网络描述失败!!!");
            return false;
        }
        boolean enable = wifiManager.enableNetwork(netId, true); //true连接新的网络
        if (false == enable) {
            Log.i(TAG, "将新的网络描述,使能失败!!!");
            return false;
        }
        boolean reconnect = wifiManager.reconnect();
        if (false == reconnect) {
            Log.i(TAG, "重新连接新的网络失败!!!");
            return false;
        }

        Log.i(TAG, "完成重新连接网络到 ----> " + ssid);
        return true;
    }
}
