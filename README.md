# LunarService FOD HBM Bridge

This module re-implements Xiaomi FOD HBM signaling from the framework `fodCallBack`
path and routes it through LunarService.

## Service API

- `LunarWrapper.onFodCallback(int cmd, int param, String ownerPackage)`
  - delegates to `FodModule.onFrameworkCallback(int cmd, int param, String ownerPackage)`
  - `ownerPackage` is ignored and treated as `com.android.systemui` for highest privilege

## Framework -> Service Mapping

| Framework class#method | cmd | param | Service call |
| --- | --- | --- | --- |
| `FingerprintAuthenticationClient.startHalOperation` | `1` (CMD_APP_AUTHEN) | `0` | `LunarWrapper.onFodCallback(1, 0, owner)` |
| `FingerprintAuthenticationClient.stopHalOperation` | `2` (CMD_APP_CANCEL_AUTHEN) | `0` | `LunarWrapper.onFodCallback(2, 0, owner)` |
| `FingerprintAuthenticationClient.onAuthenticated` | `3` (CMD_VENDOR_AUTHENTICATED) | `biometricId` | `LunarWrapper.onFodCallback(3, biometricId, owner)` |
| `AidlResponseHandler.onError` | `4` (CMD_VENDOR_ERROR) | `error` | `LunarWrapper.onFodCallback(4, error, owner)` |
| `FingerprintAuthenticationClient.onLockoutTimed` | `5` (CMD_FW_LOCK_CANCEL) | `1` | `LunarWrapper.onFodCallback(5, 1, owner)` |
| `FingerprintAuthenticationClient.onLockoutPermanent` | `5` (CMD_FW_LOCK_CANCEL) | `2` | `LunarWrapper.onFodCallback(5, 2, owner)` |
| `FingerprintProvider.BiometricTaskStackListener.onTaskStackChanged` | `6` (CMD_FW_TOP_APP_CANCEL) | `0` | `LunarWrapper.onFodCallback(6, 0, owner)` |
| `FingerprintEnrollClient.startHalOperation` | `7` (CMD_APP_ENROLL) | `0` | `LunarWrapper.onFodCallback(7, 0, owner)` |
| `FingerprintEnrollClient.stopHalOperation` | `8` (CMD_APP_CANCEL_ENROLL) | `0` | `LunarWrapper.onFodCallback(8, 0, owner)` |
| `FingerprintEnrollClient.onEnrollResult` | `9` (CMD_VENDOR_ENROLL_RES) | `remaining` | `LunarWrapper.onFodCallback(9, remaining, owner)` |
| `AidlResponseHandler.onEnrollmentRemoved` | `11` (CMD_VENDOR_REMOVED) | `0` | `LunarWrapper.onFodCallback(11, 0, owner)` |
| `FingerprintDetectClient.startHalOperation` | `12` (CMD_KEYGUARD_DETECT) | `0` | `LunarWrapper.onFodCallback(12, 0, owner)` |
| `FingerprintDetectClient.stopHalOperation` | `13` (CMD_KEYGUARD_CANCEL_DETECT) | `0` | `LunarWrapper.onFodCallback(13, 0, owner)` |

## Internal Transport Interfaces

- `FodModule.ExtCmdTransport` abstracts HIDL/AIDL extcmd (`vendor.xiaomi.hardware.fingerprintextension`).
  - Implementations: `AidlExtCmdTransport`, `HidlExtCmdTransport`.
  - `ExtCmdDispatcher` provides fallback between transports and a small retry queue.
- `FodModule.TouchFeatureTransport` abstracts touchfeature (`vendor.xiaomi.hw.touchfeature`).
  - Implementation: `MiTouchFeatureTransport` (wraps `MiTouchFeature`).
- `SurfaceFlingerTransport` follows MIUI behavior (used when `ro.display.move_frame_rate_strategy_up=false`) and also acts as a backup path.

## Notes

- Heart-rate related scenes are intentionally not implemented.
