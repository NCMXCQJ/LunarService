package me.trdyun.lunar.utils;

import android.os.SystemProperties;
import android.os.HwBinder;
import android.os.HwParcel;
import android.os.IBinder;
import android.os.IHwBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Slog;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

//
// Created by trdyun on 2025/3/30.
// Copyright (c) 2025 CandyClub. All rights reserved.
//
public class MiTouchFeature {
    private static final String AIDL_NAME_V1 = "vendor.xiaomi.hw.touchfeature.MiTouchFeature";
    private static final int AIDL_V1_GETMODEWHITELIST = 11;
    private static final int AIDL_V1_GET_MODE_CUR_VALUE = 1;
    private static final int AIDL_V1_GET_MODE_CUR_VALUE_STRING = 10;
    private static final int AIDL_V1_GET_MODE_DEF_VALUE = 2;
    private static final int AIDL_V1_GET_MODE_MAX_VALUE = 3;
    private static final int AIDL_V1_GET_MODE_MIN_VALUE = 4;
    private static final int AIDL_V1_GET_MODE_VALUES = 5;
    private static final int AIDL_V1_GET_TOUCH_EVENT = 6;
    private static final int AIDL_V1_MODE_RESET = 7;
    private static final int AIDL_V1_REGISTER = 12;
    private static final int AIDL_V1_SET_MODE_EDGE_VALUE = 8;
    private static final int AIDL_V1_SET_MODE_PACKAGENAME = 14;
    private static final int AIDL_V1_SET_MODE_VALUE = 9;
    private static final int AIDL_V1_UNREGISTER = 13;
    private static final String DEFAULT = "default";
    private static final int GET_MODE_CUR_VALUE = 2;
    private static final int GET_MODE_DEF_VALUE = 5;
    private static final int GET_MODE_MAX_VALUE = 3;
    private static final int GET_MODE_MIN_VALUE = 4;
    private static final int GET_MODE_VALUES = 7;
    private static final int GET_MODE_WHITELIST = 10;
    private static final int GET_TOUCH_EVENT = 9;
    private static volatile MiTouchFeature INSTANCE = null;
    private static final int MODE_RESET = 6;
    private static final int REGISTER = 11;
    private static final String SERVICE_AIDL_NAME_V1 = "vendor.xiaomi.hw.touchfeature.MiTouchFeature/default";
    private static final String SERVICE_NAME_V1 = "vendor.xiaomi.hardware.touchfeature@1.0::MiTouchFeature";
    private static final String SERVICE_NAME_V2 = "vendor.xiaomi.hw.touchfeature@1.0::MiTouchFeature";
    public static final int SERVICE_VERSION_CODE_NONE = 0;
    public static final int SERVICE_VERSION_CODE_V1 = 1;
    public static final int SERVICE_VERSION_CODE_V2 = 2;
    public static final int SERVICE_VERSION_CODE_V3 = 3;
    private static final int SET_MODE_EDGE_VALUE = 8;
    private static final int SET_MODE_PACKAGENAME = 13;
    private static final int SET_MODE_VALUE = 1;
    private static final String TAG = "MiTouchFeature";
    private static final int TOUCHFEATURE_DOUBLE_TAP = 1;
    private static final int TOUCHFEATURE_DRIVER_DEBUGLEVEL = 8;
    private static final int TOUCHFEATURE_EDGE_MODE = 4;
    private static final int TOUCHFEATURE_GLOBAL_TOUCH_DIRECTION = 2;
    private static final int TOUCHFEATURE_REMOVE_EDGE_SETTINGS = 64;
    public static final int TOUCH_ACTIVE_MODE = 1;
    public static final int TOUCH_DEBUG_LEVEL = 18;
    public static final int TOUCH_DOUBLETAP_MODE = 14;
    public static final int TOUCH_EDGE_FILTER = 7;
    public static final int TOUCH_EDGE_MODE = 15;
    public static final int TOUCH_GAMETUROTOOL_ALL = 10000;
    public static final int TOUCH_GAMETUROTOOL_FOLLOW_UP = 10001;
    public static final int TOUCH_GAMETUROTOOL_RESPONSE = 10002;
    public static final int TOUCH_GAMETUROTOOL_SHAKING = 10003;
    public static final int TOUCH_GAME_MODE = 0;
    public static final int TOUCH_ID_PRIMARY = 0;
    public static final int TOUCH_ID_SECONDARY = 1;
    public static final int TOUCH_MODE_DIRECTION = 8;
    public static final int TOUCH_PASSIVE_PEN_MODE = 23;
    public static final int TOUCH_PERFORMANCE_MODE = 21;
    public static final int TOUCH_STYLUS_HOPPING_MODE = 22;
    public static final int TOUCH_STYLUS_MODE = 20;
    public static final int TOUCH_STYLUS_QUICK_NOTE_MODE = 24;
    public static final int TOUCH_TOLERANCE = 3;
    public static final int TOUCH_TP_EDGE_MODE = 25;
    public static final int TOUCH_UP_THRESHOLD = 2;
    public static final int TOUCH_WGH_MAX = 5;
    public static final int TOUCH_WGH_MIN = 4;
    public static final int TOUCH_WGH_STEP = 6;
    public static final int Touch_GAMETURBOTOOL_PACKAGE = 10100;
    private static final int UNREGISTER = 12;
    private int mServiceVersion;
    private int mTouchFeatureProperties = SystemProperties.getInt("ro.vendor.touchfeature.type", 0);

    public static MiTouchFeature getInstance() {
        if (INSTANCE == null) {
            synchronized (MiTouchFeature.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MiTouchFeature();
                }
            }
        }
        return INSTANCE;
    }

    public int getSupportTouchFeatureVersion() {
        if (this.mServiceVersion != 0) {
            Slog.i(TAG, "current device and process support version:" + this.mServiceVersion);
            return this.mServiceVersion;
        }
        IBinder binder = ServiceManager.getService(SERVICE_AIDL_NAME_V1);
        if (binder == null) {
            try {
                IHwBinder hwService2 = HwBinder.getService(SERVICE_NAME_V2, "default");
                if (hwService2 != null) {
                    Slog.i(TAG, "current device and process support v2 service");
                    return 2;
                }
            } catch (RemoteException e2) {
                e2.printStackTrace();
            } catch (NoSuchElementException e3) {
                Slog.e(TAG, e3.toString());
            }
            try {
                IHwBinder hwService1 = HwBinder.getService(SERVICE_NAME_V1, "default");
                if (hwService1 != null) {
                    Slog.i(TAG, "current device and process support v1 service");
                    return 1;
                }
            } catch (RemoteException e4) {
                e4.printStackTrace();
            } catch (NoSuchElementException e5) {
                Slog.e(TAG, e5.toString());
            }
            Slog.e(TAG, "current device and process not support, v1/v2 HIDL service and v1 AIDL service not found");
            return 0;
        }
        Slog.i(TAG, "current device and process support AIDL v1 service");
        return 3;
    }

    private MiTouchFeature() {
        this.mServiceVersion = 0;
        this.mServiceVersion = getSupportTouchFeatureVersion();
    }

    public boolean setTouchMode(int mode, int value) {
        HwParcel hidl_reply = new HwParcel();
        try {
            try {
                IHwBinder hwService = HwBinder.getService(SERVICE_NAME_V1, "default");
                if (hwService != null) {
                    HwParcel hidl_request = new HwParcel();
                    hidl_request.writeInterfaceToken(SERVICE_NAME_V1);
                    hidl_request.writeInt32(mode);
                    hidl_request.writeInt32(value);
                    hwService.transact(1, hidl_request, hidl_reply, 0);
                    hidl_reply.verifySuccess();
                    hidl_request.releaseTemporaryStorage();
                    int val = hidl_reply.readInt32();
                    if (val == 0) {
                        return true;
                    }
                    Slog.e(TAG, "setTouchMode failed. ret = " + val);
                    return false;
                }
            } catch (RemoteException e2) {
                Slog.e(TAG, "transact failed. " + e2);
            } catch (NoSuchElementException e3) {
                Slog.e(TAG, e3.toString());
            }
            Slog.e(TAG, "setTouchMode failed.");
            return false;
        } finally {
            hidl_reply.release();
        }
    }

    public boolean setTouchMode(int touchId, int mode, int value) {
        switch (getSupportTouchFeatureVersion()) {
            case 0:
                return false;
            case 1:
                return setTouchMode(mode, value);
            case 2:
                HwParcel hidl_reply = new HwParcel();
                try {
                    try {
                        IHwBinder hwService = HwBinder.getService(SERVICE_NAME_V2, "default");
                        if (hwService != null) {
                            HwParcel hidl_request = new HwParcel();
                            hidl_request.writeInterfaceToken(SERVICE_NAME_V2);
                            hidl_request.writeInt32(touchId);
                            hidl_request.writeInt32(mode);
                            hidl_request.writeInt32(value);
                            hwService.transact(1, hidl_request, hidl_reply, 0);
                            hidl_reply.verifySuccess();
                            hidl_request.releaseTemporaryStorage();
                            int val = hidl_reply.readInt32();
                            if (val == 0) {
                                return true;
                            }
                            Slog.e(TAG, "setTouchMode failed. ret = " + val);
                            return false;
                        }
                    } catch (RemoteException e2) {
                        Slog.e(TAG, "transact failed. " + e2);
                    } catch (NoSuchElementException e3) {
                        Slog.e(TAG, e3.toString());
                    }
                    Slog.e(TAG, "setTouchMode failed.");
                    return false;
                } finally {
                    hidl_reply.release();
                }
            case 3:
                Parcel parcel = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    try {
                        IBinder binder = ServiceManager.getService(SERVICE_AIDL_NAME_V1);
                        if (binder == null) {
                            Slog.e(TAG, "[JAVA] can't get vendor.xiaomi.hw.touchfeature.MiTouchFeature/default service!");
                            return false;
                        }
                        parcel.writeInterfaceToken(AIDL_NAME_V1);
                        parcel.writeInt(touchId);
                        parcel.writeInt(mode);
                        parcel.writeInt(value);
                        binder.transact(9, parcel, reply, 0);
                        reply.readException();
                        int val_v3 = reply.readInt();
                        if (val_v3 == 0) {
                            return true;
                        }
                        Slog.e(TAG, "[JAVA] setTouchMode failed. ret = " + val_v3);
                        return false;
                    } catch (RemoteException e4) {
                        Slog.e(TAG, "[JAVA] transact failed. " + e4);
                        parcel.recycle();
                        reply.recycle();
                        Slog.e(TAG, "[JAVA] setTouchMode failed.");
                        return false;
                    }
                } finally {
                    parcel.recycle();
                    reply.recycle();
                }
            default:
                return false;
        }
    }

}
