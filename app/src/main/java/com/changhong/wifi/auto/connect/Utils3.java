package com.changhong.wifi.auto.connect;

import android.annotation.TargetApi;
import android.os.Build;
import android.support.annotation.FontRes;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;

public class Utils3 {
    static String TAG = Utils.class.getPackage().getName();

    enum MODE{
        CLASS_PUBLIC,
        CURR_CLASS_ALL,
    }
    /**
     * 通过反射获取类的所有变量,并打印出来
     */
    public static void printFields(Object object, MODE mode) {
        if(null == object){
            Log.i(TAG, "============= printFields object 输入参数为空 ======================");
            return;
        }
        //1.获取并输出类的名称
        Class mClass = object.getClass();
        System.out.println("类的名称：" + mClass.getName());
        Field[] fields;
        if (MODE.CLASS_PUBLIC == mode) {
            //2.1 获取所有 public 访问权限的变量
            // 包括本类声明的和从父类继承的
            Log.i(TAG, "=============  "+ object.getClass().getName() +" 所有共有的变量  ======================");
            fields = mClass.getFields();
        } else if (MODE.CURR_CLASS_ALL == mode){
            //2.2 获取所有本类(仅仅本类)声明的变量（不问访问权限）
            Log.i(TAG, "=============  "+ object.getClass().getName() +" 本类所有的变量  ======================");
            fields = mClass.getDeclaredFields();
        } else {
            return;
        }

        //3. 遍历变量并输出变量信息
        for (Field field : fields) {
            //获取访问权限并输出，输出变量的类型及变量名
            int modifiers = field.getModifiers();
            Log.i(TAG, Modifier.toString(modifiers) + " " + field.getType().getName() + " " + field.getName());
        }
    }

    /**
     * 通过反射获取类的所有方法
     */

    public static void printMethods(Object object, MODE mode) {
        if (null == object) {
            Log.i(TAG, "============= printMethods object 输入参数为空 ======================");
            return;
        }

        //1.获取并输出类的名称
        Class mClass = object.getClass();
        System.out.println("类的名称：" + mClass.getName());

        Method[] mMethods;
        if (MODE.CLASS_PUBLIC == mode) {
            //2.1 获取所有 public 访问权限的方法
            //包括自己声明和从父类继承的
            Log.i(TAG, "=============  " + object.getClass().getName() + " 所有公有的的方法  ======================");
            mMethods = mClass.getMethods();
        } else if (MODE.CURR_CLASS_ALL == mode) {
            //2.2 获取所有本类的的方法（不问访问权限）
            Log.i(TAG, "=============  " + object.getClass().getName() + " 本类所有的的方法  ======================");
            mMethods = mClass.getDeclaredMethods();
        } else {
            return;
        }

        //3.遍历所有方法
        for (Method method : mMethods) {
            //获取并输出方法的访问权限（Modifiers：修饰符）
            int modifiers = method.getModifiers();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                //获取并输出方法的返回值类型
                Class returnType = method.getReturnType();
                //打印：获取并输出方法的访问权限、返回值类型、方法名称
                Log.i(TAG, Modifier.toString(modifiers) + " " + returnType.getName() + " " + method.getName() + "( ");

                //获取并输出方法的所有参数
                Parameter[] parameters = method.getParameters();
                for (Parameter parameter : parameters) {
                    Log.i(TAG, parameter.getType().getName() + " " + parameter.getName() + ",");
                }

                //获取并输出方法抛出的异常
                Class[] exceptionTypes = method.getExceptionTypes();
                if (exceptionTypes.length == 0) {
                    Log.i(TAG, " )");
                } else {
                    for (Class c : exceptionTypes) {
                        Log.i(TAG, " ) throws " + c.getName());
                    }
                }
            } else {
                Log.i(TAG, "\t" + method.toString());
            }
        }
    }

    public static Object getClassField(Object object, String name) {
        Class mClass = object.getClass();
        Field[] fields;

        //2.1 获取所有 public 访问权限的变量
        // 包括本类声明的和从父类继承的
        fields = mClass.getFields();
        for (Field field : fields) {
            if (field.getName().equals(name)) {
                try {
                    return field.get(object);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        //2.2 获取所有本类(仅仅本类)声明的变量（不问访问权限）
        fields = mClass.getDeclaredFields();
        for (Field field : fields) {
            if (field.getName().equals(name)) {
                try {
                    field.setAccessible(true);
                    return field.get(object);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public static Object runClassMethod(Object object, String method) {
        Class classObject = object.getClass();
        Method[] mMethods;

        //2.1 获取所有 public 访问权限的方法
        //包括自己声明和从父类继承的
        mMethods = classObject.getMethods();

        //2.2 获取所有本类的的方法（不问访问权限）
        mMethods = classObject.getDeclaredMethods();

        return object;
    }
}