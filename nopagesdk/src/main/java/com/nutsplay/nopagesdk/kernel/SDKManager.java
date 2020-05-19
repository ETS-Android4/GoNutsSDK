package com.nutsplay.nopagesdk.kernel;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.nutsplay.nopagesdk.beans.InitParameter;
import com.nutsplay.nopagesdk.beans.SDKInitModel;
import com.nutsplay.nopagesdk.beans.SDKLoginModel;
import com.nutsplay.nopagesdk.beans.SDKOrderModel;
import com.nutsplay.nopagesdk.beans.SDKResult;
import com.nutsplay.nopagesdk.beans.User;
import com.nutsplay.nopagesdk.beans.VerifyCodeResult;
import com.nutsplay.nopagesdk.callback.InitCallBack;
import com.nutsplay.nopagesdk.callback.LogOutCallBack;
import com.nutsplay.nopagesdk.callback.LoginCallBack;
import com.nutsplay.nopagesdk.callback.NetCallBack;
import com.nutsplay.nopagesdk.callback.PurchaseCallBack;
import com.nutsplay.nopagesdk.callback.ResultCallBack;
import com.nutsplay.nopagesdk.callback.SDKGetSkuDetailsCallback;
import com.nutsplay.nopagesdk.callback.ThirdLoginResultCallBack;
import com.nutsplay.nopagesdk.db.DBManager;
import com.nutsplay.nopagesdk.db.PurchaseRecord;
import com.nutsplay.nopagesdk.manager.ApiManager;
import com.nutsplay.nopagesdk.manager.AppManager;
import com.nutsplay.nopagesdk.manager.GooglePayHelp;
import com.nutsplay.nopagesdk.manager.LoginManager;
import com.nutsplay.nopagesdk.manager.TrackingManager;
import com.nutsplay.nopagesdk.network.GsonUtils;
import com.nutsplay.nopagesdk.network.NetUtils;
import com.nutsplay.nopagesdk.ui.BindTipDialog;
import com.nutsplay.nopagesdk.ui.FBLoginActivity;
import com.nutsplay.nopagesdk.ui.FirstDialog;
import com.nutsplay.nopagesdk.ui.PayWebActivity;
import com.nutsplay.nopagesdk.ui.ScreenShotActivity;
import com.nutsplay.nopagesdk.ui.UserCenterDialog;
import com.nutsplay.nopagesdk.utils.DeviceUtils;
import com.nutsplay.nopagesdk.utils.Installations;
import com.nutsplay.nopagesdk.utils.SDKGameUtils;
import com.nutsplay.nopagesdk.utils.encryption.AESUtils;
import com.nutsplay.nopagesdk.utils.encryption.RSAUtils;
import com.nutsplay.nopagesdk.utils.sputil.SPKey;
import com.nutsplay.nopagesdk.utils.sputil.SPManager;
import com.nutsplay.nopagesdk.utils.toast.SDKToast;
import com.nutsplay.nopagesdk.view.SDKProgressDialog;
import com.nutspower.commonlibrary.utils.LogUtils;
import com.nutspower.commonlibrary.utils.StringUtils;
import com.tencent.bugly.crashreport.CrashReport;

import java.util.List;

/**
 * Created by frank-ma on 2019-09-19 10:05
 * Email: frankma9103@gmail.com
 * Desc:
 */
public class SDKManager {

    private static String TAG = "SDKManager";

    private volatile static SDKManager INSTANCE;

    private InitParameter initParameter;

    private Activity activity;

    private PurchaseCallBack purchaseCallBack;

    private SDKProgressDialog progressDialog;

    private boolean initStatus = false;

    public static SDKManager getInstance() {
        if (INSTANCE == null) {
            synchronized (SDKManager.class) {
                if (null == INSTANCE) {
                    INSTANCE = new SDKManager();
                }
            }
        }
        return INSTANCE;
    }

    public InitParameter getInitParameter() {
        return initParameter;
    }

    public void setInitParameter(InitParameter initParameter) {
        this.initParameter = initParameter;
    }

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public PurchaseCallBack getPurchaseCallBack() {
        return purchaseCallBack;
    }

    public void setPurchaseCallBack(PurchaseCallBack purchaseCallBack) {
        this.purchaseCallBack = purchaseCallBack;
    }

    public void setAuto(boolean auto) {
        SPManager.getInstance(getActivity()).putBoolean(SPKey.key_sdk_auto, auto);
    }

    public boolean isAuto() {
        return SPManager.getInstance(getActivity()).getBoolean(SPKey.key_sdk_auto);
    }

    public void setGuestLoginCount(int count) {
        SPManager.getInstance(getActivity()).putInt(SPKey.key_sdk_auto_guest_count, count);
    }

    public int getGuestLoginCount() {
        return SPManager.getInstance(getActivity()).getInt(SPKey.key_sdk_auto_guest_count, 0);
    }

    public User getUser() {
        return (User) SPManager.getInstance(getActivity()).getBean(SPKey.key_bean_data_user);
    }

    public void setUser(User token) {
        SPManager.getInstance(getActivity()).putBean(SPKey.key_bean_data_user, token);
    }

    public boolean isInitStatus() {
        return initStatus;
    }

    public void setInitStatus(boolean initStatus) {
        this.initStatus = initStatus;
    }

    public void showProgress(Activity activity) {
        if (null == progressDialog)
            if (activity.hasWindowFocus()) {
                progressDialog = SDKProgressDialog.createProgrssDialog(activity,"Loading...");
            }
        if (null != progressDialog) {
            if (activity.hasWindowFocus()) {
                progressDialog.show();
                progressDialog.setCancelable(false);
            }

        }
    }

    public void hideProgress() {
        if (null != progressDialog) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    //*******************************************SDK接口*********************************************

    /**
     * 初始化接口
     *
     * @param activity
     * @param initParameter
     * @param initCallBack
     */
    public void initSDK(Activity activity, InitParameter initParameter, InitCallBack initCallBack) {

        if (activity == null) {
            System.out.println("initSDK failed:Activity is null.");
            return;
        }
        setActivity(activity);

        if (initParameter == null) {
            System.out.println("initSDK failed:InitParameter is null.");
            return;
        }
        setInitParameter(initParameter);

        if (initCallBack == null) {
            System.out.println("initSDK failed:InitCallBack is null.");
            return;
        }

        //配置debug模式
        LogUtils.setIsDeBug(initParameter.isDebug());

        //向用户申请读取手机信息的权限
        DeviceUtils.checkPermission(activity);

        //初始化bugly
        CrashReport.initCrashReport(activity.getApplicationContext(), initParameter.getBuglyId(), false);
        if (initParameter.getBuglyChannel() != null && !initParameter.getBuglyChannel().isEmpty()) {
            CrashReport.setAppChannel(activity.getApplicationContext(), initParameter.getBuglyChannel());
        }

        //初始化追踪
        TrackingManager.trackingInit(activity,  initParameter.getAppsflyerId(), activity.getApplication());

        showProgress(activity);

        //获取公钥
        getPublicKey(activity, initCallBack);

        //获取keyHash
        SDKGameUtils.getKeyHash(activity);
    }

    /**
     * 获取公钥
     *
     * @param activity
     * @param initCallBack
     */
    private void getPublicKey(final Activity activity, final InitCallBack initCallBack) {

        ApiManager.getInstance().getRASPublicKey(new NetCallBack() {
            @Override
            public void onSuccess(String result) {

                SDKResult sdkResult = (SDKResult) GsonUtils.json2Bean(result, SDKResult.class);
                if (sdkResult == null) {
                    initCallBack.onFailure("sdkResult is null：json解析格式错误");
                    return;
                }
                if (sdkResult.getCode() == 1) {
                    String publickey = NetUtils.decode(sdkResult.getData());
                    publickey = publickey.replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "");
                    SPManager.getInstance(activity).putString(SPKey.PUBLIC_KEY, publickey);

                    //初始化接口go
                    doSdkInit(activity, initCallBack);
                } else {
                    SDKGameUtils.showServiceInfo(sdkResult.getCode(), sdkResult.getMessage());
                    initCallBack.onFailure(sdkResult.getMessage());
                }
            }

            @Override
            public void onFailure(String errorMsg) {
                hideProgress();
                LogUtils.e("getRASPublicKey", "onFailure----" + errorMsg);
                initCallBack.onFailure(errorMsg);
            }
        });
    }

    /**
     * 初始化接口go语言
     */
    private void doSdkInit(final Activity activity, final InitCallBack initCallBackListener) {

        try {
            final String aesKey = AESUtils.generate16SecretKey();
            String publicKey = SPManager.getInstance(activity).getString(SPKey.PUBLIC_KEY);
            String aesKey16byRSA = RSAUtils.encryptData(aesKey.getBytes(), RSAUtils.loadPublicKey(publicKey));

            ApiManager.getInstance().SDKInitGo(aesKey, aesKey16byRSA, new NetCallBack() {

                @Override
                public void onSuccess(String result) {
                    hideProgress();
                    if (result == null || result.isEmpty()) {
                        initCallBackListener.onFailure("server response is empty.");
                        return;
                    }
                    try {
                        String decodeData = AESUtils.decrypt(result, aesKey);
                        LogUtils.d(TAG, "SDKInitGo---" + decodeData);
                        SDKInitModel initgoBean = (SDKInitModel) GsonUtils.json2Bean(decodeData, SDKInitModel.class);
                        if (initgoBean == null) {
                            initCallBackListener.onFailure("InitGoBean is null.");
                            return;
                        }
                        LogUtils.d(TAG, "initCode:" + initgoBean.getCode());
                        if (initgoBean.getCode() == 1) {
                            LogUtils.d(TAG, "SDKInitGo成功 " + initgoBean.getMessage());
                            setInitData(initgoBean);
                            initCallBackListener.onSuccess();
                        } else if (initgoBean.getCode() == -6) {
                            //STATUS_TICKET_INVALID,可能封号或修改密码或另一台手机登录或绑定账号成功，ticket重新生成了
                            LogUtils.d(TAG, "code:" + initgoBean.getCode() + "  msg:" + initgoBean.getMessage());
                            handleLogout();
                            initCallBackListener.onFailure(initgoBean.getMessage());

                        } else {
                            LogUtils.d(TAG, "code:" + initgoBean.getCode() + "  msg:" + initgoBean.getMessage());
                            SDKGameUtils.showServiceInfo(initgoBean.getCode(), initgoBean.getMessage());
                            initCallBackListener.onFailure(initgoBean.getMessage());
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(String errorMsg) {
                    hideProgress();
                    LogUtils.e(TAG, "SDKInitGo---onFailure:" + errorMsg);
                    sdkUploadLog(activity,"init interface error",errorMsg);
                    initCallBackListener.onFailure(errorMsg);

                }
            });
        } catch (Exception e) {
            hideProgress();
            e.printStackTrace();
        }

    }

    private void setInitData(SDKInitModel initGoBean) {

        if (initGoBean == null) return;
        setInitStatus(true);
        SPManager.getInstance(getActivity()).putBean(SPKey.key_bean_data_init, initGoBean);
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

        try {

            if (activity == null) {
                System.out.println("sdkRegister failed:Activity is null.");
                return;
            }
            setActivity(activity);

            if (loginCallBack == null) {
                System.out.println("sdkRegister failed:registerCallBack is null.");
                return;
            }

            //账号检查
            if (!SDKGameUtils.matchAccount(userName) || !SDKGameUtils.matchPw(pwd)) {
                return;
            }

            if (!isInitStatus()) {
                SDKToast.getInstance().ToastShow("The SDK is not initialized.", 3);
                loginCallBack.onFailure("The SDK is not initialized.");
                return;
            }

            final String aesKey = AESUtils.generate16SecretKey();
            String publicKey = SPManager.getInstance(activity).getString(SPKey.PUBLIC_KEY);
            String aesKey16byRSA = RSAUtils.encryptData(aesKey.getBytes(), RSAUtils.loadPublicKey(publicKey));

            showProgress(activity);
            ApiManager.getInstance().SDKRegisterAccount(aesKey, aesKey16byRSA, userName, pwd, new NetCallBack() {
                @Override
                public void onSuccess(String result) {
                    hideProgress();

                    LogUtils.e(TAG, "SDKRegisterAccount---onSuccess:" + result);
                    if (result == null || result.isEmpty()) {
                        loginCallBack.onFailure("result is null.");
                        return;
                    }
                    try {
                        String decodeData = AESUtils.decrypt(result, aesKey);
                        LogUtils.e(TAG, "SDKRegisterAccount---onSuccess:" + decodeData);
                        SDKLoginModel loginModel = (SDKLoginModel) GsonUtils.json2Bean(decodeData, SDKLoginModel.class);
                        if (loginModel == null) {
                            loginCallBack.onFailure("loginModel is null.");
                            return;
                        }
                        if (loginModel.getCode() == 1) {
                            User user = new User();
                            user.setUserId(loginModel.getData().getPassportId());
                            user.setTicket(loginModel.getData().getTicket());
                            user.setSdkmemberType(SDKConstant.TYPE_ACCOUNT);
                            loginCallBack.onSuccess(user);
                            //记住账号密码
                            SPManager.getInstance(activity).putString(SPKey.key_user_name_last_login, userName);
                            SPManager.getInstance(activity).putString(SPKey.key_pwd_last_login, pwd);

                            //注册追踪
                            TrackingManager.registerTracking(loginModel.getData().getPassportId());
                        } else {
                            LogUtils.e(TAG, "SDKRegisterAccount---onSuccess:" + loginModel.getMessage());
                            SDKGameUtils.showServiceInfo(loginModel.getCode(), loginModel.getMessage());
                            loginCallBack.onFailure(loginModel.getMessage());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(String errorMsg) {
                    hideProgress();
                    LogUtils.e(TAG, "SDKRegisterAccount---onFailure:" + errorMsg);
                    sdkUploadLog(activity,"SDKRegisterAccount interface error",errorMsg);
                    loginCallBack.onFailure(errorMsg);
                }
            });

        } catch (Exception e) {
            hideProgress();
            e.printStackTrace();
        }
    }

    /**
     * 无UI
     *
     * @param activity
     * @param userName
     * @param pwd
     * @param loginCallBack
     */
    public void sdkRegister2Dialog(final Activity activity, final String userName, final String pwd, final LoginCallBack loginCallBack, final ResultCallBack callBack) {

        try {

            if (activity == null) {
                System.out.println("sdkRegister failed:Activity is null.");
                return;
            }
            setActivity(activity);

            if (loginCallBack == null) {
                System.out.println("sdkRegister failed:registerCallBack is null.");
                return;
            }

            //账号检查
            if (!SDKGameUtils.matchAccount(userName) || !SDKGameUtils.matchPw(pwd)) {
                return;
            }

            if (!isInitStatus()) {
                SDKToast.getInstance().ToastShow("The SDK is not initialized.", 3);
                loginCallBack.onFailure("The SDK is not initialized.");
                return;
            }

            final String aesKey = AESUtils.generate16SecretKey();
            String publicKey = SPManager.getInstance(activity).getString(SPKey.PUBLIC_KEY);
            String aesKey16byRSA = RSAUtils.encryptData(aesKey.getBytes(), RSAUtils.loadPublicKey(publicKey));

            showProgress(activity);
            ApiManager.getInstance().SDKRegisterAccount(aesKey, aesKey16byRSA, userName, pwd, new NetCallBack() {
                @Override
                public void onSuccess(String result) {
                    hideProgress();

                    LogUtils.e(TAG, "SDKRegisterAccount---onSuccess:" + result);
                    if (result == null || result.isEmpty()) {
                        loginCallBack.onFailure("result is null.");
                        return;
                    }
                    try {
                        String decodeData = AESUtils.decrypt(result, aesKey);
                        LogUtils.e(TAG, "SDKRegisterAccount---onSuccess:" + decodeData);
                        SDKLoginModel loginModel = (SDKLoginModel) GsonUtils.json2Bean(decodeData, SDKLoginModel.class);
                        if (loginModel == null) {
                            loginCallBack.onFailure("loginModel is null.");
                            return;
                        }
                        if (loginModel.getCode() == 1) {
                            User user = new User();
                            user.setUserId(loginModel.getData().getPassportId());
                            user.setTicket(loginModel.getData().getTicket());
                            user.setSdkmemberType(SDKConstant.TYPE_ACCOUNT);
                            loginCallBack.onSuccess(user);
                            callBack.onSuccess();
                            //记住账号密码
                            SPManager.getInstance(activity).putString(SPKey.key_user_name_last_login, userName);
                            SPManager.getInstance(activity).putString(SPKey.key_pwd_last_login, pwd);

                            //注册追踪
                            TrackingManager.registerTracking(loginModel.getData().getPassportId());
                        } else {
                            LogUtils.e(TAG, "SDKRegisterAccount---onSuccess:" + loginModel.getMessage());
                            SDKGameUtils.showServiceInfo(loginModel.getCode(), loginModel.getMessage());
                            loginCallBack.onFailure(loginModel.getMessage());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(String errorMsg) {
                    hideProgress();
                    LogUtils.e(TAG, "SDKRegisterAccount---onFailure:" + errorMsg);
                    loginCallBack.onFailure(errorMsg);
                }
            });

        } catch (Exception e) {
            hideProgress();
            e.printStackTrace();
        }
    }

    /**
     * SDK登陆接口,有UI的
     *
     * @param activity
     * @param loginCallBack
     */
    public void sdkLogin(final Activity activity, final LoginCallBack loginCallBack) {

        try {
            if (activity == null) {
                System.out.println("sdkLogin failed:Activity is null.");
                return;
            }
            setActivity(activity);

            if (loginCallBack == null) {
                System.out.println("sdkLogin failed:loginCallBack is null.");
                return;
            }

            if (!isInitStatus()) {
                SDKToast.getInstance().ToastShow("The SDK is not initialized.", 3);
                loginCallBack.onFailure("The SDK is not initialized.");
                return;
            }

            //自动登录
            if (isAuto()) {
                if (getUser() != null && StringUtils.isNotBlank(getUser().getTicket())) {
                    if (getUser().getSdkmemberType().equals(SDKConstant.TYPE_GUEST)) {


                        if (SDKManager.getInstance().getGuestLoginCount() >= 5) {

                            BindTipDialog.Builder builder = new BindTipDialog.Builder(activity, loginCallBack);
                            builder.create().show();
                            return;
                        } else {
                            SDKManager.getInstance().setGuestLoginCount(SDKManager.getInstance().getGuestLoginCount() + 1);
                        }

                    }
                    showProgress(activity);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            hideProgress();
                            loginCallBack.onSuccess(getUser());
                            //登录追踪
                            TrackingManager.loginTracking(getUser().getUserId());


                        }
                    }, 1000);
                    return;

                }
            }
            FirstDialog.Builder builder = new FirstDialog.Builder(activity, loginCallBack);
            builder.create().show();


        } catch (Exception e) {
            hideProgress();
            e.printStackTrace();
        }
    }

    /**
     *
     * 默认游客登录，自动初始化
     *
     * @param activity
     * @param loginCallBack
     */
    public void sdkDefaultLogin(final Activity activity, InitParameter initParameter, final LoginCallBack loginCallBack){

        try{

            if (loginCallBack == null) {
                System.out.println("sdkDefaultLogin failed:loginCallBack is null.");
                return;
            }
            if (activity == null) {
                loginCallBack.onFailure("initSDK failed:Activity is null.");
                return;
            }
            initSDK(activity, initParameter, new InitCallBack() {
                @Override
                public void onSuccess() {
                    LoginManager.getInstance().visitorLogin(activity, loginCallBack);
                }

                @Override
                public void onFailure(String msg) {
                    loginCallBack.onFailure(msg);
                }
            });

        }catch (Exception e){
            hideProgress();
            e.printStackTrace();
        }
    }

    /**
     * SDK登陆接口 无UI
     *
     * @param activity
     * @param loginCallBack
     */
    public void sdkLoginNoUI(final Activity activity, final String userName, final String pwd, final LoginCallBack loginCallBack) {

        try {
            if (activity == null) {
                System.out.println("sdkLogin failed:Activity is null.");
                return;
            }
            setActivity(activity);

            if (loginCallBack == null) {
                System.out.println("sdkLogin failed:loginCallBack is null.");
                return;
            }

            if (StringUtils.isBlank(userName) || StringUtils.isBlank(pwd)) {
                SDKToast.getInstance().ToastShow("UserName or password can't be empty.", 3);
                loginCallBack.onFailure("userName or pwd is null.");
                return;
            }

            //账号检查
            if (!SDKGameUtils.matchAccount(userName) || !SDKGameUtils.matchPw(pwd)) {
                return;
            }

            if (!isInitStatus()) {
                SDKToast.getInstance().ToastShow("The SDK is not initialized.", 3);
                loginCallBack.onFailure("The SDK is not initialized.");
                return;
            }

            //自动登录
            if (isAuto()) {
                if (getUser() != null) {
                    showProgress(activity);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            hideProgress();
                            loginCallBack.onSuccess(getUser());
                            //登录追踪
                            TrackingManager.loginTracking(getUser().getUserId());
                        }
                    }, 1000);
                    return;
                }
            }

            final String aesKey = AESUtils.generate16SecretKey();
            String publicKey = SPManager.getInstance(activity).getString(SPKey.PUBLIC_KEY);
            String aesKey16byRSA = RSAUtils.encryptData(aesKey.getBytes(), RSAUtils.loadPublicKey(publicKey));

            showProgress(activity);
            ApiManager.getInstance().SDKLoginGo(aesKey, aesKey16byRSA, userName, pwd, new NetCallBack() {
                @Override
                public void onSuccess(String result) {
                    hideProgress();

                    LogUtils.e(TAG, "SDKLoginGo---onSuccess:" + result);
                    if (result == null || result.isEmpty()) {
                        loginCallBack.onFailure("result is null.");
                        return;
                    }
                    try {
                        String decodeData = AESUtils.decrypt(result, aesKey);
                        LogUtils.e(TAG, "SDKLoginGo---onSuccess:" + decodeData);
                        SDKLoginModel loginModel = (SDKLoginModel) GsonUtils.json2Bean(decodeData, SDKLoginModel.class);
                        if (loginModel == null) {
                            loginCallBack.onFailure("loginModel is null.");
                            return;
                        }
                        if (loginModel.getCode() == 1) {
                            User user = new User();
                            user.setUserId(loginModel.getData().getPassportId());
                            user.setTicket(loginModel.getData().getTicket());
                            user.setSdkmemberType(SDKConstant.TYPE_ACCOUNT);
                            setUser(user);
                            loginCallBack.onSuccess(user);

                            //记住账号密码
                            SPManager.getInstance(activity).putString(SPKey.key_user_name_last_login, userName);
                            SPManager.getInstance(activity).putString(SPKey.key_pwd_last_login, pwd);

                            //登录追踪
                            TrackingManager.loginTracking(loginModel.getData().getPassportId());

                        } else {
                            LogUtils.e(TAG, "SDKLoginGo---onSuccess:" + loginModel.getMessage());
                            SDKGameUtils.showServiceInfo(loginModel.getCode(), loginModel.getMessage());
                            loginCallBack.onFailure(loginModel.getMessage());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(String errorMsg) {
                    hideProgress();
                    LogUtils.e(TAG, "SDKLoginGo---onFailure:" + errorMsg);
                    loginCallBack.onFailure(errorMsg);
                }
            });

        } catch (Exception e) {
            hideProgress();
            e.printStackTrace();
        }
    }

    /**
     * 切换账号 UI
     *
     * @param activity
     * @param loginCallBack
     */
    public void sdkSwitchAccount(Activity activity,LoginCallBack loginCallBack){

        if (activity == null) {
            System.out.println("sdkSwitchAccount failed:Activity is null.");
            return;
        }
        setActivity(activity);

        if (loginCallBack == null) {
            System.out.println("sdkSwitchAccount failed:loginCallBack is null.");
            return;
        }

        if (!isInitStatus()) {
            SDKToast.getInstance().ToastShow("The SDK is not initialized.", 3);
            loginCallBack.onFailure("The SDK is not initialized.");
            return;
        }

        //登出操作
        handleLogout();

        //登录操作
        sdkLogin(activity,loginCallBack);
    }

    /**
     * 切换账号 无UI
     *
     * @param activity
     * @param loginCallBack
     */
    public void sdkSwitchAccountNoUI(Activity activity,String userName,String pwd,LoginCallBack loginCallBack){

        if (activity == null) {
            System.out.println("sdkSwitchAccount failed:Activity is null.");
            return;
        }
        setActivity(activity);

        if (loginCallBack == null) {
            System.out.println("sdkSwitchAccount failed:loginCallBack is null.");
            return;
        }

        if (!isInitStatus()) {
            SDKToast.getInstance().ToastShow("The SDK is not initialized.", 3);
            loginCallBack.onFailure("The SDK is not initialized.");
            return;
        }

        //登出操作
        handleLogout();

        //登录操作
        sdkLoginNoUI(activity,userName,pwd,loginCallBack);
    }

    public void sdkLogin2Dialog(final Activity activity, final String userName, final String pwd,
                                final LoginCallBack loginCallBack, final ResultCallBack resultCallBack) {

        try {
            if (activity == null) {
                System.out.println("sdkLogin failed:Activity is null.");
                return;
            }
            setActivity(activity);

            if (loginCallBack == null) {
                System.out.println("sdkLogin failed:loginCallBack is null.");
                return;
            }

            if (StringUtils.isBlank(userName) || StringUtils.isBlank(pwd)) {
                SDKToast.getInstance().ToastShow("UserName or password can't be empty.", 3);
                loginCallBack.onFailure("userName or pwd is null.");
                return;
            }

            //账号检查
            if (!SDKGameUtils.matchAccount(userName) || !SDKGameUtils.matchPw(pwd)) {
                return;
            }

            if (!isInitStatus()) {
                SDKToast.getInstance().ToastShow("The SDK is not initialized.", 3);
                loginCallBack.onFailure("The SDK is not initialized.");
                return;
            }

            final String aesKey = AESUtils.generate16SecretKey();
            String publicKey = SPManager.getInstance(activity).getString(SPKey.PUBLIC_KEY);
            String aesKey16byRSA = RSAUtils.encryptData(aesKey.getBytes(), RSAUtils.loadPublicKey(publicKey));

            showProgress(activity);
            ApiManager.getInstance().SDKLoginGo(aesKey, aesKey16byRSA, userName, pwd, new NetCallBack() {
                @Override
                public void onSuccess(String result) {
                    hideProgress();

                    LogUtils.e(TAG, "SDKLoginGo---onSuccess:" + result);
                    if (result == null || result.isEmpty()) {
                        loginCallBack.onFailure("result is null.");
                        return;
                    }
                    try {
                        String decodeData = AESUtils.decrypt(result, aesKey);
                        SDKLoginModel loginModel = (SDKLoginModel) GsonUtils.json2Bean(decodeData, SDKLoginModel.class);
                        if (loginModel == null) {
                            loginCallBack.onFailure("loginModel is null.");
                            return;
                        }
                        if (loginModel.getCode() == 1) {
                            User user = new User();
                            user.setUserId(loginModel.getData().getPassportId());
                            user.setTicket(loginModel.getData().getTicket());
                            user.setSdkmemberType(SDKConstant.TYPE_ACCOUNT);
                            setUser(user);
                            loginCallBack.onSuccess(user);

                            //记住账号密码
                            SPManager.getInstance(activity).putString(SPKey.key_user_name_last_login, userName);
                            SPManager.getInstance(activity).putString(SPKey.key_pwd_last_login, pwd);

                            //登录追踪
                            TrackingManager.loginTracking(loginModel.getData().getPassportId());

                            if (resultCallBack != null) resultCallBack.onSuccess();

                        } else {
                            LogUtils.e(TAG, "SDKLoginGo---onSuccess:" + loginModel.getMessage());
                            SDKGameUtils.showServiceInfo(loginModel.getCode(), loginModel.getMessage());
                            loginCallBack.onFailure(loginModel.getMessage());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(String errorMsg) {
                    hideProgress();
                    LogUtils.e(TAG, "SDKLoginGo---onFailure:" + errorMsg);
                    loginCallBack.onFailure(errorMsg);
                }
            });

        } catch (Exception e) {
            hideProgress();
            e.printStackTrace();
        }
    }

    public void sdkLoginWithFacebook(Activity activity, LoginCallBack loginCallBack) {

        if (activity == null) {
            System.out.println("sdkLoginWithFacebook failed:Activity is null.");
            return;
        }

        if (loginCallBack == null) {
            System.out.println("sdkLoginWithGoogle failed:loginCallBack is null.");
            return;
        }

        if (!isInitStatus()) {
            SDKToast.getInstance().ToastShow("The SDK is not initialized.", 3);
            loginCallBack.onFailure("The SDK is not initialized.");
            return;
        }
//        LoginManager.getInstance().facebookLogin(activity, loginCallBack);
    }

    public void sdkLoginWithGoogle(Activity activity, LoginCallBack loginCallBack) {

        if (activity == null) {
            System.out.println("sdkLoginWithGoogle failed:Activity is null.");
            return;
        }

        if (loginCallBack == null) {
            System.out.println("sdkLoginWithGoogle failed:loginCallBack is null.");
            return;
        }

        if (!isInitStatus()) {
            SDKToast.getInstance().ToastShow("The SDK is not initialized.", 3);
            loginCallBack.onFailure("The SDK is not initialized.");
            return;
        }

//        LoginManager.getInstance().googleLogin(activity, loginCallBack);
    }

    /**
     * 游客登录
     *
     * @param activity
     * @param loginCallBack
     */
    public void sdkLoginWithVisitor(Activity activity, LoginCallBack loginCallBack) {

        if (activity == null) {
            System.out.println("sdkLoginWithVisitor failed:Activity is null.");
            return;
        }
        setActivity(activity);

        if (loginCallBack == null) {
            System.out.println("sdkLoginWithVisitor failed:loginCallBack is null.");
            return;
        }

        if (!isInitStatus()) {
            SDKToast.getInstance().ToastShow("The SDK is not initialized.", 3);
            loginCallBack.onFailure("The SDK is not initialized.");
            return;
        }
        LoginManager.getInstance().visitorLogin(activity, loginCallBack);

    }

    /**
     * SDK三方账户登录接口
     *
     * @param activity
     * @param loginCallBack
     */
    public void sdkLoginThirdAccount(Activity activity, String oauthId, final String oauthSource, final LoginCallBack loginCallBack) {

        try {
            if (activity == null) {
                System.out.println("sdkLoginThirdAccount failed:Activity is null.");
                return;
            }
            setActivity(activity);

            if (loginCallBack == null) {
                System.out.println("sdkLoginThirdAccount failed:loginCallBack is null.");
                return;
            }

            if (!isInitStatus()) {
                SDKToast.getInstance().ToastShow("The SDK is not initialized.", 3);
                loginCallBack.onFailure("The SDK is not initialized.");
                return;
            }

            final String aesKey = AESUtils.generate16SecretKey();
            String publicKey = SPManager.getInstance(activity).getString(SPKey.PUBLIC_KEY);
            String aesKey16byRSA = RSAUtils.encryptData(aesKey.getBytes(), RSAUtils.loadPublicKey(publicKey));

            showProgress(activity);
            ApiManager.getInstance().SDKLoginThird(aesKey, aesKey16byRSA, oauthId, oauthSource, new NetCallBack() {
                @Override
                public void onSuccess(String result) {
                    hideProgress();

                    LogUtils.e(TAG, "sdkLoginThirdAccount---onSuccess:" + result);
                    if (result == null || result.isEmpty()) {
                        loginCallBack.onFailure("result is null.");
                        return;
                    }
                    try {
                        String decodeData = AESUtils.decrypt(result, aesKey);
                        SDKLoginModel loginModel = (SDKLoginModel) GsonUtils.json2Bean(decodeData, SDKLoginModel.class);
                        if (loginModel == null) {
                            loginCallBack.onFailure("loginModel is null.");
                            return;
                        }
                        if (loginModel.getCode() == 1) {
                            User user = new User();
                            user.setUserId(loginModel.getData().getPassportId());
                            user.setTicket(loginModel.getData().getTicket());
                            user.setSdkmemberType(oauthSource);
                            setUser(user);
                            loginCallBack.onSuccess(user);

                            //游客登录追踪
                            TrackingManager.loginTracking(loginModel.getData().getPassportId());

                        } else {
                            LogUtils.e(TAG, "sdkLoginThirdAccount---onSuccess:" + loginModel.getMessage());
                            SDKGameUtils.showServiceInfo(loginModel.getCode(), loginModel.getMessage());
                            loginCallBack.onFailure(loginModel.getMessage());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(String errorMsg) {
                    hideProgress();
                    LogUtils.e(TAG, "sdkLoginThirdAccount---onFailure:" + errorMsg);
                    loginCallBack.onFailure(errorMsg);
                }
            });

        } catch (Exception e) {
            hideProgress();
            e.printStackTrace();
        }
    }


    /**
     * 打点日志上传
     *
     * @param activity
     */
    public void sdkUploadLog(Activity activity, String title, String content) {

        try {
            if (activity == null) {
                System.out.println("sdkLoginThirdAccount failed:Activity is null.");
                return;
            }
            setActivity(activity);

            if (!isInitStatus()) {
                SDKToast.getInstance().ToastShow("The SDK is not initialized.", 3);
//                callBack.onFailure("The SDK is not initialized.");
                return;
            }

            final String aesKey = AESUtils.generate16SecretKey();
            String publicKey = SPManager.getInstance(activity).getString(SPKey.PUBLIC_KEY);
            String aesKey16byRSA = RSAUtils.encryptData(aesKey.getBytes(), RSAUtils.loadPublicKey(publicKey));

            ApiManager.getInstance().SDKUploadLog(activity,aesKey, aesKey16byRSA, title, content, new NetCallBack() {
                @Override
                public void onSuccess(String result) {

                    LogUtils.e(TAG, "SDKUploadLog---onSuccess:" + result);
                    if (result == null || result.isEmpty()) {
//                        callBack.onFailure("result is null.");
                        return;
                    }
                    try {
                        String decodeData = AESUtils.decrypt(result, aesKey);
                        SDKResult sdkResult = (SDKResult) GsonUtils.json2Bean(decodeData, SDKResult.class);
                        if (sdkResult == null) {
//                            callBack.onFailure("loginModel is null.");
                            return;
                        }
                        if (sdkResult.getCode() == 1) {
                            //上传日志成功
//                            callBack.onSuccess();

                        } else {
                            LogUtils.e(TAG, "SDKUploadLog---onSuccess:" + sdkResult.getMessage());
//                            callBack.onFailure(sdkResult.getMessage());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(String errorMsg) {
                    LogUtils.e(TAG, "SDKUploadLog---onFailure:" + errorMsg);
//                    callBack.onFailure(errorMsg);
                }
            });

        } catch (Exception e) {
            hideProgress();
            e.printStackTrace();
        }
    }


    /**
     * 注销账号
     *
     * @param activity
     * @param logOutCallBack
     */
    public void sdkLogout(Activity activity, LogOutCallBack logOutCallBack) {

        if (activity == null) {
            System.out.println("sdkLogout failed:Activity is null.");
            return;
        }
        setActivity(activity);

        if (logOutCallBack == null) {
            System.out.println("logOutCallBack is null.");
            return;
        }

        handleLogout();

        logOutCallBack.onSuccess();
    }


    /**
     *
     * 登出公共操作
     *
     */
    public void handleLogout() {
        User user = new User();
        setUser(user);
        setAuto(false);
        //FB登出
        com.facebook.login.LoginManager.getInstance().logOut();
    }


    /**
     * SDK下单接口
     *
     * @param activity
     * @param purchaseCallBack
     */
    public void sdkPurchase(final Activity activity, String serverId, final String referenceId, String gameExt, final PurchaseCallBack purchaseCallBack) {

        try {
            if (activity == null) {
                System.out.println("sdkMakeOrder failed:Activity is null.");
                return;
            }
            setActivity(activity);

            if (purchaseCallBack == null) {
                System.out.println("sdkMakeOrder failed:loginCallBack is null.");
                return;
            }

            setPurchaseCallBack(purchaseCallBack);

            if (serverId == null || referenceId == null || gameExt == null) {
                purchaseCallBack.onFailure("serverId or referenceId or gameExt is null");
                return;
            }

            if (!isInitStatus()) {
                SDKToast.getInstance().ToastShow("The SDK is not initialized.", 3);
                purchaseCallBack.onFailure("The SDK is not initialized.");
                return;
            }

            if (getUser() == null || getUser().getUserId() == null) {
                SDKToast.getInstance().ToastShow("Please login first.", 3);
                purchaseCallBack.onFailure("Please login first.");
                return;
            }

            final String aesKey = AESUtils.generate16SecretKey();
            String publicKey = SPManager.getInstance(activity).getString(SPKey.PUBLIC_KEY);
            String aesKey16byRSA = RSAUtils.encryptData(aesKey.getBytes(), RSAUtils.loadPublicKey(publicKey));

            showProgress(activity);
            ApiManager.getInstance().SDKMakeOrder(aesKey, aesKey16byRSA, serverId, referenceId, gameExt, new NetCallBack() {
                @Override
                public void onSuccess(String result) {
                    hideProgress();

                    LogUtils.e(TAG, "sdkMakeOrder---onSuccess:" + result);
                    if (result == null || result.isEmpty()) {
                        purchaseCallBack.onFailure("result is null.");
                        return;
                    }
                    try {
                        String decodeData = AESUtils.decrypt(result, aesKey);
                        LogUtils.d(TAG, "下单:" + decodeData);
                        SDKOrderModel orderModel = (SDKOrderModel) GsonUtils.json2Bean(decodeData, SDKOrderModel.class);
                        if (orderModel == null) {
                            purchaseCallBack.onFailure("orderModel is null.");
                            return;
                        }
                        if (orderModel.getCode() == 1) {
                            //创建订单成功
                            String transactionId = orderModel.getData().getTransactionId();//订单号

                            //DB插入数据
                            PurchaseRecord purchaseRecord = new PurchaseRecord();
                            purchaseRecord.setTransactionId(transactionId);
                            purchaseRecord.setSkuId(referenceId);
                            purchaseRecord.setStatus(0);
                            DBManager.getInstance().insertOrReplace(purchaseRecord);


                            String payUrl = orderModel.getData().getPayUrl();
                            String channelCode = orderModel.getData().getChannelCode();
                            LogUtils.d(TAG, "payurl:" + payUrl + "    " + channelCode);
                            if (channelCode != null && channelCode.equals("WEBPAY") && StringUtils.isNotBlank(payUrl)) {
                                //使用WebPay
                                AppManager.startActivityWithData(PayWebActivity.class, payUrl, transactionId);
                            } else {
                                //使用Google内购一次性商品
                                LogUtils.d(TAG, "发起Google内购一次性商品");
                                GooglePayHelp.getInstance().initGoogleIAP(activity, referenceId, transactionId, BillingClient.SkuType.INAPP);
                            }

                        } else {
                            LogUtils.e(TAG, "sdkMakeOrder---onSuccess:" + orderModel.getMessage());
                            SDKGameUtils.showServiceInfo(orderModel.getCode(), orderModel.getMessage());
                            purchaseCallBack.onFailure(orderModel.getMessage());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(String errorMsg) {
                    hideProgress();
                    LogUtils.e(TAG, "sdkMakeOrder---onFailure:" + errorMsg);
                    purchaseCallBack.onFailure(errorMsg);
                }
            });

        } catch (Exception e) {
            hideProgress();
            e.printStackTrace();
        }
    }


    /**
     * SDK订阅商品接口
     *
     * @param activity
     * @param purchaseCallBack
     */
    public void sdkSubscription(final Activity activity, String serverId, final String referenceId,String gameExt, final PurchaseCallBack purchaseCallBack) {

        try {
            if (activity == null) {
                System.out.println("sdkMakeOrder failed:Activity is null.");
                return;
            }
            setActivity(activity);

            if (purchaseCallBack == null) {
                System.out.println("sdkMakeOrder failed:loginCallBack is null.");
                return;
            }

            setPurchaseCallBack(purchaseCallBack);

            if (serverId == null || referenceId == null || gameExt == null) {
                purchaseCallBack.onFailure("serverId or referenceId or gameExt is null");
                return;
            }

            if (!isInitStatus()) {
                SDKToast.getInstance().ToastShow("The SDK is not initialized.", 3);
                purchaseCallBack.onFailure("The SDK is not initialized.");
                return;
            }

            if (getUser() == null || getUser().getUserId() == null) {
                SDKToast.getInstance().ToastShow("Please login first.", 3);
                purchaseCallBack.onFailure("Please login first.");
                return;
            }

            final String aesKey = AESUtils.generate16SecretKey();
            String publicKey = SPManager.getInstance(activity).getString(SPKey.PUBLIC_KEY);
            String aesKey16byRSA = RSAUtils.encryptData(aesKey.getBytes(), RSAUtils.loadPublicKey(publicKey));

            showProgress(activity);
            ApiManager.getInstance().SDKMakeOrder(aesKey, aesKey16byRSA, serverId, referenceId, gameExt, new NetCallBack() {
                @Override
                public void onSuccess(String result) {
                    hideProgress();

                    LogUtils.e(TAG, "sdkMakeOrder---onSuccess:" + result);
                    if (result == null || result.isEmpty()) {
                        purchaseCallBack.onFailure("result is null.");
                        return;
                    }
                    try {
                        String decodeData = AESUtils.decrypt(result, aesKey);
                        LogUtils.d(TAG, "下单:" + decodeData);
                        SDKOrderModel orderModel = (SDKOrderModel) GsonUtils.json2Bean(decodeData, SDKOrderModel.class);
                        if (orderModel == null) {
                            purchaseCallBack.onFailure("orderModel is null.");
                            return;
                        }
                        if (orderModel.getCode() == 1) {
                            //创建订单成功
                            String transactionId = orderModel.getData().getTransactionId();//订单号

                            //DB插入数据
                            PurchaseRecord purchaseRecord = new PurchaseRecord();
                            purchaseRecord.setTransactionId(transactionId);
                            purchaseRecord.setSkuId(referenceId);
                            purchaseRecord.setStatus(0);
                            DBManager.getInstance().insertOrReplace(purchaseRecord);


                            //使用Google订阅
                            LogUtils.d(TAG, "发起Google订阅");
                            GooglePayHelp.getInstance().initGoogleIAP(activity, referenceId,transactionId, BillingClient.SkuType.SUBS);

                        } else {
                            LogUtils.e(TAG, "sdkMakeOrder---onSuccess:" + orderModel.getMessage());
                            SDKGameUtils.showServiceInfo(orderModel.getCode(), orderModel.getMessage());
                            purchaseCallBack.onFailure(orderModel.getMessage());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(String errorMsg) {
                    hideProgress();
                    LogUtils.e(TAG, "sdkMakeOrder---onFailure:" + errorMsg);
                    purchaseCallBack.onFailure(errorMsg);
                }
            });

        } catch (Exception e) {
            hideProgress();
            e.printStackTrace();
        }
    }

    /**
     * 查询内购商品的本地价格
     *
     * @param activity
     * @param skuList
     * @param callback
     */
    public void sdkQuerySkuLocalPrice(Activity activity, final List<String> skuList,String skuType, final SDKGetSkuDetailsCallback callback) {

        if (activity == null) {
            System.out.println("sdkQuerySkuLocalPrice failed:Activity is null.");
            return;
        }
        setActivity(activity);

        if (callback == null) {
            System.out.println("SDKGetSkuDetailsCallback == null");
            return;
        }

        if (skuList == null || skuList.size() == 0) {
            callback.onFailure("skuList is null or skuList.size() == 0");
            return;
        }
        if (!isInitStatus()) {
            SDKToast.getInstance().ToastShow("The SDK is not initialized.", 3);
            callback.onFailure("The SDK is not initialized.");
            return;
        }

        GooglePayHelp.getInstance().querySkuDetails(skuList,skuType, callback);

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
        if (activity == null) {
            System.out.println("activity is null.");
            return;
        }
        setActivity(activity);

        if (serverId == null || roleId == null || roleName == null) {
            System.out.println("parameter is null.");
            return;
        }
        if (!isInitStatus()) {
            SDKToast.getInstance().ToastShow("The SDK is not initialized.", 3);
            System.out.println("The SDK is not initialized.");
            return;
        }
        TrackingManager.createRoleTracking(activity, serverId, roleId, roleName);
    }

    /**
     * 切换SDK语言
     *
     * @param language
     */
    public void sdkUpdateLanguage(String language) {

        if (language == null || language.isEmpty()) {
            System.out.println("language is null.");
            return;
        }
        if (!isInitStatus()) {
            SDKToast.getInstance().ToastShow("The SDK is not initialized.", 3);
            System.out.println("The SDK is not initialized.");
            return;
        }
        SPManager.getInstance(getActivity()).putString(SPKey.key_sdk_language,language);
        SDKManager.getInstance().getInitParameter().setLanguage(language);
    }


    /**
     * 在activity的onResume()方法中调用
     */
    public void sdkOnResume(Activity activity) {

        if (activity == null) {
            System.out.println("sdkOnResume():activity == null");
            return;
        }

        if (!isInitStatus()) return;

        if (GooglePayHelp.getInstance().isConnected()) {
//            GooglePayHelp.getInstance().resentOrderByDbRecord();
            GooglePayHelp.getInstance().queryPurchase(false,SDKConstant.INAPP);
        }else {
            GooglePayHelp.getInstance().initGoogleIAP(activity, new BillingClientStateListener() {
                @Override
                public void onBillingSetupFinished(BillingResult billingResult) {
                    GooglePayHelp.getInstance().setConnected(true);
//                    GooglePayHelp.getInstance().resentOrderByDbRecord();
                    GooglePayHelp.getInstance().queryPurchase(false,SDKConstant.INAPP);
                }

                @Override
                public void onBillingServiceDisconnected() {
                    Log.i(TAG, "line1240-onBillingServiceDisconnected()");
                }
            });
        }
    }

    /**
     * 在activity的onDestroy()方法中调用
     */
    public void sdkOnDestroy() {

//        if (!isInitStatus()) return;
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
    public void sdkResetPwd(Activity activity, String account, String oldPwd, String newPwd, final LoginCallBack callback) {

        try {

            if (activity == null) {
                System.out.println("sdkResetPwd failed:Activity is null.");
                return;
            }
            setActivity(activity);

            if (callback == null) {
                System.out.println("ResetPwdCallBack == null");
                return;
            }

            if (StringUtils.isBlank(account) || StringUtils.isBlank(oldPwd) || StringUtils.isBlank(newPwd)) {
                SDKToast.getInstance().ToastShow("Account,oldPwd and newPwd can't be empty.", 3);
                callback.onFailure("ic_account||oldPwd||newPwd is null.");
                return;
            }

            //账号检查
            if (!SDKGameUtils.matchAccount(account) || !SDKGameUtils.matchPw(oldPwd) || !SDKGameUtils.matchPw(newPwd)) {
                return;
            }

            if (!isInitStatus()) {
                SDKToast.getInstance().ToastShow("The SDK is not initialized.", 3);
                callback.onFailure("The SDK is not initialized.");
                return;
            }

            final String aesKey = AESUtils.generate16SecretKey();
            String publicKey = SPManager.getInstance(activity).getString(SPKey.PUBLIC_KEY);
            String aesKey16byRSA = RSAUtils.encryptData(aesKey.getBytes(), RSAUtils.loadPublicKey(publicKey));

            showProgress(activity);
            ApiManager.getInstance().SDKResetPwd(aesKey, aesKey16byRSA, account, oldPwd, newPwd, new NetCallBack() {
                @Override
                public void onSuccess(String result) {
                    hideProgress();
                    if (result == null || result.isEmpty()) {
                        callback.onFailure("result is null.");
                        return;
                    }
                    try {

                        String decodeData = AESUtils.decrypt(result, aesKey);
                        LogUtils.d(TAG, "重置密码:" + decodeData);
                        SDKLoginModel loginModel = (SDKLoginModel) GsonUtils.json2Bean(decodeData, SDKLoginModel.class);
                        if (loginModel == null) {
                            callback.onFailure("sdkLoginModel is null.");
                            return;
                        }

                        if (loginModel.getCode() == 1) {
                            //修改密码成功
                            User user = new User();
                            user.setUserId(loginModel.getData().getPassportId());
                            user.setTicket(loginModel.getData().getTicket());
                            setUser(user);
                            callback.onSuccess(user);

                            SDKToast.getInstance().ToastShow(SDKLangConfig.getInstance().findMessage("resetPwdOk"), 1);
                        } else {
                            SDKGameUtils.showServiceInfo(loginModel.getCode(), loginModel.getMessage());
                            callback.onFailure(loginModel.getMessage());
                            LogUtils.e(TAG, "sdkResetPwd---onFailure:" + loginModel.getMessage());
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        callback.onFailure(e.getMessage());
                    }
                }

                @Override
                public void onFailure(String msg) {
                    hideProgress();
                    callback.onFailure(msg);
                    LogUtils.e(TAG, "sdkResetPwd---onFailure:" + msg);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 第三方账号绑定账号密码
     *
     * @param activity
     * @param oauthid     第三方账号ID
     * @param oauthsource 第三方账号来源
     * @param account     账号
     * @param second      密码
     * @param callback
     */
    public void sdkBindAccount2Dialog(Activity activity, String oauthid, String oauthsource, String account, String second, final ResultCallBack callback) {

        try {

            if (activity == null) {
                System.out.println("sdkResetPwd failed:Activity is null.");
                return;
            }
            setActivity(activity);

            if (callback == null) {
                System.out.println("ResultCallBack == null");
                return;
            }

            if (StringUtils.isBlank(account) || StringUtils.isBlank(second) || StringUtils.isBlank(oauthid) || StringUtils.isBlank(oauthsource)) {
                SDKToast.getInstance().ToastShow("Account,password and oauthId can't be empty.", 3);
                callback.onFailure("account||second||oauthId||oauthSource is null.");
                return;
            }

            //账号检查
            if (!SDKGameUtils.matchAccount(account) || !SDKGameUtils.matchPw(second)) {
                return;
            }

            if (!isInitStatus()) {
                SDKToast.getInstance().ToastShow("The SDK is not initialized.", 3);
                callback.onFailure("The SDK is not initialized.");
                return;
            }

            final String aesKey = AESUtils.generate16SecretKey();
            String publicKey = SPManager.getInstance(activity).getString(SPKey.PUBLIC_KEY);
            String aesKey16byRSA = RSAUtils.encryptData(aesKey.getBytes(), RSAUtils.loadPublicKey(publicKey));

            showProgress(activity);
            ApiManager.getInstance().SDKBindAccount(aesKey, aesKey16byRSA, oauthid, oauthsource, account, second, new NetCallBack() {
                @Override
                public void onSuccess(String result) {
                    hideProgress();
                    if (result == null || result.isEmpty()) {
                        callback.onFailure("result is null.");
                        return;
                    }
                    try {

                        String decodeData = AESUtils.decrypt(result, aesKey);
                        LogUtils.d(TAG, "绑定账号data:" + decodeData);
                        SDKLoginModel loginModel = (SDKLoginModel) GsonUtils.json2Bean(decodeData, SDKLoginModel.class);
                        if (loginModel == null) {
                            callback.onFailure("sdkLoginModel is null.");
                            return;
                        }

                        if (loginModel.getCode() == 1) {
                            //绑定账号成功

                            SDKToast.getInstance().ToastShow(SDKLangConfig.getInstance().findMessage("bindSucess"), 1);
                            //ticket重新生成了，保存ticket
                            User user = new User();
                            user.setUserId(loginModel.getData().getPassportId());
                            user.setTicket(loginModel.getData().getTicket());
                            user.setSdkmemberType(SDKConstant.TYPE_ACCOUNT);
                            SDKManager.getInstance().setUser(user);

                            callback.onSuccess();

                        } else if (loginModel.getCode() == -8){
                            //游客账号已绑定
                            User user = getUser();
                            user.setSdkmemberType(SDKConstant.TYPE_ACCOUNT);
                            SDKManager.getInstance().setUser(user);

                            SDKGameUtils.showServiceInfo(loginModel.getCode(), loginModel.getMessage());
                            callback.onSuccess();
                        } else {
                            SDKGameUtils.showServiceInfo(loginModel.getCode(), loginModel.getMessage());
                            callback.onFailure(loginModel.getMessage());
                            LogUtils.e(TAG, "sdkBindAccount---onFailure:" + loginModel.getMessage());
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        callback.onFailure(e.getMessage());
                    }
                }

                @Override
                public void onFailure(String msg) {
                    hideProgress();
                    callback.onFailure(msg);
                    LogUtils.e(TAG, "sdkBindAccount---onFailure:" + msg);
                }
            });

        } catch (Exception e) {
            if (callback != null) callback.onFailure(e.getMessage());
            e.printStackTrace();
        }

    }

    /**
     * 第三方账号绑定账号密码
     *
     * @param activity
     * @param account
     * @param oauthid     第三方账号ID
     * @param oauthsource 第三方账号来源
     * @param account     账号
     * @param second      密码
     * @param callback
     */
    public void sdkBindAccount(Activity activity, String oauthid, String oauthsource, String account, String second, final ResultCallBack callback) {

        try {

            if (activity == null) {
                System.out.println("sdkResetPwd failed:Activity is null.");
                return;
            }
            setActivity(activity);

            if (callback == null) {
                System.out.println("ResultCallBack == null");
                return;
            }

            if (StringUtils.isBlank(account) || StringUtils.isBlank(second) || StringUtils.isBlank(oauthid) || StringUtils.isBlank(oauthsource)) {
                SDKToast.getInstance().ToastShow("Account,password and oauthId can't be empty.", 3);
                callback.onFailure("account||second||oauthId||oauthSource is null.");
                return;
            }

            //账号检查
            if (!SDKGameUtils.matchAccount(account) || !SDKGameUtils.matchPw(second)) {
                return;
            }

            if (!isInitStatus()) {
                SDKToast.getInstance().ToastShow("The SDK is not initialized.", 3);
                callback.onFailure("The SDK is not initialized.");
                return;
            }

            final String aesKey = AESUtils.generate16SecretKey();
            String publicKey = SPManager.getInstance(activity).getString(SPKey.PUBLIC_KEY);
            String aesKey16byRSA = RSAUtils.encryptData(aesKey.getBytes(), RSAUtils.loadPublicKey(publicKey));

            showProgress(activity);
            ApiManager.getInstance().SDKBindAccount(aesKey, aesKey16byRSA, oauthid, oauthsource, account, second, new NetCallBack() {
                @Override
                public void onSuccess(String result) {
                    hideProgress();
                    if (result == null || result.isEmpty()) {
                        callback.onFailure("result is null.");
                        return;
                    }
                    try {

                        String decodeData = AESUtils.decrypt(result, aesKey);
                        LogUtils.d(TAG, "绑定账号data:" + decodeData);
                        SDKLoginModel loginModel = (SDKLoginModel) GsonUtils.json2Bean(decodeData, SDKLoginModel.class);
                        if (loginModel == null) {
                            callback.onFailure("sdkLoginModel is null.");
                            return;
                        }

                        if (loginModel.getCode() == 1) {
                            //绑定账号成功

                            SDKToast.getInstance().ToastShow(SDKLangConfig.getInstance().findMessage("bindSucess"), 1);
                            //ticket改变了，重新保存User
                            User user = new User();
                            user.setUserId(loginModel.getData().getPassportId());
                            user.setTicket(loginModel.getData().getTicket());
                            user.setSdkmemberType(SDKConstant.TYPE_ACCOUNT);
                            SDKManager.getInstance().setUser(user);

                            callback.onSuccess();
                        } else if (loginModel.getCode() == -8){
                            //游客账号已绑定
                            User user = getUser();
                            user.setSdkmemberType(SDKConstant.TYPE_ACCOUNT);
                            SDKManager.getInstance().setUser(user);

                            SDKGameUtils.showServiceInfo(loginModel.getCode(), loginModel.getMessage());
                            callback.onSuccess();

                        } else {
                            SDKGameUtils.showServiceInfo(loginModel.getCode(), loginModel.getMessage());
                            callback.onFailure(loginModel.getMessage());
                            LogUtils.e(TAG, "sdkBindAccount---onFailure:" + loginModel.getMessage());
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        callback.onFailure(e.getMessage());
                    }
                }

                @Override
                public void onFailure(String msg) {
                    hideProgress();
                    callback.onFailure(msg);
                    LogUtils.e(TAG, "sdkBindAccount---onFailure:" + msg);
                }
            });

        } catch (Exception e) {
            if (callback != null) callback.onFailure(e.getMessage());
            e.printStackTrace();
        }

    }

    /**
     * 游客绑定FB账户
     *
     * @param activity
     * @param callBack
     */
    public void sdkGuestBindFB(final Activity activity,final ResultCallBack callBack){

        LoginManager.getInstance().facebookLogin(activity, new ThirdLoginResultCallBack() {
            @Override
            public void onSuccess(String thirdId) {
                LogUtils.d(TAG,"FBId:"+thirdId);
                sdkGuestBindThirdAccount(activity,thirdId,"facebook",callBack);
            }

            @Override
            public void onFailure(String msg) {

            }
        });

    }

    /**
     * 游客绑定第三方账户
     *
     * @param activity
     * @param thirdId     第三方账号ID
     * @param thirdSource 第三方账号来源
     * @param callback
     */
    private void sdkGuestBindThirdAccount(Activity activity, String thirdId, String thirdSource, final ResultCallBack callback) {

        try {

            if (activity == null) {
                System.out.println("sdkGuestBindThirdAccount failed:Activity is null.");
                return;
            }
            setActivity(activity);

            if (callback == null) {
                System.out.println("ResultCallBack == null");
                return;
            }

            if (!isInitStatus()) {
                SDKToast.getInstance().ToastShow("The SDK is not initialized.", 3);
                callback.onFailure("The SDK is not initialized.");
                return;
            }

            final String aesKey = AESUtils.generate16SecretKey();
            String publicKey = SPManager.getInstance(activity).getString(SPKey.PUBLIC_KEY);
            String aesKey16byRSA = RSAUtils.encryptData(aesKey.getBytes(), RSAUtils.loadPublicKey(publicKey));

            String oauthId = Installations.id(activity);
            showProgress(activity);
            ApiManager.getInstance().SDKGuestBindThirdAccount(aesKey, aesKey16byRSA, oauthId, thirdId,thirdSource, new NetCallBack() {
                @Override
                public void onSuccess(String result) {
                    hideProgress();
                    if (result == null || result.isEmpty()) {
                        callback.onFailure("result is null.");
                        return;
                    }
                    try {

                        String decodeData = AESUtils.decrypt(result, aesKey);
                        LogUtils.d(TAG, "游客绑定三方data:" + decodeData);
                        SDKLoginModel loginModel = (SDKLoginModel) GsonUtils.json2Bean(decodeData, SDKLoginModel.class);
                        if (loginModel == null) {
                            callback.onFailure("sdkLoginModel is null.");
                            return;
                        }

                        if (loginModel.getCode() == 1) {
                            //绑定账号成功

                            SDKToast.getInstance().ToastShow(SDKLangConfig.getInstance().findMessage("bindSucess"), 1);
                            //ticket改变了，重新保存User
                            User user = new User();
                            user.setUserId(loginModel.getData().getPassportId());
                            user.setTicket(loginModel.getData().getTicket());
                            user.setSdkmemberType(SDKConstant.TYPE_ACCOUNT);
                            SDKManager.getInstance().setUser(user);

                            callback.onSuccess();
                        } else if (loginModel.getCode() == -8){
                            //游客账号已绑定
                            User user = getUser();
                            user.setSdkmemberType(SDKConstant.TYPE_ACCOUNT);
                            SDKManager.getInstance().setUser(user);

                            SDKGameUtils.showServiceInfo(loginModel.getCode(), loginModel.getMessage());
                            callback.onSuccess();

                        } else {
                            SDKGameUtils.showServiceInfo(loginModel.getCode(), loginModel.getMessage());
                            callback.onFailure(loginModel.getMessage());
                            LogUtils.e(TAG, "sdkGuestBindThirdAccount---onFailure:" + loginModel.getMessage());
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        callback.onFailure(e.getMessage());
                    }
                }

                @Override
                public void onFailure(String msg) {
                    hideProgress();
                    callback.onFailure(msg);
                    LogUtils.e(TAG, "sdkGuestBindThirdAccount---onFailure:" + msg);
                }
            });

        } catch (Exception e) {
            if (callback != null) callback.onFailure(e.getMessage());
            e.printStackTrace();
        }

    }


    /**
     *
     * 用户中心：坚果账号绑定邮箱
     * @param activity
     * @param email 邮箱
     */
    public void sdkUserBindEmailSendCode(Activity activity, String email, final ResultCallBack resultCallBack) {

        try {

            if (resultCallBack == null)return;
            if (activity == null) {
                System.out.println("sdkUserBindEmailSendCode failed:Activity is null.");
                return;
            }
            setActivity(activity);

            if (!isInitStatus()) {
                SDKToast.getInstance().ToastShow("The SDK is not initialized.", 3);
                return;
            }

            final String aesKey = AESUtils.generate16SecretKey();
            String publicKey = SPManager.getInstance(activity).getString(SPKey.PUBLIC_KEY);
            String aesKey16byRSA = RSAUtils.encryptData(aesKey.getBytes(), RSAUtils.loadPublicKey(publicKey));


            if (SDKManager.getInstance().getUser() == null) return;
            String ticket = SDKManager.getInstance().getUser().getTicket();
            showProgress(activity);
            ApiManager.getInstance().SDKRequestBindEmail(aesKey, aesKey16byRSA, ticket, email, new NetCallBack() {
                @Override
                public void onSuccess(String result) {
                    hideProgress();
                    if (result == null || result.isEmpty()) return;
                    try {

                        String decodeData = AESUtils.decrypt(result, aesKey);
                        LogUtils.d(TAG, "发送邮箱验证码data:" + decodeData);
                        VerifyCodeResult verifyCodeResult = (VerifyCodeResult) GsonUtils.json2Bean(decodeData, VerifyCodeResult.class);
                        if (verifyCodeResult == null) return;

                        if (verifyCodeResult.getCode() == 1) {
                            //发送邮箱验证码成功
                            SDKToast.getInstance().ToastShow(SDKLangConfig.getInstance().findMessage("sendVerifySuccess"), 1);
                            resultCallBack.onSuccess();
                        } else {
                            SDKGameUtils.showServiceInfo(verifyCodeResult.getCode(), verifyCodeResult.getMessage());
                            LogUtils.e(TAG, "sdkUserBindEmailSendCode---onFailure:" + verifyCodeResult.getMessage());
                            resultCallBack.onFailure(verifyCodeResult.getMessage());
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        resultCallBack.onFailure(e.getMessage());
                    }
                }

                @Override
                public void onFailure(String msg) {
                    hideProgress();
                    LogUtils.e(TAG, "sdkUserBindEmailSendCode---onFailure:" + msg);
                    SDKToast.getInstance().ToastShow(SDKLangConfig.getInstance().findMessage("Send VerifyCode Failed:" + msg), 3);
                    resultCallBack.onFailure(msg);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * 坚果账号绑定邮箱
     *
     * @param activity
     * @param email 邮箱
     * @param verifyCode 邮箱验证码
     */
    public void sdkUserBindEmail(Activity activity, String email, String verifyCode, final ResultCallBack resultCallBack) {

        try {

            if (resultCallBack == null) return;
            if (activity == null) {
                System.out.println("sdkUserBindEmail failed:Activity is null.");
                return;
            }
            setActivity(activity);

            if (!isInitStatus()) {
                SDKToast.getInstance().ToastShow("The SDK is not initialized.", 3);
                return;
            }

            final String aesKey = AESUtils.generate16SecretKey();
            String publicKey = SPManager.getInstance(activity).getString(SPKey.PUBLIC_KEY);
            String aesKey16byRSA = RSAUtils.encryptData(aesKey.getBytes(), RSAUtils.loadPublicKey(publicKey));


            if (SDKManager.getInstance().getUser() == null) return;
            String ticket = SDKManager.getInstance().getUser().getTicket();
            showProgress(activity);
            ApiManager.getInstance().SDKBindEmailConfirm(aesKey, aesKey16byRSA, ticket, email, verifyCode, new NetCallBack() {
                @Override
                public void onSuccess(String result) {
                    hideProgress();
                    if (result == null || result.isEmpty()) {
                        return;
                    }
                    try {

                        String decodeData = AESUtils.decrypt(result, aesKey);
                        LogUtils.d(TAG, "账号绑定邮箱data:" + decodeData);
                        VerifyCodeResult sdkResult = (VerifyCodeResult) GsonUtils.json2Bean(decodeData, VerifyCodeResult.class);
                        if (sdkResult == null) return;
                        if (sdkResult.getCode() == 1) {
                            //绑定账号成功
                            SDKToast.getInstance().ToastShow(SDKLangConfig.getInstance().findMessage("bindEmailSuccess"), 1);
                            resultCallBack.onSuccess();
                        } else {
                            SDKGameUtils.showServiceInfo(sdkResult.getCode(), sdkResult.getMessage());
                            LogUtils.e(TAG, "SDKBindEmailConfirm---onFailure:" + sdkResult.getMessage());
                            resultCallBack.onFailure(sdkResult.getMessage());
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        resultCallBack.onFailure(e.getMessage());
                    }
                }

                @Override
                public void onFailure(String msg) {
                    hideProgress();
                    LogUtils.e(TAG, "SDKBindEmailConfirm---onFailure:" + msg);
                    SDKToast.getInstance().ToastShow(SDKLangConfig.getInstance().findMessage("Bind Email Failed:" + msg), 2);
                    resultCallBack.onFailure(msg);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 重置密码：发送验证码
     *
     * @param activity
     * @param account 用户的账号（需要绑定邮箱）
     */
    public void sdkResetPwdSendCode(Activity activity, String account, final ResultCallBack resultCallBack) {

        try {

            if (resultCallBack == null) return;
            if (activity == null) {
                System.out.println("sdkResetPwdSendCode failed:Activity is null.");
                return;
            }
            setActivity(activity);

            if (!isInitStatus()) {
                SDKToast.getInstance().ToastShow("The SDK is not initialized.", 3);
                return;
            }

            final String aesKey = AESUtils.generate16SecretKey();
            String publicKey = SPManager.getInstance(activity).getString(SPKey.PUBLIC_KEY);
            String aesKey16byRSA = RSAUtils.encryptData(aesKey.getBytes(), RSAUtils.loadPublicKey(publicKey));


            if (SDKManager.getInstance().getUser() == null) return;
            showProgress(activity);
            ApiManager.getInstance().SDKRequestResetPwd(aesKey, aesKey16byRSA, account, new NetCallBack() {
                @Override
                public void onSuccess(String result) {
                    hideProgress();
                    if (result == null || result.isEmpty()) return;

                    try {
                        String decodeData = AESUtils.decrypt(result, aesKey);
                        LogUtils.d(TAG, "重置密码发送验证码data:" + decodeData);
                        VerifyCodeResult codeResult = (VerifyCodeResult) GsonUtils.json2Bean(decodeData, VerifyCodeResult.class);
                        if (codeResult == null) return;

                        if (codeResult.getCode() == 1) {
                            //发送邮箱验证码成功
                            SDKToast.getInstance().ToastShow(SDKLangConfig.getInstance().findMessage("sendVerifySuccess"), 1);
                            resultCallBack.onSuccess();
                        } else {
                            SDKGameUtils.showServiceInfo(codeResult.getCode(), codeResult.getMessage());
                            LogUtils.e(TAG, "SDKRequestResetPwd---onFailure:" + codeResult.getMessage());
                            resultCallBack.onFailure(codeResult.getMessage());
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        resultCallBack.onFailure(e.getMessage());
                    }
                }

                @Override
                public void onFailure(String msg) {
                    hideProgress();
                    SDKToast.getInstance().ToastShow(SDKLangConfig.getInstance().findMessage("Send VerifyCode Failed:" + msg), 3);
                    LogUtils.e(TAG, "SDKRequestResetPwd---onFailure:" + msg);
                    resultCallBack.onFailure(msg);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * 重置密码
     *
     * @param activity
     * @param account 邮箱
     * @param verifyCode 邮箱验证码
     * @param newPwd 新密码
     */
    public void sdkResetPwdByCode(Activity activity, String account, String verifyCode, String newPwd, final ResultCallBack resultCallBack) {

        try {

            if (resultCallBack == null)return;
            if (activity == null) {
                System.out.println("sdkResetPwdByCode failed:Activity is null.");
                return;
            }
            setActivity(activity);

            if (!isInitStatus()) {
                SDKToast.getInstance().ToastShow("The SDK is not initialized.", 3);
                return;
            }

            final String aesKey = AESUtils.generate16SecretKey();
            String publicKey = SPManager.getInstance(activity).getString(SPKey.PUBLIC_KEY);
            String aesKey16byRSA = RSAUtils.encryptData(aesKey.getBytes(), RSAUtils.loadPublicKey(publicKey));


            if (SDKManager.getInstance().getUser() == null) return;
            showProgress(activity);
            ApiManager.getInstance().SDKResetPwdByVerifycode(aesKey, aesKey16byRSA, account, verifyCode ,newPwd, new NetCallBack() {
                @Override
                public void onSuccess(String result) {
                    hideProgress();
                    if (result == null || result.isEmpty()) return;
                    try {

                        String decodeData = AESUtils.decrypt(result, aesKey);
                        LogUtils.d(TAG, "请求账号绑定邮箱data:" + decodeData);
                        VerifyCodeResult sdkResult = (VerifyCodeResult) GsonUtils.json2Bean(decodeData, VerifyCodeResult.class);
                        if (sdkResult == null) return;
                        if (sdkResult.getCode() == 1) {
                            //绑定账号成功
                            SDKManager.getInstance().handleLogout();
                            SDKToast.getInstance().ToastShow(SDKLangConfig.getInstance().findMessage("resetPwdSuccess"), 1);
                            resultCallBack.onSuccess();
                        } else {
                            SDKGameUtils.showServiceInfo(sdkResult.getCode(), sdkResult.getMessage());
                            LogUtils.e(TAG, "SDKResetPwdByVerifyCode---onFailure:" + sdkResult.getMessage());
                            resultCallBack.onFailure(sdkResult.getMessage());
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        resultCallBack.onFailure(e.getMessage());
                    }
                }

                @Override
                public void onFailure(String msg) {
                    hideProgress();
                    LogUtils.e(TAG, "SDKResetPwdByVerifyCode---onFailure:" + msg);
                    SDKToast.getInstance().ToastShow(SDKLangConfig.getInstance().findMessage("Reset Password Failed:" + msg), 3);
                    resultCallBack.onFailure(msg);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    /**
     * 打开用户中心
     */
    public void openUserCenter(Activity activity){

        UserCenterDialog.Builder builder = new UserCenterDialog.Builder(activity);
        builder.create().show();
    }

    /**
     *
     * 截图保存
     *
     * @param activity
     */
    public void saveShot(Activity activity) {

        if (activity == null) return;
        AppManager.startActivity(ScreenShotActivity.class);


    }


    /**
     * 获取FB用户信息
     *
     * @param activity
     * @param resultCallBack
     */
    public void sdkGetFbUserInfo(Activity activity, ResultCallBack resultCallBack) {

        if (activity == null || resultCallBack==null) return;
        AppManager.startActivity(FBLoginActivity.class);
    }

    public void facebookGameLogin(LoginManager.FbLoginListener fbLoginListener) {

        if (fbLoginListener == null) return;
        AppManager.startActivity(FBLoginActivity.class);
        LoginManager.getInstance().setFBLoginListener(fbLoginListener);
    }

    public void facebookFriendFinder() {
        AppManager.startActivity(FBLoginActivity.class);
        LoginManager.getInstance().setFBLoginListener(new LoginManager.FbLoginListener() {
            @Override
            public void onSuccess(String fbId,String name) {

            }

            @Override
            public void onFailure(String msg) {

            }
        });

    }

    public void facebookSharing() {

    }
}
