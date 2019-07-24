package com.changhong.wifi.auto.connect;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class LedControl {
    enum State {
        Connect_no,
        Connect_ing,
        Connect_dhcp_successful,

        Ping_ing,
        Ping_faile,
        Ping_successful
    }
    static String TAG = LedControl.class.getPackage().getName();
    static State stateCurr = State.Connect_no;


    public static void ledCtrl(int status,String type) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("jsonrpc","2.0");
            jsonObject.put("method","ledCtrl");
            JSONObject obj2 = new JSONObject();
            obj2.put("status",status );//0 or 1
            obj2.put("type", type);//network standy ir
            jsonObject.put("params",obj2);
            jsonObject.put("id",1);

        }catch (JSONException e)
        {
            e.printStackTrace();
        }

        String jsonStr = jsonObject.toString();
        RequestBody body = RequestBody.create(MyApp.JSON, jsonStr);
        Request request = new Request.Builder().url(MyApp.interfaceUrl).addHeader("content-type", "application/json;charset:utf-8")
                .post(body).build();
        try {
            Response response = MyApp.getOkHttp().newCall(request).execute();
            if (!response.isSuccessful()) {
                Log.i(TAG, "----------ledCtrl----- fail");
            }
            response.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    static Thread cycleLedThread = null;    //同一时刻只能有一个灯的状态，闪烁也只可能有一种。

    //未连接，同时也没有开始连接
    public static void ledWifiConnect_no() {
        Log.d(TAG, "============= 连接断开，没有连接: " + stateCurr);

        if (stateCurr == State.Connect_no) {
            return;
        } else {
            stateCurr = State.Connect_no;
        }
        ledCloseAllCycle();

        ledOFF();
    }

    //正在连接到wifi
    public static void ledWifiConnect_ing() {
        Log.d(TAG, "============= 正在连接: " + stateCurr);

        if (stateCurr == State.Connect_ing) {
            return;
        } else {
            stateCurr = State.Connect_ing;
        }
        ledCloseAllCycle();

        ledCycle(100);
    }

    //dhcp获取ip成功
    public static void ledWifiConnect_dhcp_succesful() {
        Log.d(TAG, "============= 连接成功，且dhcp获取成功: " + stateCurr);

        if (stateCurr == State.Connect_dhcp_successful) {
            return;
        } else {
            stateCurr = State.Connect_dhcp_successful;
        }

        ledCloseAllCycle();

        ledCycle(1000);
    }

    //正在进行ping测试
    public static void ledWifiPing_ing() {
        Log.d(TAG, "============= 进行ping测试: " + stateCurr);

        if (stateCurr == State.Ping_ing) {
            return;
        } else {
            stateCurr = State.Ping_ing;
        }
        ledCloseAllCycle();

        ledCycle(1000);
    }

    //ping失败
    public static void ledWifiPing_failure() {
        Log.d(TAG, "============= ping测试失败: " + stateCurr);

        if (stateCurr == State.Ping_faile) {
            return;
        } else {
            stateCurr = State.Ping_faile;
        }
        ledCloseAllCycle();

        ledCycle(3000);
    }



    //ping成功
    public static void ledWifiPing_successful(final Context context, final List<Map<String, String>> listMapPingOkDo) {
        Log.d(TAG, "============= ping测试成功: " + stateCurr);

        if (stateCurr == State.Ping_successful) {
            return;
        } else {
            stateCurr = State.Ping_successful;
        }
        ledCloseAllCycle();

        ledON();

        new Thread(new Runnable() {
            @Override
            public void run() {
                String packageName;
                String activityName;
                for (int index=0; index<listMapPingOkDo.size(); index++) {
                    packageName = listMapPingOkDo.get(index).get("package");
                    activityName = listMapPingOkDo.get(index).get("activity");
                    if (!Utils2.isAppAndActivityExistence(context, packageName, activityName)) {
                        // 系统中没有安装这个应用
                        Log.i(TAG, "在系统中不存在 包名/.类名: " + packageName+"/."+activityName);
                        continue;
                    }
                    if (!Utils2.isAppAlive2(context, packageName)) {
                        try {
                            String cmd ="am start -n " + packageName + "/." + activityName;
                            Log.i(TAG, "执行命令，启动应用: " + cmd);
                            Runtime.getRuntime().exec(cmd);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Log.i(TAG, "应用已启动: " + packageName);
                    }
                }
            }
        }).start();
    }

    static private void ledON() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                LedControl.ledCtrl(1, "ir");
            }
        }).start();
    }
    static private void ledOFF() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                LedControl.ledCtrl(0, "ir");
            }
        }).start();
    }

    static private ReentrantLock lock = new ReentrantLock();
    static private void ledCycle(final int s) {
        lock.lock();
        cycleLedThread = new Thread(new Runnable() {
            @Override
            public void run() {
                isRuning = true;
                boolean isLightFlag = true;
                boolean isNotFirstRun = false;
                while (isRuning) {
                    try {
                        if (isNotFirstRun) {
                            Thread.sleep(s);
                        }

                        if (isLightFlag) {
                            LedControl.ledCtrl(1, "ir");
                            isLightFlag = false;
                        } else {
                            LedControl.ledCtrl(0, "ir");
                            isLightFlag = true;
                        }
                        isNotFirstRun = true;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Log.d(TAG, "线程 进入中断");
                        break;
                    }
                }
                Log.d(TAG, "线程结束部分");
            }
        });
        cycleLedThread.start();
        Log.d(TAG, "interrupt led启动闪烁： " + isRuning + " " + cycleLedThread + " " + s);
        lock.unlock();
    }

    static boolean isRuning = false;
    static private void ledCloseAllCycle() {
        lock.lock();
        if (null != cycleLedThread) {
            try {
                isRuning = false;
                Log.d(TAG, "interrupt led退出闪烁： " + isRuning + " " + cycleLedThread);
                cycleLedThread.interrupt();
                cycleLedThread.join();
                Log.d(TAG, "interrupt 线程退出");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        cycleLedThread = null; //防止join函数出现异常而未成功执行
        lock.unlock();
    }
}
