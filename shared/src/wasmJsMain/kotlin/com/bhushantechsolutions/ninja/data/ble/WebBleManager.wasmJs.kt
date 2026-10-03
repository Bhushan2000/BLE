package com.bhushantechsolutions.ninja.data.ble

import com.bhushantechsolutions.ninja.data.mock.MockBleManager
import com.bhushantechsolutions.ninja.domain.ble.BleManager

actual fun createPlatformBleManager(): BleManager = MockBleManager()
