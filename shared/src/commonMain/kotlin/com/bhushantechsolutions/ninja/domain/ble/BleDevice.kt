package com.bhushantechsolutions.ninja.domain.ble

data class BleDevice(
    val address: String,
    val name: String?,
    val rssi: Int,
    val isConnected: Boolean = false
)

data class BleGattService(
    val uuid: String,
    val name: String = BleGattResolver.resolveServiceName(uuid),
    val shortUuid: String = BleGattResolver.getShortUuid(uuid),
    val characteristics: List<BleGattCharacteristic>
)

data class BleGattCharacteristic(
    val uuid: String,
    val name: String = BleGattResolver.resolveCharacteristicName(uuid),
    val shortUuid: String = BleGattResolver.getShortUuid(uuid),
    val properties: Int,
    val isReadable: Boolean,
    val isWritable: Boolean,
    val isNotifiable: Boolean
)

sealed class BleConnectionState {
    object Disconnected : BleConnectionState()
    data class Connecting(val address: String) : BleConnectionState()
    data class Connected(val address: String, val deviceName: String?) : BleConnectionState()
    data class Error(val message: String) : BleConnectionState()
}
