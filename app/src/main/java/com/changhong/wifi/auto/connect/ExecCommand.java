package com.changhong.wifi.auto.connect;

import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class ExecCommand {
    String TAG = ExecCommand.class.getPackage().getName();

    Process process = null;

    Process run(String cmd) {
        if (null != process) {
            return null;
        }

        try {
            process = Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    process.waitFor();
                    process = null;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        return process;
    }

    public boolean destroy() {
        if (null != process) {
            process.destroy();
            process = null;
            return true;
        } else {
            return false;
        }
    }

    public boolean isRuning() {
        if (null != process ) {
            return true;
        } else {
            return false;
        }
    }

    public void printMessage(final InputStream input, final String tag) {
        new Thread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            public void run() {
                Reader reader = new InputStreamReader(input);
                BufferedReader bufferedReader = new BufferedReader(reader);
                String line = null;
                try {
                    while(null != (line=bufferedReader.readLine()) && null != process) {
                        Log.e(TAG, tag + " " + process + ": " + line);
                        FileKeyValueOP.writeAddLineToFile("/mnt/sda/sda1/ch_auto_test_result.txt", line+"\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
