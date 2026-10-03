package com.bhushantechsolutions.ninja.data.mock

import com.bhushantechsolutions.ninja.domain.ble.BleConnectionState
import com.bhushantechsolutions.ninja.domain.ble.BleDevice
import com.bhushantechsolutions.ninja.domain.ble.BleGattCharacteristic
import com.bhushantechsolutions.ninja.domain.ble.BleGattService
import com.bhushantechsolutions.ninja.domain.ble.BleManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlin.random.Random

class MockBleManager(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) : BleManager {

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BleDevice>>(emptyList())
    override val discoveredDevices: StateFlow<List<BleDevice>> = _discoveredDevices.asStateFlow()

    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    override val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val sampleDevices = listOf(
        BleDevice("AA:BB:CC:11:22:33", "Smart Watch Pro", -58),
        BleDevice("DD:EE:FF:44:55:66", "Pulse Oximeter", -64),
        BleDevice("11:22:33:77:88:99", "Wireless Audio Headset", -42),
        BleDevice("99:88:77:AA:BB:CC", "Fitness Tracker", -78),
        BleDevice("12:34:56:78:90:AB", "Smart Home Sensor Hub", -85)
    )

    override fun startScan() {
        _isScanning.value = true
        _discoveredDevices.value = emptyList()

        scope.launch {
            sampleDevices.forEachIndexed { index, device ->
                if (!_isScanning.value) return@launch
                delay((600 + index * 400).toLong())
                _discoveredDevices.value = _discoveredDevices.value + device
            }
        }
    }

    override fun stopScan() {
        _isScanning.value = false
    }

    override fun connect(address: String) {
        stopScan()
        val device = _discoveredDevices.value.find { it.address == address } ?: sampleDevices.first()
        _connectionState.value = BleConnectionState.Connecting(address)

        scope.launch {
            delay(1200)
            _connectionState.value = BleConnectionState.Connected(address, device.name)
        }
    }

    override fun disconnect() {
        _connectionState.value = BleConnectionState.Disconnected
    }

    override fun discoverServices(): Flow<List<BleGattService>> = flow {
        delay(500)
        emit(
            listOf(
                BleGattService(
                    uuid = "0000180d-0000-1000-8000-00805f9b34fb", // Heart Rate Service
                    characteristics = listOf(
                        BleGattCharacteristic(
                            uuid = "00002a37-0000-1000-8000-00805f9b34fb", // Heart Rate Measurement
                            properties = 16,
                            isReadable = true,
                            isWritable = true,
                            isNotifiable = true
                        )
                    )
                ),
                BleGattService(
                    uuid = "0000180f-0000-1000-8000-00805f9b34fb", // Battery Service
                    characteristics = listOf(
                        BleGattCharacteristic(
                            uuid = "00002a19-0000-1000-8000-00805f9b34fb", // Battery Level
                            properties = 2,
                            isReadable = true,
                            isWritable = false,
                            isNotifiable = true
                        )
                    )
                )
            )
        )
    }

    override fun readCharacteristic(serviceUuid: String, characteristicUuid: String): Flow<ByteArray> = flow {
        delay(300)
        val mockData = when {
            characteristicUuid.contains("2a19") -> byteArrayOf(88) // 88% battery
            else -> "72 BPM".encodeToByteArray()
        }
        emit(mockData)
    }

    override fun writeCharacteristic(serviceUuid: String, characteristicUuid: String, value: ByteArray): Boolean {
        return true
    }

    override fun observeNotification(
        serviceUuid: String,
        characteristicUuid: String,
        enable: Boolean
    ): Flow<ByteArray> = flow {
        if (!enable) return@flow
        while (true) {
            val bpm = Random.nextInt(65, 88)
            emit("BPM: $bpm".encodeToByteArray())
            delay(1000)
        }
    }
}
