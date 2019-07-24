package com.changhong.wifi.auto.connect;

import android.content.ContentResolver;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.RemoteException;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class SetWifiState {
    static String TAG = SetWifiState.class.getPackage().getName();

    static boolean setWifiDHCPIP(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiConfiguration wifiConfig = null;
        WifiInfo connectionInfo = wifiManager.getConnectionInfo();  //得到连接的wifi网络

        List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration conf : configuredNetworks) {
            if (conf.networkId == connectionInfo.getNetworkId()) {
                Log.e(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "[" + Thread.currentThread().getStackTrace()[2].getLineNumber() + "]:" + conf.toString() );
                wifiConfig = conf;
                break;
            }
        }

        int iRes;
        boolean bRes;
        try {
            setIpAssignment("DHCP", wifiConfig);
            iRes = wifiManager.updateNetwork(wifiConfig); // apply the setting
            bRes = wifiManager.reassociate();
            Log.e(TAG, "iRes: " + iRes + ", bRes: " + bRes);
            Log.e(TAG, "wifi --> dhcp 设置成功！");
            return true;
        } catch (NoSuchFieldException e) {
            Log.e(TAG, "wifi --> dhcp 设置失败！");
            e.printStackTrace();
            return false;
        } catch (IllegalAccessException e) {
            Log.e(TAG, "wifi --> dhcp 设置失败！");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 设置静态ip地址的方法
     */
    static boolean setWifiStaticIP(Context context, String staticIP, int staticPrefixLength, String StaticGateway, String staticDNS) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiConfiguration wifiConfig = null;
        WifiInfo connectionInfo = wifiManager.getConnectionInfo();  //得到连接的wifi网络
        List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();

        for (WifiConfiguration conf : configuredNetworks) {
            if (conf.networkId == connectionInfo.getNetworkId()) {
                Log.e(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "[" + Thread.currentThread().getStackTrace()[2].getLineNumber() + "]:" + conf.toString() );
                wifiConfig = conf;
                break;
            }
        }

        try {
            int iRes = -1;
            boolean bRes = false;
            Log.e(TAG, "wifi --> static 设置成功！" + "iRes: " + iRes + ", bRes: " + bRes + wifiConfig);
            setIpAssignment("STATIC", wifiConfig);
            setIpAddress(InetAddress.getByName(staticIP), staticPrefixLength, wifiConfig);
            setGateway(InetAddress.getByName(StaticGateway), wifiConfig);
            setDNS(InetAddress.getByName(staticDNS), wifiConfig);
            iRes = wifiManager.updateNetwork(wifiConfig); // apply the setting
            bRes = wifiManager.reassociate();
            Log.e(TAG, "wifi --> static 设置成功！" + "iRes: " + iRes + ", bRes: " + bRes + wifiConfig);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "wifi --> static 设置失败！");
            return false;
        }
    }

    private static void setIpAssignment(String assign, WifiConfiguration wifiConf)
            throws SecurityException,
            IllegalArgumentException,
            NoSuchFieldException,
            IllegalAccessException {
        setEnumField(wifiConf, assign, "ipAssignment");
    }


    private static void setIpAddress(InetAddress addr, int prefixLength, WifiConfiguration wifiConf)
            throws SecurityException,
            IllegalArgumentException, NoSuchFieldException,
            IllegalAccessException, NoSuchMethodException,
            ClassNotFoundException, InstantiationException,
            InvocationTargetException {
        Object linkProperties = getField(wifiConf, "linkProperties");
        if (linkProperties == null) {
            return;
        }
        Class<?> laClass = Class.forName("android.net.LinkAddress");
        Constructor<?> laConstructor = laClass.getConstructor(new Class[] {InetAddress.class, int.class });
        Object linkAddress = laConstructor.newInstance(addr, prefixLength);

        ArrayList<Object> mLinkAddresses = (ArrayList<Object>) getDeclaredField(linkProperties, "mLinkAddresses");
        mLinkAddresses.clear();
        mLinkAddresses.add(linkAddress);
    }

    static Object getField(Object obj, String name)
            throws SecurityException,
            NoSuchFieldException,
            IllegalArgumentException,
            IllegalAccessException {
        Field f = obj.getClass().getField(name);
        Object out = f.get(obj);
        return out;
    }

    static Object getDeclaredField(Object obj, String name)
            throws SecurityException,
            NoSuchFieldException,
            IllegalArgumentException,
            IllegalAccessException {
        Field f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        Object out = f.get(obj);
        return out;
    }
    @SuppressWarnings({"unchecked", "rawtypes" })
    private static void setEnumField(Object obj, String value, String name)
            throws SecurityException,
            NoSuchFieldException,
            IllegalArgumentException,
            IllegalAccessException {
        Field f = obj.getClass().getField(name);
        f.set(obj, Enum.valueOf((Class<Enum>)f.getType(), value));
    }

    private static void setGateway(InetAddress gateway,WifiConfiguration wifiConf)
            throws SecurityException,
            IllegalArgumentException, NoSuchFieldException,
            IllegalAccessException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException,
            InvocationTargetException {
        Object linkProperties = getField(wifiConf, "linkProperties");
        if (linkProperties == null) {
            return;
        }

        if (android.os.Build.VERSION.SDK_INT >= 14) { // android4.x版本
            Class<?> routeInfoClass = Class.forName("android.net.RouteInfo");
            Constructor<?> routeInfoConstructor = routeInfoClass.getConstructor(new Class[] { InetAddress.class });
            Object routeInfo = routeInfoConstructor.newInstance(gateway);

            ArrayList<Object> mRoutes = (ArrayList<Object>)getDeclaredField(linkProperties, "mRoutes");
            mRoutes.clear();
            mRoutes.add(routeInfo);
        } else { // android3.x版本
            ArrayList<InetAddress> mGateways = (ArrayList<InetAddress>) getDeclaredField(linkProperties, "mGateways");
            mGateways.clear();
            mGateways.add(gateway);
        }
    }

    private static void setDNS(InetAddress dns, WifiConfiguration wifiConf)
            throws SecurityException,
            IllegalArgumentException,
            NoSuchFieldException,
            IllegalAccessException{
        Object linkProperties = getField(wifiConf, "linkProperties");
        if (linkProperties == null) {
            return;
        }
        ArrayList<InetAddress> mDnses = (ArrayList<InetAddress>)getDeclaredField(linkProperties, "mDnses");
        mDnses.clear(); // 清除原有DNS设置（如果只想增加，不想清除，词句可省略）
        mDnses.add(dns);//增加新的DNS
    }


    //获取当前的wifi的状态--static、dhcp、null
    public static String getDeviceWLANAddressingType(Context context) throws RemoteException {
        String state = null;
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiConfiguration wifiConfig = null;
        WifiInfo connectionInfo = wifiManager.getConnectionInfo();  //得到连接的wifi网络
        List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();

        if (null == configuredNetworks) {
            return null;
        }

        for (WifiConfiguration conf : configuredNetworks) {
            if (conf.networkId == connectionInfo.getNetworkId()) {
                wifiConfig = conf;
                break;
            }
        }

        try {
            state = getFields(wifiConfig, "ipAssignment");
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(state.toUpperCase().equals("STATIC".toUpperCase())) {
            return "static";
        } else if(state.toUpperCase().equals("DHCP".toUpperCase())){
            return "dhcp";
        } else {
            return "null";
        }
    }

    /** 属性字段名、值、数据类型 */
    public static String getFields(Object object, String fieldName)
            throws Exception {
        Field[] fields = object.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            String classType = field.getType().toString();
            int lastIndex = classType.lastIndexOf(".");
            classType = classType.substring(lastIndex + 1);
//            System.out.println("fieldName: " + field.getName() + ",  type: " + classType + ",  value: " + field.get(object));
            if (field.getName().equals(fieldName)) {
                return ""+field.get(object);
            }
        }
        return "null";
    }
}
