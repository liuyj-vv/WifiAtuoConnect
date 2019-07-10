package com.changhong.wifi.auto.connect;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class WifiAutoConnectService extends Service {
    String TAG = WifiAutoConnectService.class.getSimpleName();
    public WifiAutoConnectService() {
        while (true) {
            try {
                Thread.sleep(1000);
                Log.e(TAG, Thread.currentThread().getStackTrace()[2].getMethodName()+"["+Thread.currentThread().getStackTrace()[2].getLineNumber()+"]");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
