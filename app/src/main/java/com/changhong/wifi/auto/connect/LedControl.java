package com.changhong.wifi.auto.connect;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;

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
        Log.e(TAG, "============= 连接断开，没有连接: " + stateCurr);

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
        Log.e(TAG, "============= 正在连接: " + stateCurr);

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
        Log.e(TAG, "============= 连接成功，且dhcp获取成功: " + stateCurr);

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
        Log.e(TAG, "============= 进行ping测试: " + stateCurr);

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
        Log.e(TAG, "============= ping测试失败: " + stateCurr);

        if (stateCurr == State.Ping_faile) {
            return;
        } else {
            stateCurr = State.Ping_faile;
        }
        ledCloseAllCycle();

        ledCycle(3000);
    }



    //ping成功
    public static void ledWifiPingSuccessful() {
        Log.e(TAG, "============= ping测试成功: " + stateCurr);

        if (stateCurr == State.Ping_successful) {
            return;
        } else {
            stateCurr = State.Ping_successful;
        }
        ledCloseAllCycle();

        ledON();
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

    static private ReentrantLock lock = new ReentrantLock(); // 需要保证多个线程使用的是同一个锁
    static private void ledCycle(final int s) {
        lock.lock();
        cycleLedThread = new Thread(new Runnable() {
            @Override
            public void run() {
                isRuning = true;
                boolean isLightFlag = true;
                while (isRuning) {
                    try {
                        if (isLightFlag) {
                            LedControl.ledCtrl(0, "ir");
                            isLightFlag = false;
                        } else {
                            LedControl.ledCtrl(1, "ir");
                            isLightFlag = true;
                        }
                        Thread.sleep(s);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Log.e(TAG, "线程 进入中断");
                        break;
                    }
                }
                Log.e(TAG, "线程结束部分");
            }
        });
        cycleLedThread.start();
        lock.unlock();
    }

    static boolean isRuning = false;
    static private void ledCloseAllCycle() {
        lock.lock();
        if (null != cycleLedThread) {
            try {
                isRuning = false;
                cycleLedThread.interrupt();
                cycleLedThread.join();
                cycleLedThread = null;
                Log.e(TAG, "interrupt 线程退出");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        lock.unlock();
    }
}
