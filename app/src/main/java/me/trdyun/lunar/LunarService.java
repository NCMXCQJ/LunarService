package me.trdyun.lunar;

//
// Created by trdyun on 2025/3/30.
// Copyright (c) 2025 CandyClub. All rights reserved.
//
public class LunarService {
    private static final LunarService INSTANCE = new LunarService();
    private final me.trdyun.lunar.modules.biometrics.fod.FodModule mFodService =
            me.trdyun.lunar.modules.biometrics.fod.FodModule.getInstance();

    public static LunarService getInstance() {
        return INSTANCE;
    }

    public me.trdyun.lunar.modules.biometrics.fod.FodModule getFodService() {
        return mFodService;
    }
}
