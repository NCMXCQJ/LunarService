package me.trdyun.lunar.modules.biometrics.fod;

import java.util.Objects;

//
// Created by trdyun on 2025/3/30.
// Copyright (c) 2025 CandyClub. All rights reserved.
//
public enum FodScene {
    CMD_APP_ENROLL("ecstth"),
    CMD_APP_CANCEL_ENROLL("ecstph"),
    CMD_VENDOR_ENROLL_RES("ecoer"),
    CMD_KEYGUARD_DETECT("dcstth"),
    CMD_KEYGUARD_CANCEL_DETECT("dcstph"),
    CMD_APP_AUTHEN("acstth"),
    CMD_APP_CANCEL_AUTHEN("acstph"),
    CMD_VENDOR_AUTHENTICATED("acoatd"),
    CMD_FW_LOCK_CANCEL("achfatt"),
    CMD_VENDOR_ERROR("f21hrcoe"),
    CMD_VENDOR_REMOVED("f21hrcor"),
    CMD_FW_TOP_APP_CANCEL("f21btslotsc"),
    UNKNOWN("unknown");
    public String sceneCode;
    FodScene(String sceneCode) {
        this.sceneCode = sceneCode;
    }

    public static FodScene getSceneByCode(String code) {
        for (FodScene scene : FodScene.values()) {
            if (Objects.equals(scene.sceneCode, code)) return scene;
        }
        return UNKNOWN;
    }
}
