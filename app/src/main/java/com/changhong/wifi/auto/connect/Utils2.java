package com.changhong.wifi.auto.connect;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;

import java.util.List;

/**
 *  判断指定app是否已经启动等。。。
 */
public class Utils2 {
    static String TAG = Utils2.class.getPackage().getName();

    /**
     *  检测指定的 app 包名是否正在运行
     */
    public static boolean isAppAlive2(Context context, String packageName) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcessInfoList = activityManager.getRunningAppProcesses();
        for(ActivityManager.RunningAppProcessInfo runningAppProcessInfo : runningAppProcessInfoList) {
            for (int i=0; i<runningAppProcessInfo.pkgList.length; i++) {
                if (runningAppProcessInfo.pkgList[i].equals(packageName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isAppExistence(Context context, String packageName) {
        PackageInfo packageInfo;
        PackageManager packageManager = context.getPackageManager();
        try {
            packageInfo = packageManager.getPackageInfo(packageName, 0);
            return packageInfo != null;
        } catch (PackageManager.NameNotFoundException e) {
//            e.printStackTrace();
            //如果没有找到将会抛出异常
            return false;
        }


    }
    public static boolean isAppAndActivityExistence(Context context, String packageName, String activityName) {
        PackageManager packageManager = context.getPackageManager();
        ActivityInfo[] activities;
        List<PackageInfo> packageInfoList = packageManager.getInstalledPackages(PackageManager.GET_ACTIVITIES);
        for (int index=0; index<packageInfoList.size();index++) {
            if (packageInfoList.get(index).packageName.equals(packageName)) {
                activities = packageInfoList.get(index).activities;
                if (null == activities) {
                    return false;   //没有activity直接返回false
                }
                for (int j=0; j<activities.length; j++) {
                    if (packageInfoList.get(index).activities[j].name.equals(packageName+"."+activityName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }



    /**
     * 判断应用是否已经启动
     *
     * @param context     一个context
     * @param packageName 要判断应用的包名
     * @return boolean
     */
    public static boolean isAppAlive(Context context, String packageName) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> processInfos = activityManager.getRunningAppProcesses();
        for (int i = 0; i < processInfos.size(); i++) {
            Log.i(TAG, "isAppAlive: " + processInfos.get(i).processName);
            if (processInfos.get(i).processName.equals(packageName)) {
                return true;
            }
        }
        Log.i("NotificationLaunch", String.format("the %s is not running, isAppAlive return false", packageName));
        return false;
    }

    /**
     * 判断MainActivity是否活动
     *
     * @param context      一个context
     * @param activityName 要判断Activity
     * @return boolean
     */
    public static boolean isMainActivityAlive(Context context, String activityName) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> list = am.getRunningTasks(Integer.MAX_VALUE);
        for (ActivityManager.RunningTaskInfo info : list) {
            // 注意这里的 topActivity 包含 packageName和className，可以打印出来看看
            Log.i(TAG, "isMainActivityAlive: " + info.topActivity.toString() + ", " + info.baseActivity.toString());
            if (info.topActivity.toString().equals(activityName) || info.baseActivity.toString().equals(activityName)) {
                Log.i(TAG, info.topActivity.getPackageName() + " info.baseActivity.getPackageName()=" + info.baseActivity.getPackageName());
                return true;
            }
        }
        return false;
    }

    /**
     * 用来判断服务是否运行.
     *
     * @param context
     * @param className 判断的服务名字
     * @return true 在运行 false 不在运行
     */
    public static boolean isServiceRunning(Context context, String className) {
        boolean isRunning = false;
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        //此处只在前30个中查找，大家根据需要调整
        List<ActivityManager.RunningServiceInfo> serviceList = activityManager.getRunningServices(Integer.MAX_VALUE);
        if (!(serviceList.size() > 0)) {
            return false;
        }
        for (int i = 0; i < serviceList.size(); i++) {
            Log.i(TAG, "isServiceRunning: " + serviceList.get(i).service.getClassName());
            if (serviceList.get(i).service.getClassName().equals(className) == true) {
                isRunning = true;
                break;
            }
        }
        return isRunning;
    }

    /**
     * 检测某Activity是否在当前Task的栈顶
     */
    public static boolean isTopActivity(Context context, String activityName) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> runningTaskInfos = manager.getRunningTasks(1);
        String cmpNameTemp = null;
        if (runningTaskInfos != null) {
            cmpNameTemp = runningTaskInfos.get(0).topActivity.toString();
        }
        if (cmpNameTemp == null) {
            return false;
        }
        Log.i(TAG, "isTopActivity: " + cmpNameTemp);
        return cmpNameTemp.equals(activityName);
    }
}
