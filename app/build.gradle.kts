plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "me.trdyun.lunar"
    compileSdk = 35

    defaultConfig {
        applicationId = "me.trdyun.lunar"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    compileOnly(files("libs\\color.jar"))
}