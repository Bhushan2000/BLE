package com.bhushantechsolutions.ninja.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bhushantechsolutions.ninja.domain.ble.BleConnectionState
import com.bhushantechsolutions.ninja.domain.ble.BleDevice
import com.bhushantechsolutions.ninja.domain.ble.BleGattService
import com.bhushantechsolutions.ninja.domain.ble.BleManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConnectivityViewModel(
    private val bleManager: BleManager
) : ViewModel() {

    val isBleScanning: StateFlow<Boolean> = bleManager.isScanning
    val bleDevices: StateFlow<List<BleDevice>> = bleManager.discoveredDevices
    val bleConnectionState: StateFlow<BleConnectionState> = bleManager.connectionState

    private val _discoveredServices = MutableStateFlow<List<BleGattService>>(emptyList())
    val discoveredServices: StateFlow<List<BleGattService>> = _discoveredServices.asStateFlow()

    private val _activeReadValue = MutableStateFlow<String>("")
    val activeReadValue: StateFlow<String> = _activeReadValue.asStateFlow()

    private val _gattPopupDialogMessage = MutableStateFlow<String?>(null)
    val gattPopupDialogMessage: StateFlow<String?> = _gattPopupDialogMessage.asStateFlow()

    private val _notificationStreamMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val notificationStreamMap: StateFlow<Map<String, String>> = _notificationStreamMap.asStateFlow()

    private val _activeNotifyingCharUuids = MutableStateFlow<Set<String>>(emptySet())
    val activeNotifyingCharUuids: StateFlow<Set<String>> = _activeNotifyingCharUuids.asStateFlow()

    private val notificationJobs = mutableMapOf<String, Job>()

    fun toggleBleScan() {
        if (isBleScanning.value) {
            bleManager.stopScan()
        } else {
            bleManager.startScan()
        }
    }

    fun connectBleDevice(device: BleDevice) {
        bleManager.connect(device.address)
        _discoveredServices.value = emptyList()
        _activeReadValue.value = ""
        _gattPopupDialogMessage.value = null
    }

    fun disconnectBle() {
        notificationJobs.values.forEach { it.cancel() }
        notificationJobs.clear()
        _activeNotifyingCharUuids.value = emptySet()
        _notificationStreamMap.value = emptyMap()
        _discoveredServices.value = emptyList()
        _activeReadValue.value = ""
        _gattPopupDialogMessage.value = null
        bleManager.disconnect()
    }

    fun dismissGattPopup() {
        _gattPopupDialogMessage.value = null
    }

    fun discoverServices() {
        viewModelScope.launch {
            bleManager.discoverServices().collect { services ->
                _discoveredServices.value = services
            }
        }
    }

    fun readCharacteristic(serviceUuid: String, characteristicUuid: String) {
        _activeReadValue.value = "Reading characteristic..."
        viewModelScope.launch {
            bleManager.readCharacteristic(serviceUuid, characteristicUuid).collect { bytes ->
                val resultText = if (bytes.isNotEmpty()) {
                    val str = bytes.decodeToString()
                    val hex = bytes.joinToString(" ") { (it.toInt() and 0xFF).toString(16).padStart(2, '0').uppercase() }
                    "Read Result: '$str'\nHex Bytes: [$hex]"
                } else {
                    "Read completed (Empty value or GATT status error)"
                }
                _activeReadValue.value = resultText
                _gattPopupDialogMessage.value = "Characteristic: ${characteristicUuid.take(8)}...\n\n$resultText"
            }
        }
    }

    fun writeCharacteristic(serviceUuid: String, characteristicUuid: String, textValue: String): Boolean {
        val bytes = textValue.encodeToByteArray()
        val success = bleManager.writeCharacteristic(serviceUuid, characteristicUuid, bytes)
        val resultText = if (success) {
            "Write Sent Successfully: '$textValue' (${bytes.size} bytes)"
        } else {
            "Write Failed: Check device write permissions"
        }
        _activeReadValue.value = resultText
        _gattPopupDialogMessage.value = "Characteristic: ${characteristicUuid.take(8)}...\n\n$resultText"
        return success
    }

    fun toggleNotification(serviceUuid: String, characteristicUuid: String) {
        val currentSet = _activeNotifyingCharUuids.value.toMutableSet()

        if (currentSet.contains(characteristicUuid)) {
            // Unsubscribe this specific characteristic
            notificationJobs[characteristicUuid]?.cancel()
            notificationJobs.remove(characteristicUuid)
            currentSet.remove(characteristicUuid)
            _activeNotifyingCharUuids.value = currentSet

            val currentMap = _notificationStreamMap.value.toMutableMap()
            currentMap.remove(characteristicUuid)
            _notificationStreamMap.value = currentMap
        } else {
            // Subscribe to this characteristic
            currentSet.add(characteristicUuid)
            _activeNotifyingCharUuids.value = currentSet

            val currentMap = _notificationStreamMap.value.toMutableMap()
            currentMap[characteristicUuid] = "Listening for notifications..."
            _notificationStreamMap.value = currentMap

            val job = viewModelScope.launch {
                bleManager.observeNotification(serviceUuid, characteristicUuid, true).collect { bytes ->
                    val str = bytes.decodeToString()
                    val updatedMap = _notificationStreamMap.value.toMutableMap()
                    updatedMap[characteristicUuid] = "Live Notify: '$str' (${bytes.size} bytes)"
                    _notificationStreamMap.value = updatedMap
                }
            }
            notificationJobs[characteristicUuid] = job
        }
    }
}
