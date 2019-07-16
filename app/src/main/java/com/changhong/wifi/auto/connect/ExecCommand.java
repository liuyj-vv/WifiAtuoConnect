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

    String logFile;
    String log;

    Process run(final ProcessBuilder processBuilder) {
        if (null != process) {
            return null;
        }

        try {
            process = processBuilder.start();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        new Thread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void run() {
                try {
                    process.waitFor();
                    FileKeyValueOP.writeAddLineToFile(logFile, Utils.getCurrDate() + ": " + log + "\n");
                    Log.e(TAG, Thread.currentThread().getStackTrace()[2].getMethodName()+"["+Thread.currentThread().getStackTrace()[2].getLineNumber()+"] 监听程序结束了" + process.exitValue());
                    Thread.sleep(100);
                    process = null;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        return process;
    }

    public void exitLog(String logFile, String log) {
        this.logFile = logFile;
        this.log = log;
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
                    while(null != (line = bufferedReader.readLine()) && null != process) {
                        Log.e(TAG, tag + " " + process + ": " + line);
                        FileKeyValueOP.writeAddLineToFile("/mnt/sda/sda1/ch_auto_test_result.txt", line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
