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
        Log.i(TAG, "status: "+ status +", type: " + type);

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
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
