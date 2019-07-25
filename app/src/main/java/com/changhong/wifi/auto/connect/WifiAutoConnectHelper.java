package com.changhong.wifi.auto.connect;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.RemoteException;
import android.support.annotation.RequiresApi;
import android.util.ArrayMap;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.net.wifi.SupplicantState.ASSOCIATING;
import static android.net.wifi.SupplicantState.COMPLETED;
import static android.net.wifi.SupplicantState.DISCONNECTED;
import static android.net.wifi.WifiManager.WIFI_STATE_ENABLED;

public class WifiAutoConnectHelper {
    String TAG = this.getClass().getPackage().getName();
    boolean isPingTestRunging = false;
    Thread cyclePingThread = null;
    Context context;
    String  wifiType, ssid, passwd, repeate, host,count,timeout,datasize;
//    #指定ping成功后，自动启动以下应用（包名称+类名称）。如果有多个应用，以空格区分。
//    ping_ok_do=com.changhong.vod/.RootActivity com.changhong.vod1/.RootActivity1 com.changhong.vod2/.RootActivity3

    List<Map<String, String>> listMapPingOkDo = new ArrayList<>();
    String wifi_ip_cfg;
    String wifi_ip;
    String wifi_mask;
    String wifi_gateway;


    static String keyWifiType = "wifiType";
    static String keySsid = "ssid";
    static String keyPasswd = "passwd";
    static String keyRepeate = "ping_repeate";
    static String keyparameter = "ping_parameter";
    static String keyPing_ok_do= "ping_ok_do";
    static String keyWifi_ip_cfg= "wifi_ip_cfg";
    static String keyWifi_ip= "wifi_ip";
    static String configFile = "/mnt/sda/sda1/ch_auto_test_wifi.cfg";
    static String logFile;
    List<ExecCommand> execCommandList = new ArrayList<>();

    static Long configFileLastModified = 0L;

    static {
        try {
            String mac = Utils.getDeviceLanMACAddress();
            if (12 == mac.length()) {
                logFile = "/mnt/sda/sda1/ch_auto_test_result_" + mac.substring(8) + ".txt";
            } else {
                logFile = "/mnt/sda/sda1/ch_auto_test_result_0000.txt";
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            logFile = "/mnt/sda/sda1/ch_auto_test_result_0000.txt";
        }
    }

    WifiAutoConnectHelper(Context context) {
        this.context = context;
    }


    static int configNetworkID = -1;
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private boolean readConfig__() {
        if (!readConfig()) {
            LedControl.ledWifiConnect_no();
            return false;
        } else {
            //若盒子中未保存，尝试添加将配置文件中的wifi配置到盒子中
            int index;
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            List<WifiConfiguration>  wifiConfigurationList = wifiManager.getConfiguredNetworks();
            if (null != wifiConfigurationList) {
                for (index=0; index<wifiConfigurationList.size(); index++) {
                    if (wifiConfigurationList.get(index).SSID.equals(ssid)) {
                        break;
                    }
                }
                if (wifiConfigurationList.size() == index) {
                    configNetworkID = wifiManager.addNetwork(WifiHelper.createWifiConfig(wifiManager, ssid, passwd, Integer.parseInt(wifiType)));
                    if (-1 == configNetworkID) {
                        Log.i(TAG, "添加新的网络到配置文件失败， " + "ssid： " + ssid +  ", wifiType:" + wifiType + "====>> networkID: " + configNetworkID);
                        return false;
                    } else {
                        Log.i(TAG, "添加新的网络到配置文件成功， " + "ssid： " + ssid +  ", wifiType:" + wifiType + "====>> networkID: " + configNetworkID);
                        return true;
                    }
                }
            }
            return false;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private boolean readConfig() {
        String[] wifiType = new String[1];
        String[] ssid = new String[1];
        String[] passwd = new String[1];
        String[] ping_repeate = new String[1];
        String[] ping_parameter = new String[1];
        String[] ping_ok_do_line = new String[1];
        String[] wifi_ip_cfg = new String[1];
        String[] wifi_ip = new String[1];
        File file = new File(configFile);
        if (!file.exists()) {
            Log.i(TAG, "文件 " + configFile + " 不存在");
            return false;
        }

        if (0L == configFileLastModified || configFileLastModified != file.lastModified()) {

        } else {
            // 配置文件上次读取后，没有修改。
            Log.d(TAG, "配置信息（未重取），wifiType: " + this.wifiType
                    + ", ssid: " + this.ssid
                    + ", passwd: " + this.passwd
                    + ", repeate: " + this.repeate
                    + ", host: " + this.host
                    + ", count: " + this.count
                    + ", timeout: " + this.timeout
                    + ", datasize: " + this.datasize
                    + ", listMapPingOkDo: " + this.listMapPingOkDo
                    + ", wifi_ip_cfg: " + this.wifi_ip_cfg
                    + ", wifi_ip: " + this.wifi_ip
                    + ", wifi_mask: " + this.wifi_mask
                    + ", wifi_gateway: " + this.wifi_gateway
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
            if (4 != parameter.length) {
                return false;
            }
            this.host = parameter[0];
            if (0 == Integer.parseInt(parameter[1])) {
                this.count = "" + 1;  //零不能工作，修改为1
            } else {
                this.count = parameter[1];
            }
            this.timeout = parameter[2];
            this.datasize = parameter[3];
        }

        if (false == FileKeyValueOP.readFileKeyValue(configFile, keyRepeate, ping_repeate)) {
            ping_repeate[0] = "";
        }
        this.repeate = ping_repeate[0];


        if (false == FileKeyValueOP.readFileKeyValue(configFile, keyPing_ok_do, ping_ok_do_line)) {

        } else {
            String[] ping_ok_do = ping_ok_do_line[0].trim().split("\\s");
            Pattern pattern = Pattern.compile("(.*?)/\\.(.*)");
            Matcher matcher;

            for (int i = 0; i < ping_ok_do.length; i++) {
                matcher = pattern.matcher(ping_ok_do[i]);
                if (matcher.find()) {
                    //匹配出错，输入参数格式不对
                    Map map = new ArrayMap();
                    Log.d(TAG, "matcher.group(0): " + matcher.group(0) + ", matcher.group(1): " + matcher.group(1) + ", matcher.group(2): " + matcher.group(2));
                    map.put("package", matcher.group(1));
                    map.put("activity", matcher.group(2));
                    listMapPingOkDo.add(map);
                }
            }
        }

        if (false == FileKeyValueOP.readFileKeyValue(configFile, keyWifi_ip_cfg, wifi_ip_cfg)) {
            return false;
        }
        this.wifi_ip_cfg = wifi_ip_cfg[0].trim();

        if (!wifi_ip_cfg.equals("static") || !wifi_ip.equals("dhcp")) {
        } else {
            return false;
        }

        if (this.wifi_ip_cfg.equals("static")) {
            if (false == FileKeyValueOP.readFileKeyValue(configFile, keyWifi_ip, wifi_ip)) {
                return false;
            }else {
                String[] tmp = wifi_ip[0].trim().split("\\|");
                Log.i(TAG, tmp.toString());
                if (3 != tmp.length) {
                    return false;
                } else {
                    this.wifi_ip = tmp[0].trim();
                    this.wifi_mask = tmp[1].trim();
                    this.wifi_gateway = tmp[2].trim();
                }
            }
        }

        Log.i(TAG, "配置信息，wifiType: " + this.wifiType
                + ", ssid: " + this.ssid
                + ", passwd: " + this.passwd
                + ", repeate: " + this.repeate
                + ", host: " + this.host
                + ", count: " + this.count
                + ", timeout: " + this.timeout
                + ", datasize: " + this.datasize
                + ", listMapPingOkDo: " + this.listMapPingOkDo
                + ", wifi_ip_cfg: " + this.wifi_ip_cfg
                + ", wifi_ip: " + this.wifi_ip
                + ", wifi_mask: " + this.wifi_mask
                + ", wifi_gateway: " + this.wifi_gateway
        );
        configFileLastModified = file.lastModified();
        return true;
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void handlerWifiState(WifiManager wifiManager, int wifiState) {
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        Log.i(TAG, wifiInfo.toString());

        if (WIFI_STATE_ENABLED != wifiState) {
            destroyPingTest();
            LedControl.ledWifiConnect_no();
            return;
        }

        List<ScanResult>scanResultList = wifiManager.getScanResults();
        if (null == scanResultList || 0 == scanResultList.size()) {
            if (SupplicantState.SCANNING != wifiManager.getConnectionInfo().getSupplicantState()) {
                //如果没有发现热点，且当前没有扫描
                wifiManager.startScan();    //启动扫描
            }
            return;
        }

        if(!readConfig()) {
            Log.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName()+"["+Thread.currentThread().getStackTrace()[2].getLineNumber()+"] 配置文件读取错误");
            return;
        }
        if (null != wifiInfo) {
            if ( null == wifiInfo.getSSID()) {
                //ssid还不能获取
                if (ASSOCIATING != wifiInfo.getSupplicantState()) {
                    //如果当前没有尝试连接到热点
                    for (int index = 0; index < scanResultList.size(); index++) {
                        if (scanResultList.get(index).SSID.equals(ssid)) {
                            //扫描到的热点中存在配置文件中的ssid配置
                            Log.i(TAG, "当前热点ssid为空，且未连接，开始连接: " + ssid);
                            connectWifi(wifiManager, ssid, passwd, Integer.parseInt(wifiType));
                        }
                    }
                }
            } else {
                //ssid已经存在了
                if (wifiInfo.getSSID().equals("\"" + ssid + "\"")) {
                    //正在连接的ssid和配置文件中的相同
                    Log.i(TAG, "正在连接的wifi热点就是配置中的wifi热点: " +wifiInfo.getSSID());
                } else {
                    //正在连接的ssid和配置文件中的不同
                    Log.i(TAG, "尝试重新开始连接配置中的wifi: " +wifiInfo.getSSID() + "---->" + ssid);
                    connectWifi(wifiManager, ssid, passwd, Integer.parseInt(wifiType));
                }
            }
        } else {
            //应该不存在
        }

    }

    private boolean wifiIsConfigssid(WifiInfo wifiInfo) {
        if (null != wifiInfo && null != wifiInfo.getSSID()) {
            if (wifiInfo.getSSID().equals("\"" + ssid + "\"")
                    && ("\"" + ssid + "\"").equals(wifiInfo.getSSID())) {
                //连接上的热点就是配置文件中的热点，直接退出
                Log.i(TAG, Thread.currentThread().getStackTrace()[3].getMethodName()+ ": 正在连接的热点就是配置文件中的热点: " + wifiInfo.getSSID());
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

        try {
            String type = SetWifiState.getDeviceWLANAddressingType(context);
            Log.e(TAG, "TYPE: " + type);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        if (null != wifiInfo && null != wifiInfo.getSSID()) {
            if(!readConfig()){
                Log.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName()+"["+Thread.currentThread().getStackTrace()[2].getLineNumber()+"] 配置文件读取错误");
                return;
            }

            startPingTest(wifiManager, connectivityManager);

            if (!scanResultsIsContainConfigssid(scanResultList)) {
                Log.e(TAG, "扫描到的热点没有配置文件中的ssid, 当前连接: " + wifiInfo.getSSID());
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
                Log.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName()+"["+Thread.currentThread().getStackTrace()[2].getLineNumber()+"] 配置文件读取错误");
                return;
            }
            connectConfigWIFI(wifiManager);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public  void handlerSupplicant(Context context, WifiManager wifiManager, ConnectivityManager connectivityManager, SupplicantState supplicantState, int errNo) {
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        Log.i(TAG, wifiInfo.toString());


        if (DISCONNECTED == supplicantState) {
            //断开连接
            LedControl.ledWifiConnect_no();
            destroyPingTest();
        }

        if (!readConfig()) {
            Log.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName()+"["+Thread.currentThread().getStackTrace()[2].getLineNumber()+"] 配置文件读取错误");
            return;
        }

        if (ASSOCIATING == supplicantState) {
            //尝试连接到wifi
            if (!wifiIsConfigssid(wifiInfo)) {
                //连接的wifi不是配置文件中得wifi，尝试连接到配置文件中的wifi
                List<ScanResult> scanResultList = wifiManager.getScanResults();
                if (null != scanResultList) {
                    for (int index = 0; index < scanResultList.size(); index++) {
                        if (scanResultList.get(index).SSID.equals(ssid)) {
                            //扫描到的热点中存在配置文件中的ssid配置
                            Log.i(TAG, "当前热点ssid为空，且未连接，开始连接: " + ssid);
                            connectWifi(wifiManager, ssid, passwd, Integer.parseInt(wifiType));
                        }
                    }
                }
                return;
            }
            //灯闪烁
            LedControl.ledWifiConnect_ing();
        }

        if(errNo == WifiManager.ERROR_AUTHENTICATING){
            Log.i(TAG, "ERROR_AUTHENTICATING 身份验证不通过!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            return;
        }

         if (COMPLETED == supplicantState) {
             if (!wifiIsConfigssid(wifiInfo)) {
                 //连接的wifi不是配置文件中得wifi，尝试连接到配置文件中的wifi
                 List<ScanResult> scanResultList = wifiManager.getScanResults();
                 if (null != scanResultList) {
                     for (int index = 0; index < scanResultList.size(); index++) {
                         if (scanResultList.get(index).SSID.equals(ssid)) {
                             //扫描到的热点中存在配置文件中的ssid配置
                             Log.i(TAG, "当前热点ssid为空，且未连接，开始连接: " + ssid);
                             connectWifi(wifiManager, ssid, passwd, Integer.parseInt(wifiType));
                         }
                     }
                 }
                 return;
             }

             //启动ping测试
             startPingTest(wifiManager, connectivityManager);
         }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public  void handlefNetwork(WifiManager wifiManager, ConnectivityManager connectivityManager, NetworkInfo networkInfo, WifiInfo wifiInfo, String bssid) {
        Log.i(TAG, "wifiinfo: " + ((null==wifiInfo) ? null : wifiInfo.toString()));
        if (!readConfig()) {
            Log.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName()+"["+Thread.currentThread().getStackTrace()[2].getLineNumber()+"] 配置文件读取错误");
            return;
        }

        if (NetworkInfo.DetailedState.DISCONNECTED == networkInfo.getDetailedState()) {
            destroyPingTest();
            LedControl.ledWifiConnect_no();
        }

        if (NetworkInfo.DetailedState.CONNECTED == networkInfo.getDetailedState()) {
//            startPingTest(wifiManager, connectivityManager);
            try {
                String type = SetWifiState.getDeviceWLANAddressingType(context);
                Log.e(TAG, "TYPE: " + type);
                if (null != type && type.equals(wifi_ip_cfg)) {
                    startPingTest(wifiManager, connectivityManager);
                } else {
                    if (wifi_ip_cfg.equals("static")) {
                        SetWifiState.setWifiStaticIP(context, wifi_ip, Utils.calcPrefixLengthByMack(wifi_mask), wifi_gateway, "0.0.0.0");
                        startPingTest(wifiManager, connectivityManager);
                    } else {
                        SetWifiState.setWifiDHCPIP(context);
                        startPingTest(wifiManager, connectivityManager);
                    }
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
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
                    Log.e(TAG, "当前连接的wifi BSSID，在扫描到的列表中不存在: " + wifiInfo.getBSSID());
                    return false;
                }

                final String currIP = Utils.ipMultipleStringToSignleString(Utils.hisiIpLongToString(wifiInfo.getIpAddress()));
                final String currWifiFrequency = ""+scanResult.frequency;
                final String cmd = "ping"+ " -c " + count + " -s " + datasize + " -W " + timeout + " " + host;
                if (0 == iRrepeateTime && null != scanResult) {
                    isPingTestRunging = true;
                    ExecCommand execCommand = new ExecCommand(context, listMapPingOkDo);
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

                                    ExecCommand execCommand = new ExecCommand(context, listMapPingOkDo);
                                    execCommandList.add(execCommand);
                                    execCommand.writeLogToFile(logFile, cmd, currIP , wifiInfo.getBSSID(), currWifiFrequency, wifiInfo.getSSID(), repeate);
                                    ProcessBuilder processBuilder = new ProcessBuilder("sh", "-c" , cmd);
                                    execCommand.run(processBuilder);
                                    execCommand.printStdoutMessage(logFile, "stdout");
                                    execCommand.printStderrMessage(logFile, "stderr");

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
                    LedControl.ledWifiConnect_dhcp_succesful();
                    Log.i(TAG, "开启定时启动的ping测试, iRrepeateTime: " + iRrepeateTime);
                    lock.unlock();
                    return true;
                }
            }
        }

        Log.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName()+"["+Thread.currentThread().getStackTrace()[2].getLineNumber()+"] 条件不满足，没有进行ping测试");
        return false;
    }


    private boolean connectWifi(WifiManager wifiManager, String ssid, String passwd , int wifiType) {
        int netId;
        boolean enable;
        boolean reconnect;
        netId = wifiManager.addNetwork(WifiHelper.createWifiConfig(wifiManager, ssid, passwd, wifiType));
        if (-1 == netId) {
            Log.i(TAG, "添加新的网络到配置文件失败， " + "ssid： " + ssid +  ", wifiType:" + wifiType + "====>> networkID: " + netId);
        } else {
            Log.i(TAG, "添加新的网络到配置文件成功， " + "ssid： " + ssid +  ", wifiType:" + wifiType + "====>> networkID: " + netId);
        }

        enable = wifiManager.enableNetwork(netId, true); //true连接新的网络
        if (false == enable) {
            Log.i(TAG, "将新的网络描述,使能失败!!!");
            return false;
        }
        reconnect = wifiManager.reconnect();
        if (false == reconnect) {
            Log.i(TAG, "重新连接新的网络失败!!!");
            return false;
        }

        Log.i(TAG, "完成重新连接网络到 ----> " + ssid);
        return true;
    }

    private boolean connectConfigWIFI(WifiManager wifiManager) {
//        WifiConfiguration wifiConfig = null;
//        int netId = 0;
//        WifiInfo wifiInfo = wifiManager.getConnectionInfo();  //得到连接的wifi网络
//        List<WifiConfiguration> configurationList = wifiManager.getConfiguredNetworks();
//        for (WifiConfiguration configuration : configurationList) {
//            if (configuration.SSID == ssid) {
//                wifiConfig = configuration;
//                netId = wifiConfig.networkId;
//                break;
//            }
//        }
//
//        if (null == wifiConfig) {
//            netId = wifiManager.addNetwork(WifiHelper.createWifiConfig(wifiManager, ssid, passwd, Integer.parseInt(wifiType)));
//            if (-1 == netId) {
//                Log.i(TAG, "添加新的网络到配置文件失败， " + "ssid： " + ssid +  ", wifiType:" + wifiType + "====>> networkID: " + netId);
//                return false;
//            } else {
//                Log.i(TAG, "添加新的网络到配置文件成功， " + "ssid： " + ssid +  ", wifiType:" + wifiType + "====>> networkID: " + netId);
//            }
//        }

        boolean enable = wifiManager.enableNetwork(configNetworkID, true); //true连接新的网络
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
