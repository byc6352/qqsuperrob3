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
	public static  String TAG = "byc001";//���Ա�ʶ��
	private static RobbedLuckyMoneyCount current;
	private Context context;
	public static double money;//�����ĺ����
	public static int count=0;//�����ĺ��������
	public static int preCount=0;//ǰһ�������ĺ��������
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
     * �������ݵ��߳�
     */
    private Thread RobbedLuckyMoneyChangeThread;
    /**
     * ��������count���ݱ仯��
     */
    public void onCountChangeListener()
    {
    	RobbedLuckyMoneyChangeThread = new Thread()
        {
            public void run()
            {
                while (true){
                	// ������ʱ����
                    try{
                        Thread.sleep(1000);
                    } catch (InterruptedException e)
                    {
                       System.out.println("error in onCountChangeListener Thread.sleep(100) " + e.getMessage());
                    }
                    if(preCount<count){
                    	 //���͹㲥��
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
