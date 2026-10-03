package com.bhushantechsolutions.ninja.domain.ble

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface BleManager {
    val isScanning: StateFlow<Boolean>
    val discoveredDevices: StateFlow<List<BleDevice>>
    val connectionState: StateFlow<BleConnectionState>
    fun startScan()
    fun stopScan()
    fun connect(address: String)
    fun disconnect()
    fun discoverServices(): Flow<List<BleGattService>>
    fun readCharacteristic(serviceUuid: String, characteristicUuid: String): Flow<ByteArray>
    fun writeCharacteristic(serviceUuid: String, characteristicUuid: String, value: ByteArray): Boolean
    fun observeNotification(serviceUuid: String, characteristicUuid: String, enable: Boolean): Flow<ByteArray>
}
