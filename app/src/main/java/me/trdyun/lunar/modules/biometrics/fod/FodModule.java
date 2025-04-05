package me.trdyun.lunar.modules.biometrics.fod;

import android.os.HwBinder;
import android.os.HwParcel;
import android.os.IBinder;
import android.os.IHwBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Slog;

import java.util.ArrayList;
import java.util.NoSuchElementException;

import me.trdyun.lunar.utils.MiTouchFeature;

//
// Created by trdyun on 2025/3/30.
// Copyright (c) 2025 CandyClub. All rights reserved.
//
public class FodModule {
    public static final String TAG = "LunarFodService";
    public static boolean IS_FOD = SystemProperties.getBoolean("ro.hardware.fp.fod", false);
    public static boolean IS_FODENGINE_ENABLED = SystemProperties.get("ro.hardware.fp.fod.touch.ctl.version", "").equals("2.0");
    public static boolean IS_LOADED = false;
    private static final Object DAEMON_BINDER_LOCK = new Object();
    public static final int SERVICE_VERSION_CODE_AIDL = 2;
    public static final int SERVICE_VERSION_CODE_HIDL = 1;
    public static final int SERVICE_VERSION_CODE_NONE = 0;

    private static final String NAME_EXT_DAEMON = "vendor.xiaomi.hardware.fingerprintextension@1.0::IXiaomiFingerprint";
    private static final String NAME_EXT_DAEMON_AIDL = "vendor.xiaomi.hardware.fingerprintextension.IXiaomiFingerprint/default";
    private static IHwBinder mExtDaemon;
    private static IHwBinder.DeathRecipient mDeathRecipient = new IHwBinder.DeathRecipient() {
        @Override
        public void serviceDied(long cookie) {
            if (mExtDaemon == null) return;
            try {
                mExtDaemon.unlinkToDeath(mDeathRecipient);
                mExtDaemon = null;
                mExtDaemon = getExtDaemon();
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
    private static IBinder mDaemonBinder;
    private static IBinder.DeathRecipient mDeathRecipientAidl = new IBinder.DeathRecipient(){
        @Override
        public void binderDied() {
            if (mDaemonBinder == null) return;
            mDaemonBinder.unlinkToDeath(mDeathRecipientAidl, 0);
            mDaemonBinder = null;
        }
    };

    private static int mServiceVersion = 0;

    private static IHwBinder getExtDaemon() throws RemoteException{
        if (mExtDaemon == null) {
            IHwBinder service = HwBinder.getService(NAME_EXT_DAEMON, "default");
            mExtDaemon = service;
            service.linkToDeath(mDeathRecipient, 0L);
        }
        return mExtDaemon;
    }

    private static IBinder getExtDaemonAidl() throws RemoteException{
        if (mDaemonBinder == null) {
            IBinder service = ServiceManager.getService(NAME_EXT_DAEMON_AIDL);
            mDaemonBinder = service;
            service.linkToDeath(mDeathRecipientAidl, 0);
        }
        return mDaemonBinder;
    }

    public synchronized static void fodCall(String sceneCode, int param) {
        if (!IS_LOADED || !IS_FODENGINE_ENABLED || !IS_FOD) {
            Log.w(TAG, "Not Udfps device or xiaomi hal not init, pass");
            return;
        }

        FodScene scene = FodScene.getSceneByCode(sceneCode);
        int touchCmd = -1;
        int sfCmd = -1;
        switch (scene) {
            case CMD_APP_ENROLL:
            case CMD_APP_AUTHEN:
            case CMD_KEYGUARD_DETECT:
                touchCmd = 1;
                break;
            case CMD_APP_CANCEL_AUTHEN:
            case CMD_APP_CANCEL_ENROLL:
            case CMD_KEYGUARD_CANCEL_DETECT:
            case CMD_FW_LOCK_CANCEL:
            case CMD_VENDOR_ERROR:
            case CMD_VENDOR_REMOVED:
            case CMD_FW_TOP_APP_CANCEL:
                touchCmd = 0;
                break;
            case CMD_VENDOR_ENROLL_RES:
                if (param == 0) touchCmd = 0; else touchCmd = -1;
                break;
            case CMD_VENDOR_AUTHENTICATED:
                if (param != 0) touchCmd = 0; else touchCmd = -1;
                break;
            case UNKNOWN:
            default:
                Log.w(TAG, "Unknown Scene,Fuck you");
                return;
        }
        if (touchCmd != -1) MiTouchFeature.getInstance().setTouchMode(0,10 , touchCmd);
        switch (scene) {
            case CMD_APP_ENROLL:
                sfCmd = 1;
                break;
            case CMD_APP_CANCEL_ENROLL:
                sfCmd = 2;
                break;
            case CMD_VENDOR_ENROLL_RES:
                if (param == 0) sfCmd = 2; else  sfCmd = 1;
                break;
            case CMD_KEYGUARD_DETECT:
                sfCmd = 7;
                break;
            case CMD_KEYGUARD_CANCEL_DETECT:
                sfCmd = 8;
                break;
            case CMD_APP_AUTHEN:
                sfCmd = 3;
                break;
            case CMD_APP_CANCEL_AUTHEN:
            case CMD_VENDOR_ERROR:
            case CMD_FW_TOP_APP_CANCEL:
                sfCmd = 4;
                break;
            case CMD_VENDOR_AUTHENTICATED:
                if (param == 0) sfCmd = 3; else sfCmd = 4;
                break;
            case CMD_FW_LOCK_CANCEL:
                sfCmd = 0;
                break;
        }
        if (sfCmd == -1) return;
        if (scene == FodScene.CMD_KEYGUARD_DETECT) {
            callExtCmd(5,2);
        } else if (scene == FodScene.CMD_KEYGUARD_CANCEL_DETECT) {
            callExtCmd(5,0);
        }
        callExtCmd(4, sfCmd);
        if (scene == FodScene.CMD_APP_AUTHEN) {
            callExtCmd(7, 1);
        } else {
            callExtCmd(7, 0);
        }
    }

    private static final String EXT_DESCRIPTOR = "vendor.xiaomi.hardware.fingerprintextension@1.0::IXiaomiFingerprint";
    private static final String EXT_DESCRIPTOR_AIDL = "vendor.xiaomi.hardware.fingerprintextension.IXiaomiFingerprint";

    private static int callExtCmd(int cmd, int param) {
        if (cmd == 0) return -1;
        int result = -1;
        switch (mServiceVersion) {
            case -1:
                return -1;
            default:
            case 0:
                try {
                    if (getExtDaemon() != null) mServiceVersion = 1;
                    return callExtCmd(cmd, param);
                }catch (RemoteException e) {
                    Slog.e(TAG, e.getMessage());
                    e.printStackTrace();
                    mServiceVersion = -1;
                }
                try {
                    if (getExtDaemonAidl() != null) mServiceVersion = 2;
                    return callExtCmd(cmd, param);
                }catch (RemoteException e) {
                    Slog.e(TAG, e.getMessage());
                    e.printStackTrace();
                    mServiceVersion = -1;
                }
                return result;
            case 1:
                if (mExtDaemon == null) {
                    try {
                        getExtDaemon();
                    }catch (Exception e) {
                        Slog.e(TAG, "Failed when get miext hidl");
                    }
                }
                if (mExtDaemon != null) {
                    HwParcel hidl_reply = new HwParcel();
                    try {
                        try {
                            HwParcel hidl_request = new HwParcel();
                            hidl_request.writeInterfaceToken(EXT_DESCRIPTOR);
                            hidl_request.writeInt32(cmd);
                            hidl_request.writeInt32(param);
                            mExtDaemon.transact(1, hidl_request, hidl_reply, 0);
                            hidl_reply.verifySuccess();
                            hidl_request.releaseTemporaryStorage();
                            result = hidl_reply.readInt32();
                        } catch (RemoteException | NoSuchElementException e3) {
                            mExtDaemon = null;
                        }
                        hidl_reply.release();
                    } catch (Throwable th) {
                        hidl_reply.release();
                        throw th;
                    }
                }
                return result;
            case 2:
                synchronized (DAEMON_BINDER_LOCK) {
                    if (mDaemonBinder == null) {
                        try {
                            getExtDaemonAidl();
                        }catch (RemoteException e) {
                            Slog.e(TAG, "Failed when get miext aidl");
                        }
                    }
                    if (mDaemonBinder != null) {
                        Parcel parcel = Parcel.obtain();
                        Parcel reply = Parcel.obtain();
                        try {
                            try {
                                parcel.writeInterfaceToken(EXT_DESCRIPTOR_AIDL);
                                parcel.writeInt(cmd);
                                parcel.writeInt(param);
                                mDaemonBinder.transact(1, parcel, reply, 0);
                                reply.readException();
                                result = reply.readInt();
                                parcel.recycle();
                            } catch (Throwable th2) {
                                parcel.recycle();
                                reply.recycle();
                                throw th2;
                            }
                        } catch (RemoteException e5) {
                            Slog.e(TAG, "[JAVA] transact failed. " + e5);
                            parcel.recycle();
                        }
                        reply.recycle();
                    }
                }
                return result;
        }
    }


}
