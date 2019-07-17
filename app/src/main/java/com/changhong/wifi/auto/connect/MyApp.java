package com.changhong.wifi.auto.connect;

import android.app.Application;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;


public class MyApp extends Application{

	
	public final static String TAG = "chcommonfactory";
	private static MyApp instance;
	public static boolean bUSB1 = false;
	public static boolean bUSB2	= false;

	
	int cpuNums = Runtime.getRuntime().availableProcessors();
	public ExecutorService mExeCutorService =Executors.newFixedThreadPool(cpuNums*4);

	//for okhttp
	public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	public static String  interfaceUrl = "http://127.0.0.1:18176/rpc";


	private static final int CONNECT_TIMEOUT = 5;
	private static final int READ_TIMEOUT = 5;
	private static final int WRITE_TIMEOUT = 5;

	private static OkHttpClient mHttpClient;
	//for okhttp end


	public MyApp()
	{
		
	}
	@Override
	public void onCreate() {
		super.onCreate();
		
		Log.i(TAG, "oncreat !!!");



	}

	
	public synchronized static MyApp getInstance() {
		if (null == instance) {
			instance = new MyApp();
		}
		return instance;
	}


	public static OkHttpClient getOkHttp()
	{

		if(mHttpClient == null )
		{
			mHttpClient= new OkHttpClient.Builder().readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
					.writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS).connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS).build();
		}

		return mHttpClient;



	}


}
