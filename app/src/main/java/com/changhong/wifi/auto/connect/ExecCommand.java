package com.changhong.wifi.auto.connect;

import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class ExecCommand {
    String TAG = ExecCommand.class.getPackage().getName();
    Process process;
    Process run(String cmd) {
        try {
            process = Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return process;
    }

    public void printMessage(final InputStream input, final String tag) {
        new Thread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            public void run() {
                Reader reader = new InputStreamReader(input);
                BufferedReader bf = new BufferedReader(reader);
                String line = null;
                try {
                    while((line=bf.readLine())!=null) {
                        Log.e(TAG, tag + " " + process + ": " + line);
                        writeLineToFile("/mnt/sda/sda1/ch_auto_test_result.txt", line+"\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private boolean writeLineToFile(String pathName, String line) {
        File file = new File(pathName);
        if (!file.exists()) {
            Log.i(TAG, "保存文件 "+ pathName +" 不存在!");
            return false;
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(pathName, true))){
            bw.write(line);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

}
