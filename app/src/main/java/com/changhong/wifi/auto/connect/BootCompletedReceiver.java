package com.changhong.wifi.auto.connect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class BootCompletedReceiver extends BroadcastReceiver {
    String TAG = BootCompletedReceiver.class.getPackage().getName();
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e(TAG, Thread.currentThread().getStackTrace()[2].getMethodName()+"["+Thread.currentThread().getStackTrace()[2].getLineNumber()+"]");

        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            Toast.makeText(context, "收到开机广播", Toast.LENGTH_LONG).show();
            Intent i = new Intent(context, WifiAutoConnectService.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startService(i);
        }
    }
}
