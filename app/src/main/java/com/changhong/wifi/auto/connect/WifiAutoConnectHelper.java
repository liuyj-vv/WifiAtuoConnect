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
import java.util.concurrent.locks.ReentrantLock;

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

    static Long configFileLastModified = 0L;

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

        if (0L == configFileLastModified || configFileLastModified != file.lastModified()) {
            configFileLastModified = file.lastModified();
        } else {
            // 配置文件上次读取后，没有修改。
            Log.d(TAG,  "配置信息（未重取），wifiType: " + this.wifiType
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

        Log.d(TAG,  "配置信息，wifiType: " + this.wifiType
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
            destroyPingTest();
            LedControl.ledWifiConnect_no();
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
    public void handlerScanResults(WifiManager wifiManager, ConnectivityManager connectivityManager) {
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        List<ScanResult> scanResultList = wifiManager.getScanResults();
        if (null != wifiInfo && null != wifiInfo.getSSID()) {

            startPingTest(wifiManager, connectivityManager);

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
            if (!readConfig()) {
                return;
            }
            if (!wifiIsConfigssid(wifiInfo)) {
                return;
            }
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

    private ReentrantLock lock = new ReentrantLock();
    public boolean destroyPingTest() {
        lock.lock();
        for (ExecCommand execCommand : execCommandList) {
            execCommand.destroy();
        }
        execCommandList.clear();
        try {
            if (null != cyclePingThread) {
                isPingTestRunging = false;
                Log.i(TAG, "interrupt 输入，停止循环的ping测试!!!");
                cyclePingThread.interrupt();
                cyclePingThread.join();
                Log.d(TAG, "cyclePingThread.join 完成，循环启动ping测试的线程正常结束!");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        cyclePingThread = null; //移到下面，方式 join 函数出现异常而没有赋值。
        Log.d(TAG, "销毁循环启动ping的函数正常结束!");
        lock.unlock();
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
                    lock.lock();
                    isPingTestRunging = true;
                    cyclePingThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            boolean isNotFirstRun = false;
                            while (isPingTestRunging) {
                                try {
                                    if (isNotFirstRun) {
                                        Thread.sleep(iRrepeateTime*1000);
                                    }

                                    ExecCommand execCommand = new ExecCommand();
                                    execCommandList.add(execCommand);
                                    execCommand.writeLogToFile(logFile, cmd, currIP , wifiInfo.getBSSID(), currWifiFrequency, wifiInfo.getSSID(), repeate);
                                    ProcessBuilder processBuilder = new ProcessBuilder("sh", "-c" , cmd);
                                    execCommand.run(processBuilder);
                                    execCommand.printStdoutMessage(logFile, "stdout");
                                    execCommand.printStderrMessage(logFile, "stderr");
                                    LedControl.ledWifiConnect_dhcp_succesful();

                                    isNotFirstRun = true;
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                    Log.i(TAG, "收到中断，停止定时ping测试的线程！！！");
                                    break;
                                }
                            }
                            Log.i(TAG, "定时启动的ping测试，终止！！, iRrepeateTime: " + iRrepeateTime);
                        }
                    });
                    cyclePingThread.start();
                    Log.i(TAG, "开启定时启动的ping测试, iRrepeateTime: " + iRrepeateTime);
                    lock.unlock();
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
