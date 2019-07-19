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

public class FileKeyValueOP {
    private static String TAG = FileKeyValueOP.class.getPackage().getName();

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    static boolean readFileKeyValue(String pathName, String key, String[] vaule) {
        String line;
        String strKey;
        String strValue;    // 返回 defValue 表示失败

        File file = new File(pathName);
        if (!file.exists()) {
            Log.i(TAG, "文件 "+ pathName +" 不存在");
            return false;
        }

        try (FileReader reader = new FileReader(pathName);
             BufferedReader br = new BufferedReader(reader) // 建立一个对象，它把文件内容转成计算机能读懂的语言
        ) {
            while (null != (line = br.readLine())) { // 一次读入一行数据
                line = line.trim();
                int index = line.indexOf('=');
                if (-1 != index) {
                    strKey = line.substring(0, index); // 获取键
                    if(key.trim().equals(strKey.trim())) { //比较
                        strValue = line.substring(index+1).trim(); // 获取值
                        if (-1 != (index = strValue.indexOf("//"))) {
                            strValue = strValue.substring(0, index).trim();
                        }
                        //Log.e(TAG, Thread.currentThread().getStackTrace()[2].getMethodName()+"["+Thread.currentThread().getStackTrace()[2].getLineNumber()+"]: " + strValue);
                        vaule[0] = strValue;
                        return true;
                    }
                }
            }
            br.close();
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static boolean writeAddLineToFile(String pathName, String line) {

        int start = pathName.lastIndexOf("/");
        String path = pathName.substring(0, start);

        File file = new File(path);
        if (!file.exists()) {
            Log.i(TAG, "保存路径 "+ path +" 不存在!");
            return false;
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(pathName, true))){
            bw.write(line+"\n");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
