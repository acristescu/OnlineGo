package io.zenandroid.onlinego.ui.screens.face2face

import android.os.Build

internal fun isProbablyAndroidEmulator(): Boolean {
  return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
    || Build.FINGERPRINT.startsWith("generic")
    || Build.FINGERPRINT.startsWith("unknown")
    || Build.HARDWARE.contains("goldfish")
    || Build.HARDWARE.contains("ranchu")
    || Build.MODEL.contains("google_sdk")
    || Build.MODEL.contains("Emulator")
    || Build.MODEL.contains("Android SDK built for x86")
    || Build.MANUFACTURER.contains("Genymotion")
    || Build.PRODUCT.contains("sdk_google")
    || Build.PRODUCT.contains("google_sdk")
    || Build.PRODUCT.contains("sdk")
    || Build.PRODUCT.contains("sdk_x86")
    || Build.PRODUCT.contains("sdk_gphone64_arm64")
    || Build.PRODUCT.contains("vbox86p")
    || Build.PRODUCT.contains("emulator")
    || Build.PRODUCT.contains("simulator"))
}
