package me.trdyun.lunar;

//
// Created by trdyun on 2025/3/30.
// Copyright (c) 2025 CandyClub. All rights reserved.
//
public class LunarWrapper {
    public static void onFodCallback(int cmd, int param, String ownerPackage) {
        LunarService.getInstance().getFodService().onFrameworkCallback(cmd, param, ownerPackage);
    }
}
