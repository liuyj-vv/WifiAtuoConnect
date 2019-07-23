package com.changhong.wifi.auto.connect;

import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.regex.Pattern;

public class ExecCommand {
    String TAG = ExecCommand.class.getPackage().getName();

    Process process = null;

    String logFilename = "";
    String log = "";

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    Process run(final ProcessBuilder processBuilder) {
        if (null != process) {
            return null;
        }

        try {
            FileKeyValueOP.writeAddLineToFile(logFilename, Utils.getCurrDate() + "[START] " + log);
            Log.e(TAG, Thread.currentThread().getStackTrace()[2].getMethodName()+"["+Thread.currentThread().getStackTrace()[2].getLineNumber()+"] " + Utils.getCurrDate() + " 开始进行一次ping测试");
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
                    Thread.sleep(50);
                    FileKeyValueOP.writeAddLineToFile(logFilename, Utils.getCurrDate() + "[END]   " + log + "\n");
                    Log.e(TAG, Thread.currentThread().getStackTrace()[2].getMethodName()+"["+Thread.currentThread().getStackTrace()[2].getLineNumber()+"] 一次ping测试结束了");
                    process = null;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        return process;
    }

    public void writeLogToFile(String filename, String cmd, String ip, String mac, String frequency, String ssid, String repeate) {
        this.logFilename = filename;
        this.log = "IP:" + ip + ", AP MAC:" + mac + ", AP frequency:" + frequency + ", ssid:" + ssid + ", repeate:" + repeate + ", cmd:" +cmd;
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

    public void printStdoutMessage(String filename, String tag) {
        if (null != process) {
            printMessage(process.getInputStream(), filename, tag);
        }
    }

    public void printStderrMessage(String filename, String tag) {
        if (null != process) {
            printMessage(process.getErrorStream(), filename, tag);
        }
    }

    private void printMessage(final InputStream input, final String filename, final String tag) {
        new Thread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            public void run() {
                Reader reader = new InputStreamReader(input);
                BufferedReader bufferedReader = new BufferedReader(reader);
                String line = null;
                Pattern pattern;
                int res;
                int index = -1;
                try {
                    while(null != (line = bufferedReader.readLine()) && null != process) {
                        Log.i(TAG, tag + " " + process + ": " + line);
                        FileKeyValueOP.writeAddLineToFile(filename, Utils.getCurrDate() + line);

                        String key = "received, ";
                        index = line.indexOf(key);
                        if (-1 != index && (index+key.length()) < line.length()) {
                            res = Integer.parseInt(line.substring(index+key.length()));
                            if (0 == res) {
                                LedControl.ledWifiPing_failure();
                            } else {
                                LedControl.ledWifiPing_successful();
                            }
                        }

                        index = line.indexOf("Redirect Network");
                        if (-1 != index) {
                            Log.d(TAG, line.substring(index));
                            LedControl.ledWifiPing_failure();
                        }
                        index = line.indexOf("time=");
                        if (-1 != index) {
                            Log.d(TAG, line.substring(index));
//                            res = Integer.parseInt(line.substring(index));
//                            Log.e(TAG, "返回结果： " + res);
                            LedControl.ledWifiPing_successful();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
