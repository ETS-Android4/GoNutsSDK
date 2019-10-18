package com.nutsplay.nopagesdk.kernel;

import android.app.Activity;

import com.nutsplay.nopagesdk.beans.InitParameter;
import com.nutsplay.nopagesdk.callback.InitCallBack;
import com.nutsplay.nopagesdk.callback.LogOutCallBack;
import com.nutsplay.nopagesdk.callback.LoginCallBack;
import com.nutsplay.nopagesdk.callback.PurchaseCallBack;
import com.nutsplay.nopagesdk.callback.ResultCallBack;
import com.nutsplay.nopagesdk.callback.SDKGetSkuDetailsCallback;

import java.util.List;

/**
 * Created by frank-ma on 2019-09-19 10:05
 * Email: frankma9103@gmail.com
 * Desc:
 */
public class SDK {

    private volatile static SDK INSTANCE;

    public static SDK getInstance() {
        if (INSTANCE == null) {
            synchronized (SDK.class) {
                if (null == INSTANCE) {
                    INSTANCE = new SDK();
                }
            }
        }
        return INSTANCE;
    }


    //********************************SDK接口******************************************

    /**
     * 初始化接口
     *
     * @param activity
     * @param initParameter
     * @param initCallBack
     */
    public void initSDK(Activity activity, InitParameter initParameter, InitCallBack initCallBack) {
        SDKManager.getInstance().initSDK(activity, initParameter, initCallBack);
    }

    /**
     * 无UI
     *
     * @param activity
     * @param userName
     * @param pwd
     * @param loginCallBack
     */
    public void sdkRegister(final Activity activity, final String userName, final String pwd, final LoginCallBack loginCallBack) {

        SDKManager.getInstance().sdkRegister(activity,userName,pwd,loginCallBack);
    }

    /**
     * 登录 UI
     * @param activity
     * @param loginCallBack
     */
    public void sdkLogin(Activity activity,LoginCallBack loginCallBack){
        SDKManager.getInstance().sdkLogin(activity,loginCallBack);
    }

    /**
     * SDK登陆接口
     *
     * @param activity
     * @param loginCallBack
     */
    public void sdkLoginNoUI(final Activity activity, final String userName, final String pwd, final LoginCallBack loginCallBack) {

        SDKManager.getInstance().sdkLoginNoUI(activity,userName,pwd,loginCallBack);
    }

    /**
     * 切换账号 UI
     *
     * @param activity
     * @param loginCallBack
     */
    public void sdkSwitchAccount(Activity activity,LoginCallBack loginCallBack){
        SDKManager.getInstance().sdkSwitchAccount(activity,loginCallBack);
    }

    /**
     * 切换账号 无UI
     *
     * @param activity
     * @param loginCallBack
     */
    public void sdkSwitchAccountNoUI(Activity activity,String userName,String pwd,LoginCallBack loginCallBack){
        SDKManager.getInstance().sdkSwitchAccountNoUI(activity,userName,pwd,loginCallBack);
    }

    /**
     * 游客登录
     *
     * @param activity
     * @param loginCallBack
     */
    public void sdkLoginWithVisitor(Activity activity, LoginCallBack loginCallBack){
        SDKManager.getInstance().sdkLoginWithVisitor(activity,loginCallBack);
    }

    /**
     * SDK三方账户登录接口
     *
     * @param activity
     * @param loginCallBack
     */
    public void sdkLoginThirdAccount(Activity activity, String oauthId, String oauthSource, final LoginCallBack loginCallBack) {

        SDKManager.getInstance().sdkLoginThirdAccount(activity,oauthId,oauthSource,loginCallBack);
    }

    /**
     * 注销账号
     *
     * @param activity
     * @param logOutCallBack
     */
    public void sdkLogout(Activity activity, LogOutCallBack logOutCallBack) {

        SDKManager.getInstance().sdkLogout(activity,logOutCallBack);
    }


    /**
     * SDK下单接口
     *
     * @param activity
     * @param purchaseCallBack
     */
    public void sdkPurchase(final Activity activity, String serverId, final String referenceId, String gameExt, final PurchaseCallBack purchaseCallBack) {

        SDKManager.getInstance().sdkPurchase(activity, serverId, referenceId, gameExt, purchaseCallBack);
    }

    /**
     * 查询内购商品的本地价格
     *
     * @param activity
     * @param skuList
     * @param callback
     */
    public void sdkQuerySkuLocalPrice(Activity activity, final List<String> skuList, final SDKGetSkuDetailsCallback callback) {

        SDKManager.getInstance().sdkQuerySkuLocalPrice(activity, skuList, callback);

    }

    /**
     * 创角色追踪
     *
     * @param activity
     * @param serverId
     * @param roleId
     * @param roleName
     */
    public void sdkCreateRoleTracking(Activity activity, String serverId, String roleId, String roleName) {

        SDKManager.getInstance().sdkCreateRoleTracking(activity, serverId, roleId, roleName);
    }

    /**
     * 切换SDK语言
     *
     * @param language
     */
    public void sdkUpdateLanguage(String language) {

        SDKManager.getInstance().sdkUpdateLanguage(language);
    }

    /**
     * 修改密码
     *
     * @param activity
     * @param account
     * @param oldPwd
     * @param newPwd
     * @param callback
     */
    public void sdkResetPwd(Activity activity, String account, String oldPwd, String newPwd, final LoginCallBack callback){
        SDKManager.getInstance().sdkResetPwd(activity,account,oldPwd,newPwd,callback);
    }

    /**
     * 第三方账号绑定账号密码
     *
     * @param activity
     * @param oauthid     第三方账号ID
     * @param oauthsource 第三方账号来源
     * @param account     账号
     * @param pwd      密码
     * @param callback
     */
    public void sdkBindAccount(Activity activity, String oauthid, String oauthsource, String account, String pwd, final ResultCallBack callback) {

        SDKManager.getInstance().sdkBindAccount(activity,oauthid,oauthsource,account,pwd,callback);

    }

    /**
     * 在activity的onResume()方法中调用
     */
    public void sdkOnResume(Activity activity) {

        SDKManager.getInstance().sdkOnResume(activity);
    }

    /**
     * 在activity的onDestroy()方法中调用
     */
    public void sdkOnDestroy(Activity activity) {

        SDKManager.getInstance().sdkOnDestroy();
    }

}
