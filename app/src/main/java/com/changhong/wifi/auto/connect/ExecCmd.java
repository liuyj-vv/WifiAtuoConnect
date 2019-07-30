package com.changhong.wifi.auto.connect;

import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.concurrent.locks.ReentrantLock;

public class ExecCmd {
    private String TAG = ExecCmd.class.getPackage().getName();
    private ReentrantLock lock = new ReentrantLock();
    private Process process = null;
    private String[] command;

    ExecCmd(){

    }

    public Process run(ProcessBuilder processBuilder) {
        try {
            if (null == process) {
                process = processBuilder.start();

                //监听命令是否执行进程是否结束
                thread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return process;
    }


    public Process run(String cmd) {
        try {
            lock.lock();
            if (null == process) {
                this.command = new String[]{"sh", "-c", cmd};
                Log.i(TAG, "start command: " + this.command[2]);
                process = Runtime.getRuntime().exec(this.command);
                //打印输出到屏幕上
                printStdoutMessage();
                printStderrMessage();
                //监听命令是否执行进程是否结束
                thread.start();
            }
        } catch (IOException e) {
            process = null;
            e.printStackTrace();
        }

        lock.unlock();
        return process;
    }

    public void destroy() {
        lock.lock();
        if (null != process) {
            process.destroy();
            process = null;
        }
        lock.unlock();
    }

    public void printStdoutMessage() {
        lock.lock();
        if (null != process) {
            printMessage(process.getInputStream());
        }
        lock.unlock();
    }

    public void printStderrMessage() {
        lock.lock();
        if (null != process) {
            printMessage(process.getErrorStream());
        }
        lock.unlock();
    }

    private void printMessage(final InputStream input) {
        new Thread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            public void run() {
                Reader reader = new InputStreamReader(input);
                BufferedReader bufferedReader = new BufferedReader(reader);
                String line = null;
                try {
                    while(null != (line = bufferedReader.readLine()) && null != process) {
                        Log.i(TAG,  process + ": " + line);
                    }
                    reader.close();
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    Thread thread = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                process.waitFor();
                Log.i(TAG, "end command: " + command[2]);
                process = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    });
}
