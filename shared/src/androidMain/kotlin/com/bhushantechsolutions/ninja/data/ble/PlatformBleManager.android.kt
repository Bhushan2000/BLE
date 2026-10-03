package com.bhushantechsolutions.ninja.data.ble

import android.content.Context
import com.bhushantechsolutions.ninja.domain.ble.BleManager

var androidApplicationContext: Context? = null

actual fun createPlatformBleManager(): BleManager {
    val ctx = androidApplicationContext ?: error("androidApplicationContext is not initialized.")
    return AndroidBleManager(ctx)
}
