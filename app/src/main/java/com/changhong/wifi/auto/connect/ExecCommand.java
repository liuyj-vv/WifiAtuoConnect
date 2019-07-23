package com.changhong.wifi.auto.connect;

import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.regex.Matcher;
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
                Matcher matcher;
                int res;
                int index = -1;
                try {
                    while(null != (line = bufferedReader.readLine()) && null != process) {
                        Log.i(TAG, tag + " " + process + ": " + line);
                        FileKeyValueOP.writeAddLineToFile(filename, Utils.getCurrDate() + line);

                        pattern = Pattern.compile("received, (\\d*?)%");
                        matcher = pattern.matcher(line);

                        if (matcher.find()) {
                            Log.d(TAG, "000000000000000000: "+matcher.group());
                            Log.d(TAG, "000000000000000000: "+matcher.group(1));
                            if (100 == Integer.parseInt(matcher.group(1))) {
                                LedControl.ledWifiPing_failure();
                            } else {
                                LedControl.ledWifiPing_successful();
                            }
                        }

                        pattern = Pattern.compile("Redirect Network");
                        matcher = pattern.matcher(line);
                        if (matcher.find()) {
                            LedControl.ledWifiPing_failure();
                        }

                        pattern = Pattern.compile("time=.*? ms");
                        matcher = pattern.matcher(line);
                        if (matcher.find()) {
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
