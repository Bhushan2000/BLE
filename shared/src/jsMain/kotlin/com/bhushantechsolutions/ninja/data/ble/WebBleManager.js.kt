package com.bhushantechsolutions.ninja.data.ble

import com.bhushantechsolutions.ninja.domain.ble.BleConnectionState
import com.bhushantechsolutions.ninja.domain.ble.BleDevice
import com.bhushantechsolutions.ninja.domain.ble.BleGattService
import com.bhushantechsolutions.ninja.domain.ble.BleManager
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow

class WebBleManager(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) : BleManager {

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BleDevice>>(emptyList())
    override val discoveredDevices: StateFlow<List<BleDevice>> = _discoveredDevices.asStateFlow()

    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    override val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private var activeDevice: dynamic = null

    override fun startScan() {
        val bluetooth = window.asDynamic().navigator.bluetooth
        if (bluetooth == null) {
            _connectionState.value = BleConnectionState.Error("Web Bluetooth API is not supported in this browser.")
            return
        }

        _isScanning.value = true
        _discoveredDevices.value = emptyList()

        val options = js("{ acceptAllDevices: true }")

        bluetooth.requestDevice(options).then({ device: dynamic ->
            _isScanning.value = false
            activeDevice = device

            val newDevice = BleDevice(
                address = device.id.toString(),
                name = device.name?.toString() ?: "Unknown Web BLE Device",
                rssi = -60
            )
            _discoveredDevices.value = listOf(newDevice)
        }, { error: dynamic ->
            _isScanning.value = false
            _connectionState.value = BleConnectionState.Error("Web BLE Error: ${error.message}")
        })
    }

    override fun stopScan() {
        _isScanning.value = false
    }

    override fun connect(address: String) {
        val device = activeDevice
        if (device == null) {
            _connectionState.value = BleConnectionState.Error("No Web BLE device selected.")
            return
        }

        _connectionState.value = BleConnectionState.Connecting(address)

        device.gatt.connect().then({ gatt: dynamic ->
            _connectionState.value = BleConnectionState.Connected(
                address = address,
                deviceName = device.name?.toString()
            )
        }, { error: dynamic ->
            _connectionState.value = BleConnectionState.Error("GATT Connection Failed: ${error.message}")
        })
    }

    override fun disconnect() {
        activeDevice?.gatt?.disconnect()
        activeDevice = null
        _connectionState.value = BleConnectionState.Disconnected
    }

    override fun discoverServices(): Flow<List<BleGattService>> = callbackFlow {
        val gatt = activeDevice?.gatt
        if (gatt == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        gatt.getPrimaryServices().then({ services: dynamic ->
            val result = mutableListOf<BleGattService>()
            val len = services.length as Int
            for (i in 0 until len) {
                val service = services[i]
                result.add(
                    BleGattService(
                        uuid = service.uuid.toString(),
                        characteristics = emptyList()
                    )
                )
            }
            trySend(result)
            close()
        }, { error: dynamic ->
            trySend(emptyList())
            close()
        })

        awaitClose {}
    }

    override fun readCharacteristic(serviceUuid: String, characteristicUuid: String): Flow<ByteArray> = flow {
        emit(byteArrayOf())
    }

    override fun writeCharacteristic(serviceUuid: String, characteristicUuid: String, value: ByteArray): Boolean {
        return true
    }

    override fun observeNotification(serviceUuid: String, characteristicUuid: String, enable: Boolean): Flow<ByteArray> = flow {
        emit(byteArrayOf())
    }
}

actual fun createPlatformBleManager(): BleManager = WebBleManager()
