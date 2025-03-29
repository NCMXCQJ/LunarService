package me.trdyun.lunar.modules.biometrics.fod;

import android.os.HwBinder;
import android.os.HwParcel;
import android.os.IHwBinder;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Slog;

import java.util.ArrayList;

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

    private static IHwBinder mExtDaemon;

    static {
        initExtDaemon();
    }

    public static void fodCall(String sceneCode, int param) {
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

    private static int callExtCmd(int cmd, int param) {
        if (cmd == 0) return -1;
        if (mExtDaemon == null) initExtDaemon();
        if (mExtDaemon == null) return -1;
        try {
            HwParcel request = new HwParcel();
            request.writeInterfaceToken("vendor.xiaomi.hardware.fingerprintextension@1.0::IXiaomiFingerprint");
            request.writeInt32(cmd);
            request.writeInt32(param);
            HwParcel reply = new HwParcel();
            mExtDaemon.transact(1, request, reply, 0);
            reply.verifySuccess();
            request.releaseTemporaryStorage();
            return reply.readInt32();
        }catch (Exception e) {
            Slog.e(TAG, e.getMessage());
            return -1;
        }
    }

    private static void initExtDaemon() {
        try {
            mExtDaemon = HwBinder.getService("vendor.xiaomi.hardware.fingerprintextension@1.0::IXiaomiFingerprint", "default");
            mExtDaemon.linkToDeath(deathRecipient, 0);
            IS_LOADED = true;
        }catch (RemoteException e) {
            Log.e(TAG, "Xiaomi Fingerprint Service error");
        }
    }

    private static IHwBinder.DeathRecipient deathRecipient = new IHwBinder.DeathRecipient() {
        @Override
        public void serviceDied(long l) {
            Slog.e(TAG, "xiaomi ext daemon died");
            IS_LOADED = false;
            if (mExtDaemon == null) return;
            mExtDaemon.unlinkToDeath(this::serviceDied);
            mExtDaemon = null;
            initExtDaemon();
        }
    };

}
