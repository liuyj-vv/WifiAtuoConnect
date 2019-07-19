package com.changhong.wifi.auto.connect;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LedControl {
    static String TAG = LedControl.class.getPackage().getName();
    public static void ledCtrl(int status,String type)
    {
//        Log.i(TAG, "status: "+ status +", type: " + type);
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


    static Thread ledFlickerThread = null;
    static boolean isFlickerThreadRuing = false;

    public static void ledWifiNo() {
        if (null != ledFlickerThread) {
            try {
                isFlickerThreadRuing = false;
                ledFlickerThread.join();
                ledFlickerThread = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                LedControl.ledCtrl(1, "ir");
                Log.e(TAG, "=============亮灯");
            }
        }).start();
    }
    public static void ledWifiConnected() {
        if (null != ledFlickerThread) {
            try {
                isFlickerThreadRuing = false;
                ledFlickerThread.join();
                ledFlickerThread = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "=============灭灯");
                LedControl.ledCtrl(0, "ir");
            }
        }).start();
    }

    public static void ledWifiConnecting() {
        if (null == ledFlickerThread) {
            ledFlickerThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    int count = 0;
                    isFlickerThreadRuing = true;
                    Log.e(TAG, "=============闪烁  222");
                    while (true && isFlickerThreadRuing) {
                        try {
                            count ++;
                            //"type": "ir"
                            //"ir""power""network"....
                            //仅仅ir可以控制红灯亮灭
                            if (1 == count%2) {
                                LedControl.ledCtrl(1, "ir");
                            } else {
                                LedControl.ledCtrl(0, "ir");
                            }
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            ledFlickerThread.start();
        }
    }




}
