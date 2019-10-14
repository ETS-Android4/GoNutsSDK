package com.nutsplay.nopagesdk.manager;

import android.app.Activity;
import android.app.Application;
import android.content.Context;

import com.appsflyer.AFInAppEventParameterName;
import com.appsflyer.AFInAppEventType;
import com.appsflyer.AppsFlyerConversionListener;
import com.appsflyer.AppsFlyerLib;
import com.dataeye.tracking.sdk.trackingAPI.DCTrackingAgent;
import com.dataeye.tracking.sdk.trackingAPI.DCTrackingPoint;
import com.nutsplay.nopagesdk.kernel.SDKLangConfig;
import com.nutsplay.nopagesdk.kernel.SDKManager;
import com.nutsplay.nopagesdk.utils.toast.SDKToast;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by frank-ma on 2019/6/29 12:51 PM
 * Email: frankma9103@gmail.com
 * Desc: 追踪管理类，appsflyer和DE都写在这里面
 */
public class TrackingManager {

    /**
     * 事件追踪初始化
     *
     * @param application
     */
    public static void trackingInit(Activity activity,String DEId, String afKey, Application application){
        if (afKey == null || afKey.isEmpty()){
            afKey = "VBmCBKvNg5uvd4iiLZSx7J";
        }
        //AF初始化
        AppsFlyerConversionListener conversionListener = new AppsFlyerConversionListener() {
            @Override
            public void onInstallConversionDataLoaded(Map<String, String> map) {

            }

            @Override
            public void onInstallConversionFailure(String s) {

            }

            @Override
            public void onAppOpenAttribution(Map<String, String> map) {

            }

            @Override
            public void onAttributionFailure(String s) {

            }
        };

        AppsFlyerLib.getInstance().init(afKey,conversionListener,application);
        AppsFlyerLib.getInstance().startTracking(application,afKey);


        //配置DataEye
        DCTrackingAgent.initWithAppIdAndChannelId(activity, DEId, "default");
    }



    /**
     * 公共的事件追踪方法
     * @param context
     * @param eventType
     * @param eventValue
     */
    public static void EventTracking(Context context, String eventType, Map<String, Object> eventValue) {
        try {
            if (context!=null && eventType!=null && eventValue!=null){
                AppsFlyerLib.getInstance().trackEvent(context.getApplicationContext(), eventType, eventValue);
                //DE注意：自定义效果点必须先在平台上创建,每个APP只支持15个自定义效果点。创建方法：登录广告效果监测平台,进入对应的APP,在菜单 投放管理->效果点管理 中创建。
                DCTrackingPoint.setEffectPoint(eventType,eventValue);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void onResume(Context context){
        //DE用来计算在线时长
        DCTrackingAgent.resume(context);
    }

    public static void onPause(Context context){
        //DE用来计算在线时长
        DCTrackingAgent.pause(context);
    }

    /**
     * 注册账号追踪
     * @param accountId
     */
    public static void registerTracking(String accountId){
        SDKToast.getInstance().ToastShow(SDKLangConfig.getInstance().findMessage("registerok"),1);

        DCTrackingPoint.createAccount(accountId);
//        GooglePayHelp.getInstance().queryPurchase(false);
        GooglePayHelp.getInstance().resentOrderByDbRecord();
    }

    /**
     * 登录追踪
     * @param accountId
     */
    public static void loginTracking(String accountId){

        SDKManager.getInstance().setAuto(true);
        SDKToast.getInstance().ToastShow(SDKLangConfig.getInstance().findMessage("loginok"),1);

        DCTrackingPoint.login(accountId);
//        GooglePayHelp.getInstance().queryPurchase(false);
        GooglePayHelp.getInstance().resentOrderByDbRecord();
    }

    /**
     * 创建角色追踪
     * @param context
     * @param serverId
     * @param roleId
     * @param roleName
     */
    public static void createRoleTracking(Context context,String serverId,String roleId,String roleName){
        try {
            Map<String, Object> eventValue = new HashMap<>();
            eventValue.put("Game_ServiceId", serverId);
            eventValue.put("Game_RoleId", roleId);
            eventValue.put("Game_RoleName", roleName);

            AppsFlyerLib.getInstance().trackEvent(context, "create_role", eventValue);
            //DE注意：自定义效果点必须先在平台上创建,每个APP只支持15个自定义效果点。创建方法：登录广告效果监测平台,进入对应的APP,在菜单 投放管理->效果点管理 中创建。
            DCTrackingPoint.setEffectPoint("create_role",eventValue);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 支付追踪
     * @param accountId
     * @param orderId
     * @param currencyAmount
     * @param currencyType
     * @param payType
     */
    public static void purchaseTracking(Context context,String accountId, String orderId, double currencyAmount, String currencyType, String payType){

        //AF追踪
        Map<String, Object> eventValues = new HashMap<>();
        eventValues.put(AFInAppEventParameterName.REVENUE, currencyAmount);
        eventValues.put(AFInAppEventParameterName.CURRENCY, currencyType);
        eventValues.put(AFInAppEventParameterName.CONTENT_ID, orderId);
        eventValues.put(AFInAppEventParameterName.CONTENT_TYPE, payType);
        TrackingManager.EventTracking(context, AFInAppEventType.PURCHASE, eventValues);
        //dataEye追踪
        DCTrackingPoint.paymentSuccess(SDKManager.getInstance().getUser().getUserId(), orderId, currencyAmount, currencyType, payType);
    }
}
