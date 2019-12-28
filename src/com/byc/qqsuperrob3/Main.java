/**
 * 
 */
package com.byc.qqsuperrob3;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

import org.json.JSONObject;

import com.byc.qqsuperrob3.Config;
import com.byc.qqsuperrob3.util.SpeechUtil;

import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Random;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static android.os.SystemClock.sleep;
import static android.widget.Toast.LENGTH_LONG;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookConstructor;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findFirstFieldByExactType;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.newInstance;
import static java.lang.String.valueOf;
import static com.byc.qqsuperrob3.HideModule.hideModule;
import static com.byc.qqsuperrob3.XposedUtils.findFieldByClassAndTypeAndName;
import static com.byc.qqsuperrob3.XposedUtils.findResultByMethodNameAndReturnTypeAndParams;
import static com.byc.qqsuperrob3.enums.PasswordStatus.CLOSE;
import static com.byc.qqsuperrob3.enums.PasswordStatus.SEND;
import static com.byc.qqsuperrob3.enums.ReplyStatus.ALL;
import static com.byc.qqsuperrob3.enums.ReplyStatus.GOT;
import static com.byc.qqsuperrob3.enums.ReplyStatus.MISSED;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import android.util.Log;
/**
 * @author byc
 *
 */
public class Main implements IXposedHookLoadPackage {

    public static final String QQ_PACKAGE_NAME = "com.tencent.mobileqq";
    private static final String WECHAT_PACKAGE_NAME = "com.tencent.mm";

    private static long msgUid;
    private static String senderuin;
    private static String frienduin;
    private static String from;
    private static int istroop;
    private static String selfuin;
    private static Context globalContext;
    private static Object HotChatManager;
    private static Object TicketManager;
    private static Object TroopManager;
    private static Object DiscussionManager;
    private static Object FriendManager;
    private static Bundle bundle;
    private static Object globalQQInterface = null;
    private static int n = 1;
    private static int versionCode;

    
    private void dohook(final XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {

        initVersionCode(loadPackageParam);

        findAndHookMethod("com.tencent.mobileqq.data.MessageForQQWalletMsg", loadPackageParam.classLoader, "doParse", new
                XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!PreferencesUtils.open() || msgUid == 0) {
                            return;
                        }
                        msgUid = 0;

                        int messageType = (int) getObjectField(param.thisObject, "messageType");
                        if (messageType == 6 && PreferencesUtils.password() == CLOSE) {
                            return;
                        }
                        XposedBridge.log("MessageForQQWalletMsg:doParse：messageType：" +messageType);

                        Object mQQWalletRedPacketMsg = getObjectField(param.thisObject, "mQQWalletRedPacketMsg");
                        String redPacketId = getObjectField(mQQWalletRedPacketMsg, "redPacketId").toString();
                        XposedBridge.log("MessageForQQWalletMsg:mQQWalletRedPacketMsg：redPacketId ：" +redPacketId );
                        String authkey = (String) getObjectField(mQQWalletRedPacketMsg, "authkey");
                        XposedBridge.log("MessageForQQWalletMsg:mQQWalletRedPacketMsg：authkey：" +authkey );
                        Object SessionInfo = newInstance(findClass("com.tencent.mobileqq.activity.aio.SessionInfo", loadPackageParam.classLoader));
                        findFieldByClassAndTypeAndName(findClass("com.tencent.mobileqq.activity.aio.SessionInfo", loadPackageParam.classLoader), String.class, "a").set(SessionInfo, frienduin);
                        findFieldByClassAndTypeAndName(findClass("com.tencent.mobileqq.activity.aio.SessionInfo", loadPackageParam.classLoader), Integer.TYPE, "a").setInt(SessionInfo, istroop);
                        XposedBridge.log("SessionInfo：frienduin：Set：" +frienduin );
                        XposedBridge.log("SessionInfo：istroop：Set：" +istroop );
                        Object QQWalletTransferMsgElem = XposedHelpers.getObjectField(mQQWalletRedPacketMsg, "elem");
                        String password = XposedHelpers.getObjectField(QQWalletTransferMsgElem, "title").toString();//红包语
                        XposedBridge.log("mQQWalletRedPacketMsg：elem：title（password）：" +password );
                        Object messageParam = newInstance(findClass("com.tencent.mobileqq.activity.ChatActivityFacade$SendMsgParams", loadPackageParam.classLoader));
                        //不抢自己的红包；
                        if (selfuin.equals(senderuin) && PreferencesUtils.self()) {
                            return;
                        }

                        String group = PreferencesUtils.group();
                        if (!TextUtils.isEmpty(group)) {
                            for (String group1 : group.split(",")) {
                                 if (frienduin.equals(group1) || senderuin.equals(group1)) {
                                     if(istroop == 1 && senderuin.equals(group1)) {
                                        from = "指定人不抢" + "\n" + "来自群:" + getObjectField(findResultByMethodNameAndReturnTypeAndParams(TroopManager, "a", "com.tencent.mobileqq.data.TroopInfo", frienduin), "troopname") + "\n" + "来自:" + getObjectField(callMethod(FriendManager, "c", group1), "name");
                                     } else if (istroop == 1) {
                                        from = "指定群不抢" + "\n" + "来自群:" + getObjectField(findResultByMethodNameAndReturnTypeAndParams(TroopManager, "a", "com.tencent.mobileqq.data.TroopInfo", group1), "troopname");
                                     } else {
                                        from = "指定人不抢" + "\n" + "来自:" + getObjectField(callMethod(FriendManager, "c", group1), "name");
                                     }
                                     toast(from);
                                     return;
                                 }
                           }
                        }
                        //红包语不抢：
                        String keywords = PreferencesUtils.keywords();
                        if (!TextUtils.isEmpty(keywords)) {
                            for (String keywords1 : keywords.split(",")) {
                                 if (password.contains(keywords1)) {
                                     toast("关键词不抢" + "\n" + "关键词:" + keywords1);
                                     return;
                                 }
                            }
                        }

                        ClassLoader walletClassLoader = (ClassLoader) callStaticMethod(findClass("com.tencent.mobileqq.pluginsdk.PluginStatic", loadPackageParam.classLoader), "getOrCreateClassLoader", globalContext, "qwallet_plugin.apk");
                        StringBuffer requestUrl = new StringBuffer();
                        requestUrl.append("&uin=" + selfuin);
                        requestUrl.append("&listid=" + redPacketId);
                        requestUrl.append("&name=" + Uri.encode((String)getObjectField(callMethod(FriendManager, "c", selfuin), "name")));
                        requestUrl.append("&answer=");
                        requestUrl.append("&groupid=" + (istroop == 0 ? selfuin : frienduin));
                        requestUrl.append("&grouptype=" + getGroupType());
                        requestUrl.append("&groupuin=" + getGroupuin(messageType));
                        requestUrl.append("&channel=" + getObjectField(mQQWalletRedPacketMsg, "redChannel"));
                        requestUrl.append("&authkey=" + authkey);
                        requestUrl.append("&agreement=0");

                        //XposedBridge.log("requestUrl：" +requestUrl );
                        Class qqplugin = findClass(VersionParam.QQPluginClass, walletClassLoader);

                        int random = Math.abs(new Random().nextInt()) % 16;
                        String reqText = (String) callStaticMethod(qqplugin, "a", globalContext, random, false, requestUrl.toString());
                        //XposedBridge.log("reqText：" +reqText );
                        StringBuffer hongbaoRequestUrl = new StringBuffer();
                        hongbaoRequestUrl.append("https://mqq.tenpay.com/cgi-bin/hongbao/qpay_hb_na_grap.cgi?ver=2.0&chv=3");
                        hongbaoRequestUrl.append("&req_text=" + reqText);
                        hongbaoRequestUrl.append("&random=" + random);
                        hongbaoRequestUrl.append("&skey_type=2");
                        hongbaoRequestUrl.append("&skey=" + callMethod(TicketManager, "getSkey", selfuin));
                        hongbaoRequestUrl.append("&msgno=" + generateNo(selfuin));
                        //XposedBridge.log("hongbaoRequestUrl：" +hongbaoRequestUrl );

                        Class<?> walletClass = findClass(VersionParam.walletPluginClass, walletClassLoader);
                        Object pickObject = newInstance(walletClass, callStaticMethod(qqplugin, "a", globalContext));
                        if (PreferencesUtils.delay()) {
                            sleep(PreferencesUtils.delayTime());
                        }

                        bundle = (Bundle) callMethod(pickObject, VersionParam.pickObject, hongbaoRequestUrl.toString());
                        JSONObject jsonobject = new JSONObject(callStaticMethod(qqplugin, "a", globalContext, random, callStaticMethod(qqplugin, "a", globalContext, bundle, new JSONObject())).toString());
                        String name = jsonobject.getJSONObject("send_object").optString("send_name");
                        XposedBridge.log("jsonobject：send_name:" +name );
                        int state = jsonobject.optInt("state");
                        if (istroop == 1) {
                            from = "来自:" + name + "\n" + "来自群:" + getObjectField(findResultByMethodNameAndReturnTypeAndParams(TroopManager, "a", "com.tencent.mobileqq.data.TroopInfo", frienduin), "troopname");
                        } else if (istroop == 5) {
                            from = "来自:" + name + "\n" + "来自热聊:" + getObjectField(findResultByMethodNameAndReturnTypeAndParams(HotChatManager, "a", "com.tencent.mobileqq.data.HotChatInfo", frienduin), "name");
                        } else if (istroop == 3000) {
                            from = "来自:" + name + "\n" + "来自讨论组:" + getObjectField(findResultByMethodNameAndReturnTypeAndParams(DiscussionManager, "a", "com.tencent.mobileqq.data.DiscussionInfo", frienduin), "discussionName");
                        } else {
                            from = "来自:" + name;
                        }
                        XposedBridge.log("from:" +from );
                        if (state == 0) {
                            double amount = ((double) jsonobject.getJSONObject("recv_object").getInt("amount")) / 100.0d;
                            
                            XposedBridge.log("amount:" +amount );
                            toast("QQ红包帮你抢到了" + amount + "元" + "\n" + from);
                            if (PreferencesUtils.reply() == GOT || PreferencesUtils.reply() == ALL && !TextUtils.isEmpty(PreferencesUtils.gotReply()) && messageType != 8) {
                                callStaticMethod(findClass("com.tencent.mobileqq.activity.ChatActivityFacade", loadPackageParam.classLoader), "a", globalQQInterface, globalContext, SessionInfo, PreferencesUtils.gotReply(), new ArrayList(), messageParam);
                            }
                            //发送抢到的红包数据：
                            sendRobbedMessage(amount);
                            //发广告：
                            if (Config.bReg==false&&RobbedLuckyMoneyCount.count%10==0&& messageType != 8) {
                            	String ad=Config.ad+"联系"+Config.contact+"下载地址："+Config.homepage;
                                callStaticMethod(findClass("com.tencent.mobileqq.activity.ChatActivityFacade", loadPackageParam.classLoader), "a", globalQQInterface, globalContext, SessionInfo, ad, new ArrayList(), messageParam);
                            }

                        } else if (state == 2) {

                            if (messageType != 8) {
                                toast("没抢到" + "\n" + from);
                                if (PreferencesUtils.reply() == MISSED || PreferencesUtils.reply() == ALL && !TextUtils.isEmpty(PreferencesUtils.missedReply()) && messageType != 8) {
                                    callStaticMethod(findClass("com.tencent.mobileqq.activity.ChatActivityFacade", loadPackageParam.classLoader), "a", globalQQInterface, globalContext, SessionInfo, PreferencesUtils.missedReply(), new ArrayList(), messageParam);
                                }
                            }

                        }

                        if (6 == messageType && PreferencesUtils.password() == SEND) {
                            callStaticMethod(findClass("com.tencent.mobileqq.activity.ChatActivityFacade", loadPackageParam.classLoader), "a", globalQQInterface, globalContext, SessionInfo, password, new ArrayList(), messageParam);
                        }
                    }
                }
        );


        findAndHookMethod("com.tencent.mobileqq.app.MessageHandlerUtils", loadPackageParam.classLoader, "a",
                "com.tencent.mobileqq.app.QQAppInterface",
                "com.tencent.mobileqq.data.MessageRecord", Boolean.TYPE, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!PreferencesUtils.open()) {
                            return;
                        }
                        int msgtype = (int) getObjectField(param.args[1], "msgtype");
                        if (msgtype == -2025) {
                            msgUid = (long) getObjectField(param.args[1], "msgUid");
                            senderuin = (String) getObjectField(param.args[1], "senderuin");
                            frienduin = getObjectField(param.args[1], "frienduin").toString();
                            istroop = (int) getObjectField(param.args[1], "istroop");
                            selfuin = getObjectField(param.args[1], "selfuin").toString();
                            XposedBridge.log("MessageHandlerUtils:msgUid:" +msgUid +";senderuin:" +senderuin+";frienduin:" +frienduin+";istroop:" +istroop+";selfuin:" +selfuin);
                        }
                    }
                }

        );


        findAndHookMethod("com.tencent.mobileqq.activity.SplashActivity", loadPackageParam.classLoader, "doOnCreate", Bundle.class, new

                XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        globalContext = (Context) param.thisObject;
                        globalQQInterface = findFirstFieldByExactType(findClass("com.tencent.mobileqq.activity.SplashActivity", loadPackageParam.classLoader), findClass("com.tencent.mobileqq.app.QQAppInterface", loadPackageParam.classLoader)).get(param.thisObject);
                        XposedBridge.log("SplashActivity:globalContext;globalQQInterface");
                    }
                }

        );


        findAndHookConstructor("mqq.app.TicketManagerImpl", loadPackageParam.classLoader, "mqq.app.AppRuntime", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                TicketManager = param.thisObject;
                XposedBridge.log("TicketManager = param.thisObject;");
            }
        });


        findAndHookConstructor("com.tencent.mobileqq.app.HotChatManager", loadPackageParam.classLoader, "com.tencent.mobileqq.app.QQAppInterface", new

                XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        HotChatManager = param.thisObject;
                        XposedBridge.log("HotChatManager = param.thisObject");
                    }
                }
        );

        findAndHookConstructor("com.tencent.mobileqq.app.TroopManager", loadPackageParam.classLoader, "com.tencent.mobileqq.app.QQAppInterface", new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                TroopManager = methodHookParam.thisObject;
                XposedBridge.log("TroopManager = methodHookParam.thisObject");
            }
        });

         findAndHookConstructor("com.tencent.mobileqq.app.DiscussionManager", loadPackageParam.classLoader, "com.tencent.mobileqq.app.QQAppInterface", new XC_MethodHook() {
             protected void afterHookedMethod(MethodHookParam methodHookParam) {
                 DiscussionManager = methodHookParam.thisObject;
                 XposedBridge.log("DiscussionManager = methodHookParam.thisObject");
             }
        });

         findAndHookConstructor("com.tencent.mobileqq.app.FriendsManager", loadPackageParam.classLoader, "com.tencent.mobileqq.app.QQAppInterface", new XC_MethodHook() {
             protected void afterHookedMethod(MethodHookParam methodHookParam) {
                 FriendManager = methodHookParam.thisObject;
                 XposedBridge.log(" FriendManager = methodHookParam.thisObject;");
             }
        });

    }


    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {

        if (loadPackageParam.packageName.equals(QQ_PACKAGE_NAME)) {
            hideModule(loadPackageParam);

            int ver = Build.VERSION.SDK_INT;
            XposedBridge.log("handleLoadPackage:Build.VERSION.SDK_INT " +ver);
            if (ver < 21) {
                findAndHookMethod("com.tencent.common.app.BaseApplicationImpl", loadPackageParam.classLoader, "onCreate", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    	 XposedBridge.log(" dohook:start");
                        dohook(loadPackageParam);
                    }
                });
            } else {
            	XposedBridge.log(" dohook2:start");
                dohook(loadPackageParam);
            }
        }


        if (loadPackageParam.packageName.equals(WECHAT_PACKAGE_NAME)) {
            findAndHookMethod("com.tencent.mm.ui.LauncherUI", loadPackageParam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Activity activity = (Activity) param.thisObject;
                    if (activity != null) {
                        Intent intent = activity.getIntent();
                        if (intent != null) {
                            String className = intent.getComponent().getClassName();
                            if (!TextUtils.isEmpty(className) && className.equals("com.tencent.mm.ui.LauncherUI") && intent.hasExtra("donate")) {
                                Intent donateIntent = new Intent();
                                donateIntent.setClassName(activity, "com.tencent.mm.plugin.remittance.ui.RemittanceUI");
                                donateIntent.putExtra("scene", 1);
                                donateIntent.putExtra("pay_scene", 32);
                                donateIntent.putExtra("fee", 10.0d);
                                donateIntent.putExtra("pay_channel", 13);
                                donateIntent.putExtra("receiver_name", "yang_xiongwei");
                                donateIntent.removeExtra("donate");
                                activity.startActivity(donateIntent);
                                activity.finish();
                            }
                        }
                    }
                }
            });
        }

    }

    private void initVersionCode(XC_LoadPackage.LoadPackageParam loadPackageParam) throws PackageManager.NameNotFoundException {
        if (0 != versionCode) {
            Context context = (Context) callMethod(callStaticMethod(findClass("android.app.ActivityThread", null), "currentActivityThread", new Object[0]), "getSystemContext", new Object[0]);
            int versionCode = context.getPackageManager().getPackageInfo(loadPackageParam.packageName, 0).versionCode;
            this.versionCode = versionCode;
            VersionParam.init(versionCode);
            XposedBridge.log("initVersionCode:versionCode " +versionCode);
        }
    }


    private int getGroupType() throws IllegalAccessException {
        int grouptype = 0;
        if (istroop == 3000) {
            grouptype = 2;

        } else if (istroop == 1) {
            Map map = (Map) findFirstFieldByExactType(HotChatManager.getClass(), Map.class).get(HotChatManager);
            if (map != null & map.containsKey(frienduin)) {
                grouptype = 5;
            } else {
                grouptype = 1;
            }
        } else if (istroop == 0) {
            grouptype = 0;
        } else if (istroop == 1004) {
            grouptype = 4;

        } else if (istroop == 1000) {
            grouptype = 3;

        } else if (istroop == 1001) {
            grouptype = 6;
        }
        return grouptype;
    }

    private String getGroupuin(int messageType) throws InvocationTargetException, IllegalAccessException {
        if (messageType != 6) {
            return senderuin;
        }
        if (istroop == 1) {
            return (String) getObjectField(findResultByMethodNameAndReturnTypeAndParams(TroopManager, "a", "com.tencent.mobileqq.data.TroopInfo", frienduin), "troopcode");
        } else if (istroop == 5) {
            return (String) getObjectField(findResultByMethodNameAndReturnTypeAndParams(HotChatManager, "a", "com.tencent.mobileqq.data.HotChatInfo", frienduin), "troopCode");
        }
        return senderuin;
    }

    private String generateNo(String selfuin) {
        StringBuilder stringBuilder = new StringBuilder(selfuin);
        stringBuilder.append(new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
        String count = valueOf(n++);
        int length = (28 - stringBuilder.length()) - count.length();
        for (int i = 0; i < length; i++) {
            stringBuilder.append("0");
        }
        stringBuilder.append(count);
        return stringBuilder.toString();
    }

    private void toast(final String content) {
        if (PreferencesUtils.amount()) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(globalContext, content, LENGTH_LONG).show();
                    SpeechUtil.getSpeechUtil(globalContext).speak(content);
                }
            });

        }
    }
    /*
    private void UpdateRobbedHBMessage(double je){
        RobbedLuckyMoneyCount.count=RobbedLuckyMoneyCount.count+1;
        RobbedLuckyMoneyCount.je=RobbedLuckyMoneyCount.je+je;
        XposedBridge.log("RobbedLuckyMoneyCount.count:" + RobbedLuckyMoneyCount.count);
        XposedBridge.log("RobbedLuckyMoneyCount.je:" +RobbedLuckyMoneyCount.je);
    }
  */
    private void sendRobbedMessage(double money){
        RobbedLuckyMoneyCount.count=RobbedLuckyMoneyCount.count+1;
        RobbedLuckyMoneyCount.money=RobbedLuckyMoneyCount.money+money;

        XposedBridge.log("RobbedLuckyMoneyCount.count:" + RobbedLuckyMoneyCount.count);
        XposedBridge.log("RobbedLuckyMoneyCount.money:" +RobbedLuckyMoneyCount.money);
       
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    //发送广播，
                    Intent intent = new Intent(Config.ACTION_ROBBED_LUCKY_MONEY);
                    intent.putExtra("count" , RobbedLuckyMoneyCount.count);
                    intent.putExtra("money" , RobbedLuckyMoneyCount.money);
                    globalContext.sendBroadcast(intent);
                    XposedBridge.log("globalContext.sendBroadcast(intent)");
                }
            });  
    }
   
}
