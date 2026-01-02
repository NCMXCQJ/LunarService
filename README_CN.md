# LunarService FOD HBM 桥接说明

该模块用于复刻 MIUI 框架 `fodCallBack` 路径的 HBM 通信逻辑，并通过 LunarService 转发。

## 服务接口

- `LunarWrapper.onFodCallback(int cmd, int param, String ownerPackage)`
  - 实际调用 `FodModule.onFrameworkCallback(int cmd, int param, String ownerPackage)`
  - `ownerPackage` 会被忽略并强制视为 `com.android.systemui`（最高权限）

## Framework -> 服务映射

| Framework 类#方法 | cmd | param | 服务调用 |
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

## 内部通信接口

- `FodModule.ExtCmdTransport` 抽象 extcmd（`vendor.xiaomi.hardware.fingerprintextension`）。
  - 实现：`AidlExtCmdTransport`、`HidlExtCmdTransport`。
  - `ExtCmdDispatcher` 自动切换并提供失败重试队列（灾难备份）。
- `FodModule.TouchFeatureTransport` 抽象 touchfeature（`vendor.xiaomi.hw.touchfeature`）。
  - 实现：`MiTouchFeatureTransport`（封装 `MiTouchFeature`）。
- `SurfaceFlingerTransport` 参考 MIUI 行为（`ro.display.move_frame_rate_strategy_up=false` 时使用），同时作为备用路径。

## 说明

- 心率相关场景暂不实现。
