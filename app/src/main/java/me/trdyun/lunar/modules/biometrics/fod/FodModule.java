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

import java.util.ArrayDeque;
import java.util.NoSuchElementException;

import me.trdyun.lunar.utils.MiTouchFeature;

//
// Created by trdyun on 2025/3/30.
// Copyright (c) 2025 CandyClub. All rights reserved.
//
public final class FodModule {
    public static final String TAG = "LunarFodService";

    public static final int CMD_APP_AUTHEN = 1;
    public static final int CMD_APP_CANCEL_AUTHEN = 2;
    public static final int CMD_VENDOR_AUTHENTICATED = 3;
    public static final int CMD_VENDOR_ERROR = 4;
    public static final int CMD_FW_LOCK_CANCEL = 5;
    public static final int CMD_FW_TOP_APP_CANCEL = 6;
    public static final int CMD_APP_ENROLL = 7;
    public static final int CMD_APP_CANCEL_ENROLL = 8;
    public static final int CMD_VENDOR_ENROLL_RES = 9;
    public static final int CMD_VENDOR_REMOVED = 11;
    public static final int CMD_KEYGUARD_DETECT = 12;
    public static final int CMD_KEYGUARD_CANCEL_DETECT = 13;

    private static final int SF_FINGERPRINT_NONE = 0;
    private static final int SF_ENROLL_START = 1;
    private static final int SF_ENROLL_STOP = 2;
    private static final int SF_AUTH_START = 3;
    private static final int SF_AUTH_STOP = 4;
    private static final int SF_HEART_RATE_START = 5;
    private static final int SF_HEART_RATE_STOP = 6;
    private static final int SF_KEYGUARD_DETECT_START = 7;
    private static final int SF_KEYGUARD_DETECT_STOP = 8;

    private static final int TOUCH_ID = 0;
    private static final int TOUCH_MODE = 10;
    private static final int TOUCH_ON = 1;
    private static final int TOUCH_OFF = 0;

    private static final int EXT_CMD_NOTIFY_MONITOR_STATE = 4;
    private static final int EXT_CMD_NOTIFY_LOCKOUT = 5;
    private static final int EXT_CMD_NOTIFY_LOW_BRIGHTNESS_ALLOW = 7;
    private static final int EXT_CMD_NOTIFY_CLIENT = 18;

    private static final int AUTH_CLIENT_SYSTEMUI = 1;
    private static final int AUTH_CLIENT_SECURITY = 2;
    private static final int AUTH_CLIENT_PAY = 3;
    private static final int AUTH_CLIENT_OTHER = 4;

    private static final String PACKAGE_SYSTEMUI = "com.android.systemui";
    private static final String PACKAGE_SECURITY = "com.miui.securitycenter";
    private static final String PACKAGE_TENCENT = "com.tencent.mm";
    private static final String PACKAGE_ALIPAY = "com.eg.android.AlipayGphone";

    private static final FodModule INSTANCE = new FodModule();

    public static FodModule getInstance() {
        return INSTANCE;
    }

    private final boolean mIsFod;
    private final boolean mIsFodEngineEnabled;
    private boolean mEnabled = true;

    private final ExtCmdDispatcher mExtCmd;
    private final TouchFeatureTransport mTouchFeature;
    private final SurfaceFlingerTransport mSurfaceFlinger;

    private int mLastTouchCmd = Integer.MIN_VALUE;
    private int mLastSfCmd = Integer.MIN_VALUE;
    private int mLastSfPackageType = Integer.MIN_VALUE;

    private FodModule() {
        mIsFod = SystemProperties.getBoolean("ro.hardware.fp.fod", false);
        mIsFodEngineEnabled =
                "2.0".equals(SystemProperties.get("ro.hardware.fp.fod.touch.ctl.version", ""));
        mExtCmd = new ExtCmdDispatcher(new AidlExtCmdTransport(), new HidlExtCmdTransport());
        mTouchFeature = new MiTouchFeatureTransport();
        mSurfaceFlinger = new SurfaceFlingerTransport();
    }

    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }

    public void onFrameworkCallback(int cmd, int param, String ownerPackage) {
        if (!mEnabled) {
            return;
        }
        if (!mIsFod) {
            Log.d(TAG, "Not a FOD device, skip");
            return;
        }
        String owner = PACKAGE_SYSTEMUI;
        boolean isKeyguard = PACKAGE_SYSTEMUI.equals(owner);
        int touchCmd = mapTouchCmd(cmd, param);
        int sfCmd = mapSfCmd(cmd, param);

        if (!mIsFodEngineEnabled) {
            applyTouch(touchCmd);
            applySurfaceFlinger(sfCmd, isKeyguard ? 1 : 0);
            return;
        }

        boolean allowSurfaceFlinger =
                !SystemProperties.getBoolean("ro.display.move_frame_rate_strategy_up", false);

        if (cmd == CMD_KEYGUARD_DETECT) {
            mExtCmd.send(EXT_CMD_NOTIFY_LOCKOUT, 2);
        } else if (cmd == CMD_KEYGUARD_CANCEL_DETECT) {
            mExtCmd.send(EXT_CMD_NOTIFY_LOCKOUT, 0);
        }

        if (sfCmd != -1) {
            mExtCmd.send(EXT_CMD_NOTIFY_MONITOR_STATE, sfCmd);
            if (allowSurfaceFlinger) {
                applySurfaceFlinger(sfCmd, isKeyguard ? 1 : 0);
            }
            int keyguardFlag = (cmd == CMD_APP_AUTHEN && isKeyguard) ? 1 : 0;
            mExtCmd.send(EXT_CMD_NOTIFY_LOW_BRIGHTNESS_ALLOW, keyguardFlag);
        }

        if (cmd == CMD_APP_AUTHEN) {
            int authClient = mapAuthClient(owner);
            mExtCmd.send(EXT_CMD_NOTIFY_CLIENT, authClient);
        }
    }

    public void onFrameworkCallback(int cmd, int param) {
        onFrameworkCallback(cmd, param, "");
    }

    public void onScene(String sceneCode, int param, String ownerPackage) {
        FodScene scene = FodScene.getSceneByCode(sceneCode);
        int cmd = mapSceneToCmd(scene);
        if (cmd == -1) {
            Log.w(TAG, "Unknown scene code: " + sceneCode);
            return;
        }
        onFrameworkCallback(cmd, param, ownerPackage);
    }

    private int mapSceneToCmd(FodScene scene) {
        switch (scene) {
            case CMD_APP_ENROLL:
                return CMD_APP_ENROLL;
            case CMD_APP_CANCEL_ENROLL:
                return CMD_APP_CANCEL_ENROLL;
            case CMD_VENDOR_ENROLL_RES:
                return CMD_VENDOR_ENROLL_RES;
            case CMD_KEYGUARD_DETECT:
                return CMD_KEYGUARD_DETECT;
            case CMD_KEYGUARD_CANCEL_DETECT:
                return CMD_KEYGUARD_CANCEL_DETECT;
            case CMD_APP_AUTHEN:
                return CMD_APP_AUTHEN;
            case CMD_APP_CANCEL_AUTHEN:
                return CMD_APP_CANCEL_AUTHEN;
            case CMD_VENDOR_AUTHENTICATED:
                return CMD_VENDOR_AUTHENTICATED;
            case CMD_FW_LOCK_CANCEL:
                return CMD_FW_LOCK_CANCEL;
            case CMD_VENDOR_ERROR:
                return CMD_VENDOR_ERROR;
            case CMD_VENDOR_REMOVED:
                return CMD_VENDOR_REMOVED;
            case CMD_FW_TOP_APP_CANCEL:
                return CMD_FW_TOP_APP_CANCEL;
            case UNKNOWN:
            default:
                return -1;
        }
    }

    private int mapTouchCmd(int cmd, int param) {
        switch (cmd) {
            case CMD_APP_ENROLL:
            case CMD_APP_AUTHEN:
            case CMD_KEYGUARD_DETECT:
                return TOUCH_ON;
            case CMD_APP_CANCEL_AUTHEN:
            case CMD_APP_CANCEL_ENROLL:
            case CMD_KEYGUARD_CANCEL_DETECT:
            case CMD_FW_LOCK_CANCEL:
            case CMD_VENDOR_ERROR:
            case CMD_VENDOR_REMOVED:
            case CMD_FW_TOP_APP_CANCEL:
                return TOUCH_OFF;
            case CMD_VENDOR_ENROLL_RES:
                return param == 0 ? TOUCH_OFF : -1;
            case CMD_VENDOR_AUTHENTICATED:
                return param != 0 ? TOUCH_OFF : -1;
            default:
                return -1;
        }
    }

    private int mapSfCmd(int cmd, int param) {
        switch (cmd) {
            case CMD_APP_ENROLL:
                return SF_ENROLL_START;
            case CMD_APP_CANCEL_ENROLL:
                return SF_ENROLL_STOP;
            case CMD_VENDOR_ENROLL_RES:
                return param == 0 ? SF_ENROLL_STOP : SF_ENROLL_START;
            case CMD_KEYGUARD_DETECT:
                return SF_KEYGUARD_DETECT_START;
            case CMD_KEYGUARD_CANCEL_DETECT:
                return SF_KEYGUARD_DETECT_STOP;
            case CMD_APP_AUTHEN:
                return SF_AUTH_START;
            case CMD_APP_CANCEL_AUTHEN:
            case CMD_VENDOR_ERROR:
            case CMD_FW_TOP_APP_CANCEL:
                return SF_AUTH_STOP;
            case CMD_VENDOR_AUTHENTICATED:
                return param == 0 ? SF_AUTH_START : SF_AUTH_STOP;
            case CMD_FW_LOCK_CANCEL:
                return SF_FINGERPRINT_NONE;
            default:
                return -1;
        }
    }

    private int mapAuthClient(String ownerPackage) {
        if (PACKAGE_SYSTEMUI.equals(ownerPackage)) {
            return AUTH_CLIENT_SYSTEMUI;
        }
        if (PACKAGE_SECURITY.equals(ownerPackage)) {
            return AUTH_CLIENT_SECURITY;
        }
        if (PACKAGE_TENCENT.equals(ownerPackage) || PACKAGE_ALIPAY.equals(ownerPackage)) {
            return AUTH_CLIENT_PAY;
        }
        return AUTH_CLIENT_OTHER;
    }

    private void applyTouch(int touchCmd) {
        if (touchCmd == -1 || touchCmd == mLastTouchCmd) {
            return;
        }
        if (mTouchFeature.setTouchMode(TOUCH_ID, TOUCH_MODE, touchCmd)) {
            mLastTouchCmd = touchCmd;
        }
    }

    private void applySurfaceFlinger(int sfCmd, int packageType) {
        if (sfCmd == -1) {
            return;
        }
        if (sfCmd == mLastSfCmd && packageType == mLastSfPackageType) {
            return;
        }
        int result = mSurfaceFlinger.send(sfCmd, packageType);
        if (result >= 0) {
            mLastSfCmd = sfCmd;
            mLastSfPackageType = packageType;
        }
    }

    private interface ExtCmdTransport {
        boolean isAvailable();

        int send(int cmd, int param);
    }

    private static final class ExtCmdDispatcher {
        private final ExtCmdTransport mPrimary;
        private final ExtCmdTransport mSecondary;
        private final ArrayDeque<ExtCmdRequest> mPending = new ArrayDeque<>();
        private ExtCmdTransport mLastGood;

        private ExtCmdDispatcher(ExtCmdTransport primary, ExtCmdTransport secondary) {
            mPrimary = primary;
            mSecondary = secondary;
        }

        int send(int cmd, int param) {
            if (cmd == 0) {
                return -1;
            }
            ExtCmdTransport primary = selectPrimary();
            ExtCmdTransport secondary = selectSecondary(primary);
            int result = trySend(primary, cmd, param);
            if (result < 0) {
                result = trySend(secondary, cmd, param);
            }
            if (result < 0) {
                enqueue(cmd, param);
            }
            return result;
        }

        private ExtCmdTransport selectPrimary() {
            if (mLastGood != null && mLastGood.isAvailable()) {
                return mLastGood;
            }
            if (mPrimary.isAvailable()) {
                return mPrimary;
            }
            if (mSecondary.isAvailable()) {
                return mSecondary;
            }
            return null;
        }

        private ExtCmdTransport selectSecondary(ExtCmdTransport primary) {
            if (primary == null) {
                return null;
            }
            return primary == mPrimary ? mSecondary : mPrimary;
        }

        private int trySend(ExtCmdTransport transport, int cmd, int param) {
            if (transport == null || !transport.isAvailable()) {
                return -1;
            }
            int result = transport.send(cmd, param);
            if (result >= 0) {
                mLastGood = transport;
                flushPending(transport);
            }
            return result;
        }

        private void enqueue(int cmd, int param) {
            if (mPending.size() >= 8) {
                mPending.removeFirst();
            }
            mPending.addLast(new ExtCmdRequest(cmd, param));
        }

        private void flushPending(ExtCmdTransport transport) {
            if (mPending.isEmpty()) {
                return;
            }
            ArrayDeque<ExtCmdRequest> remaining = new ArrayDeque<>();
            while (!mPending.isEmpty()) {
                ExtCmdRequest req = mPending.removeFirst();
                int result = transport.send(req.cmd, req.param);
                if (result < 0) {
                    remaining.addLast(req);
                    while (!mPending.isEmpty()) {
                        remaining.addLast(mPending.removeFirst());
                    }
                }
            }
            mPending.clear();
            mPending.addAll(remaining);
        }
    }

    private static final class ExtCmdRequest {
        private final int cmd;
        private final int param;

        private ExtCmdRequest(int cmd, int param) {
            this.cmd = cmd;
            this.param = param;
        }
    }

    private static final class HidlExtCmdTransport implements ExtCmdTransport {
        private static final String NAME_EXT_DAEMON =
                "vendor.xiaomi.hardware.fingerprintextension@1.0::IXiaomiFingerprint";
        private static final String EXT_DESCRIPTOR =
                "vendor.xiaomi.hardware.fingerprintextension@1.0::IXiaomiFingerprint";

        private IHwBinder mService;
        private final IHwBinder.DeathRecipient mDeathRecipient = cookie -> {
            synchronized (this) {
                mService = null;
            }
        };

        @Override
        public boolean isAvailable() {
            return ensureService();
        }

        @Override
        public int send(int cmd, int param) {
            if (!ensureService()) {
                return -1;
            }
            HwParcel reply = new HwParcel();
            try {
                HwParcel request = new HwParcel();
                request.writeInterfaceToken(EXT_DESCRIPTOR);
                request.writeInt32(cmd);
                request.writeInt32(param);
                mService.transact(1, request, reply, 0);
                reply.verifySuccess();
                request.releaseTemporaryStorage();
                return reply.readInt32();
            } catch (RemoteException | NoSuchElementException e) {
                synchronized (this) {
                    mService = null;
                }
                return -1;
            } finally {
                reply.release();
            }
        }

        private boolean ensureService() {
            if (mService != null) {
                return true;
            }
            try {
                IHwBinder service = HwBinder.getService(NAME_EXT_DAEMON, "default");
                if (service == null) {
                    return false;
                }
                service.linkToDeath(mDeathRecipient, 0L);
                mService = service;
                return true;
            } catch (RemoteException | NoSuchElementException e) {
                return false;
            }
        }
    }

    private static final class AidlExtCmdTransport implements ExtCmdTransport {
        private static final String NAME_EXT_DAEMON_AIDL =
                "vendor.xiaomi.hardware.fingerprintextension.IXiaomiFingerprint/default";
        private static final String EXT_DESCRIPTOR_AIDL =
                "vendor.xiaomi.hardware.fingerprintextension.IXiaomiFingerprint";

        private final Object mLock = new Object();
        private IBinder mService;

        private final IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() {
            @Override
            public void binderDied() {
                synchronized (mLock) {
                    if (mService != null) {
                        mService.unlinkToDeath(mDeathRecipient, 0);
                        mService = null;
                    }
                }
            }
        };

        @Override
        public boolean isAvailable() {
            synchronized (mLock) {
                return ensureServiceLocked();
            }
        }

        @Override
        public int send(int cmd, int param) {
            synchronized (mLock) {
                if (!ensureServiceLocked()) {
                    return -1;
                }
                Parcel parcel = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    parcel.writeInterfaceToken(EXT_DESCRIPTOR_AIDL);
                    parcel.writeInt(cmd);
                    parcel.writeInt(param);
                    mService.transact(1, parcel, reply, 0);
                    reply.readException();
                    return reply.readInt();
                } catch (RemoteException e) {
                    mService = null;
                    return -1;
                } finally {
                    parcel.recycle();
                    reply.recycle();
                }
            }
        }

        private boolean ensureServiceLocked() {
            if (mService != null) {
                return true;
            }
            IBinder service = ServiceManager.getService(NAME_EXT_DAEMON_AIDL);
            if (service == null) {
                return false;
            }
            try {
                service.linkToDeath(mDeathRecipient, 0);
            } catch (RemoteException e) {
                return false;
            }
            mService = service;
            return true;
        }
    }

    private interface TouchFeatureTransport {
        boolean setTouchMode(int touchId, int mode, int value);
    }

    private static final class MiTouchFeatureTransport implements TouchFeatureTransport {
        private final MiTouchFeature mTouchFeature = MiTouchFeature.getInstance();

        @Override
        public boolean setTouchMode(int touchId, int mode, int value) {
            return mTouchFeature.setTouchMode(touchId, mode, value);
        }
    }

    private static final class SurfaceFlingerTransport {
        private static final String SURFACEFLINGER_SERVICE = "SurfaceFlinger";
        private static final String SURFACEFLINGER_DESCRIPTOR = "android.ui.ISurfaceComposer";
        private static final int CMD_NOTIFY_TO_SURFACEFLINGER = 31111;

        int send(int value, int packageType) {
            IBinder flinger = ServiceManager.getService(SURFACEFLINGER_SERVICE);
            if (flinger == null) {
                return -1;
            }
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            try {
                data.writeInterfaceToken(SURFACEFLINGER_DESCRIPTOR);
                data.writeInt(value);
                data.writeInt(packageType);
                flinger.transact(CMD_NOTIFY_TO_SURFACEFLINGER, data, reply, 0);
                reply.readException();
                return reply.readInt();
            } catch (RemoteException e) {
                return -1;
            } finally {
                data.recycle();
                reply.recycle();
            }
        }
    }
}
