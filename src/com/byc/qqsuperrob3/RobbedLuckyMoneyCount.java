/**
 * 
 */
package com.byc.qqsuperrob3;

import android.content.Context;
import android.content.Intent;


/**
 * @author byc
 *
 */
public class RobbedLuckyMoneyCount {
	public static  String TAG = "byc001";//调试标识：
	private static RobbedLuckyMoneyCount current;
	private Context context;
	public static double money;//抢到的红包金额；
	public static int count=0;//抢到的红包数量；
	public static int preCount=0;//前一个抢到的红包数量；
    private RobbedLuckyMoneyCount(Context context) {
  	  this.context = context;
  	  TAG=Config.TAG;
    }
    public static synchronized RobbedLuckyMoneyCount getRobbedLuckyMoneyCount(Context context) {
        if(current == null) {
            current = new RobbedLuckyMoneyCount(context);
        }
        return current;
    }
    /**
     * 监听数据的线程
     */
    private Thread RobbedLuckyMoneyChangeThread;
    /**
     * 持续监听count数据变化：
     */
    public void onCountChangeListener()
    {
    	RobbedLuckyMoneyChangeThread = new Thread()
        {
            public void run()
            {
                while (true){
                	// 监听的时间间隔
                    try{
                        Thread.sleep(1000);
                    } catch (InterruptedException e)
                    {
                       System.out.println("error in onCountChangeListener Thread.sleep(100) " + e.getMessage());
                    }
                    if(preCount<count){
                    	 //发送广播，
                        Intent intent = new Intent(Config.ACTION_ROBBED_LUCKY_MONEY);
                        context.sendBroadcast(intent);
                        preCount=count;
                    }
                    count=count+1;
                }//while (true){
            }//public void run()
        };//RobbedLuckyMoneyChangeThread = new Thread()
        //RobbedLuckyMoneyChangeThread.start();
    }// public void onCountChangeListener()
}
