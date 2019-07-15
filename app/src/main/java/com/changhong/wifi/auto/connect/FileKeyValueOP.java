package com.changhong.wifi.auto.connect;

import android.os.Build;
import android.support.annotation.RequiresApi;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class FileKeyValueOP {
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    static String readFileKeyValue(String pathName, String key, String defValue) {
        String line;
        String strKey;
        String strValue;    // 返回 defValue 表示失败
        try (FileReader reader = new FileReader(pathName);
             BufferedReader br = new BufferedReader(reader) // 建立一个对象，它把文件内容转成计算机能读懂的语言
        ) {
            while (null != (line = br.readLine().trim())) { // 一次读入一行数据
                int index = line.indexOf('=');
                if (-1 != index) {
                    strKey = line.trim().substring(0, index); // 获取键
                    if(key.trim().equals(strKey.trim())) { //比较
                        strValue = line.substring(index+1); // 获取值
//                        Log.e(TAG, Thread.currentThread().getStackTrace()[2].getMethodName()+"["+Thread.currentThread().getStackTrace()[2].getLineNumber()+"]: " + str_after);
                        return strValue;
                    }
                }
            }
            br.close();
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return defValue;
    }
}
