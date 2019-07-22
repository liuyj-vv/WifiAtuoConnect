package com.changhong.wifi.auto.connect;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LedControl {
    static String TAG = LedControl.class.getPackage().getName();
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

    static List<Thread> threadList = new ArrayList<>();


    //未连接，同时也没有开始连接
    public static void ledWifiConnectNo() {
        ledCloseCycle();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "=============灯灭");
                LedControl.ledCtrl(0, "ir");
            }
        }).start();
    }

    //正在连接到wifi
    public static void ledWifiConnecting() {
        ledCloseCycle();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                boolean isLightFlag = true;
                Log.e(TAG, "============= 快速闪烁");
                while (true) {
                    try {
                        if (isLightFlag) {
                            LedControl.ledCtrl(0, "ir");
                            isLightFlag = false;
                        } else {
                            LedControl.ledCtrl(1, "ir");
                            isLightFlag = true;
                        }
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        });
        threadList.add(thread);
        thread.start();
    }

    //dhcp获取ip成功
    public static void ledWifiDhcpSuccesful() {
        ledCloseCycle();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                boolean isLightFlag = true;
                Log.e(TAG, "============= 一秒闪烁");
                while (true) {
                    try {
                        if (isLightFlag) {
                            LedControl.ledCtrl(0, "ir");
                            isLightFlag = false;
                        } else {
                            LedControl.ledCtrl(1, "ir");
                            isLightFlag = true;
                        }
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        });
        threadList.add(thread);
        thread.start();
    }

    //ping失败
    public static void ledWifiPingFailure() {
        ledCloseCycle();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                boolean isLightFlag = true;
                Log.e(TAG, "============= 三秒闪烁");
                while (true) {
                    try {
                        if (isLightFlag) {
                            LedControl.ledCtrl(0, "ir");
                            isLightFlag = false;
                        } else {
                            LedControl.ledCtrl(1, "ir");
                            isLightFlag = true;
                        }
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        });
        threadList.add(thread);
        thread.start();
    }


    //ping成功
    public static void ledWifiPingSuccessful() {
        ledCloseCycle();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "============= 灯亮");
                LedControl.ledCtrl(1, "ir");
            }
        }).start();

    }



    static private void ledCloseCycle() {
        for (int index=0; index<threadList.size(); index++) {
            threadList.get(index).interrupt();
            try {
                threadList.get(index).join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        threadList.clear();
    }

}
