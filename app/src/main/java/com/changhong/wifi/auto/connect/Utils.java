package com.changhong.wifi.auto.connect;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {
    public static String getCurrDate() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
        return simpleDateFormat.format(new Date()) + ": "; // new Date()为获取当前系统时间
    }

    /**
     * 为简化地址获取定制
     */
    public static  String hisiIpLongToString(int addr) {
        return longToIP(intToBytesLittle(addr));
    }

    public static int calcPrefixLengthByMack(String strip) {//输入子网掩码获得长度
        StringBuffer sbf;
        String str;
        // String strip = "255.255.255.0"; // 子网掩码
        int inetmask = 0, count = 0; // 子网掩码缩写代码
        String[] ipList = strip.split("\\.");
        for (int n = 0; n < ipList.length; n++) {
            sbf = toBin(Integer.parseInt(ipList[n]));
            str = sbf.reverse().toString();
            Log.i("wqm", ipList[n] + "---" + str);


            // 统计2进制字符串中1的个数
            count = 0;
            for (int i = 0; i < str.length(); i++) {
                i = str.indexOf('1', i); // 查找 字符'1'出现的位置
                if (i == -1) {
                    break;
                }
                count++; // 统计字符出现次数
            }
            inetmask += count;
        }
        return inetmask;
    }

    public static StringBuffer toBin(int x) {
        StringBuffer result = new StringBuffer();
        result.append(x % 2);
        x /= 2;
        while (x > 0) {
            result.append(x % 2);
            x /= 2;
        }
        return result;
    }

    // 001.001.001.001 ---> 1.1.1.1
    public static String ipMultipleStringToSignleString(String ip) {
        String[] strings = ip.split("\\.");
        if (4 != strings.length) {
            return ip;
        }
        return Integer.parseInt(strings[0]) + "." + Integer.parseInt(strings[1]) + "." + Integer.parseInt(strings[2]) + "." + Integer.parseInt(strings[3]);
    }

    public static int hisiIpStringToInt(String string) {
        return intToBytesLittle((int) ipToLong(string));
    }

    /**
     * ip地址转成long型数字
     * 将IP地址转化成整数的方法如下：
     * 1、通过String的split方法按.分隔得到4个长度的数组
     * 2、通过左移位操作（<<）给每一段的数字加权，第一段的权为2的24次方，第二段的权为2的16次方，第三段的权为2的8次方，最后一段的权为1
     *
     * @param strIp
     * @return
     */
    public static long ipToLong(String strIp) {
        String[] ip = strIp.split("\\.");
        return (Long.parseLong(ip[0]) << 24) + (Long.parseLong(ip[1]) << 16) + (Long.parseLong(ip[2]) << 8) + Long.parseLong(ip[3]);
    }

    /**
     * 将十进制整数形式转换成127.0.0.1形式的ip地址
     * 将整数形式的IP地址转化成字符串的方法如下：
     * 1、将整数值进行右移位操作（>>>），右移24位，右移时高位补0，得到的数字即为第一段IP。
     * 2、通过与操作符（&）将整数值的高8位设为0，再右移16位，得到的数字即为第二段IP。
     * 3、通过与操作符吧整数值的高16位设为0，再右移8位，得到的数字即为第三段IP。
     * 4、通过与操作符吧整数值的高24位设为0，得到的数字即为第四段IP。
     *
     * @param longIp
     * @return
     */
    public static String longToIP(long longIp) {
        StringBuilder result = new StringBuilder(15);
        String temp;
        for (int i = 0; i < 4; i++) {
            temp = Long.toString(longIp & 0xff);
            if (1 == temp.length()) {
                temp = "00" + temp;
            } else if (2 == temp.length()) {
                temp = "0" + temp;
            }
            result.insert(0, temp);
            if (i < 3) {
                result.insert(0, '.');
            }
            longIp = longIp >> 8;
        }
        return result.toString();
    }

    /**
     * 以大端模式将int转成byte[]
     */
    public static int intToBytesBig(int value) {
        byte[] src = new byte[4];
        src[0] = (byte) ((value >> 24) & 0xFF);
        src[1] = (byte) ((value >> 16) & 0xFF);
        src[2] = (byte) ((value >> 8) & 0xFF);
        src[3] = (byte) (value & 0xFF);
        return byteArrayToInt(src);
    }

    /**
     * 以小端模式将int转成byte[]
     *
     * @param value
     * @return
     */
    public static int intToBytesLittle(int value) {
        byte[] src = new byte[4];
        src[3] = (byte) ((value >> 24) & 0xFF);
        src[2] = (byte) ((value >> 16) & 0xFF);
        src[1] = (byte) ((value >> 8) & 0xFF);
        src[0] = (byte) (value & 0xFF);
        return byteArrayToInt(src);
    }

    /**
     * byte 数组与 int 的相互转换
     * */
    public static int byteArrayToInt(byte[] b) {
        return  (b[3] & 0xFF) |
                (b[2] & 0xFF) << 8 |
                (b[1] & 0xFF) << 16 |
                (b[0] & 0xFF) << 24;
    }

    public static byte[] intToByteArray(int a) {
        return new byte[]{
                (byte) ((a >> 24) & 0xFF),
                (byte) ((a >> 16) & 0xFF),
                (byte) ((a >> 8) & 0xFF),
                (byte) (a & 0xFF)
        };
    }


    public static String byteMacToString(byte[] mac) {
        if (null == mac) {
            return "null";
        }
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < mac.length; i++) {
            if (i != 0) {
                sb.append("");
            }
            // mac[i] & 0xFF 是为了把byte转化为正整数
            String s = Integer.toHexString(mac[i] & 0xFF);
            sb.append(s.length() == 1 ? 0 + s : s);
        }
        return sb.toString().toUpperCase();
    }
}
