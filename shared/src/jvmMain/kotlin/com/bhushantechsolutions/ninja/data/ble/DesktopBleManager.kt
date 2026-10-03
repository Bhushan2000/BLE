package com.bhushantechsolutions.ninja.data.ble

import com.bhushantechsolutions.ninja.domain.ble.BleConnectionState
import com.bhushantechsolutions.ninja.domain.ble.BleDevice
import com.bhushantechsolutions.ninja.domain.ble.BleGattCharacteristic
import com.bhushantechsolutions.ninja.domain.ble.BleGattService
import com.bhushantechsolutions.ninja.domain.ble.BleManager
import com.juul.kable.Advertisement
import com.juul.kable.Peripheral
import com.juul.kable.Scanner
import com.juul.kable.characteristicOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

@Suppress("DEPRECATION", "DEPRECATION_ERROR")
class DesktopBleManager(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) : BleManager {

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BleDevice>>(emptyList())
    override val discoveredDevices: StateFlow<List<BleDevice>> = _discoveredDevices.asStateFlow()

    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    override val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val discoveredAdvertisements = mutableMapOf<String, Advertisement>()
    private var activePeripheral: Peripheral? = null
    private var scanJob: Job? = null

    override fun startScan() {
        if (_isScanning.value) return
        _isScanning.value = true
        _discoveredDevices.value = emptyList()
        discoveredAdvertisements.clear()

        scanJob = scope.launch {
            try {
                val scanner = Scanner()
                scanner.advertisements.collect { advertisement ->
                    val identifier = advertisement.identifier.toString()
                    discoveredAdvertisements[identifier] = advertisement

                    val newDevice = BleDevice(
                        address = identifier,
                        name = advertisement.name ?: "Unknown Desktop BLE Peripheral",
                        rssi = advertisement.rssi
                    )

                    val currentList = _discoveredDevices.value.toMutableList()
                    val existingIndex = currentList.indexOfFirst { it.address == identifier }
                    if (existingIndex >= 0) {
                        currentList[existingIndex] = newDevice
                    } else {
                        currentList.add(newDevice)
                    }
                    _discoveredDevices.value = currentList
                }
            } catch (t: Throwable) {
                _isScanning.value = false
                val errorMsg = if (t.message?.contains("unwrap", ignoreCase = true) == true || t.message?.contains("None", ignoreCase = true) == true) {
                    "Bluetooth is turned OFF or no Bluetooth hardware adapter was detected on this PC. Please enable Bluetooth in OS Settings."
                } else {
                    "Desktop BLE Scan Error: ${t.message}"
                }
                _connectionState.value = BleConnectionState.Error(errorMsg)
            }
        }
    }

    override fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        _isScanning.value = false
    }

    override fun connect(address: String) {
        val advertisement = discoveredAdvertisements[address]
        if (advertisement == null) {
            _connectionState.value = BleConnectionState.Error("Device not found for address: $address")
            return
        }

        stopScan()
        _connectionState.value = BleConnectionState.Connecting(address)

        scope.launch {
            try {
                val p = Peripheral(advertisement)
                activePeripheral = p
                p.connect()
                _connectionState.value = BleConnectionState.Connected(address, advertisement.name)
            } catch (t: Throwable) {
                val errorMsg = if (t.message?.contains("unwrap", ignoreCase = true) == true || t.message?.contains("None", ignoreCase = true) == true) {
                    "Bluetooth device is no longer reachable or Bluetooth was turned OFF on this PC."
                } else {
                    "Desktop Connection Error: ${t.message}"
                }
                _connectionState.value = BleConnectionState.Error(errorMsg)
            }
        }
    }

    override fun disconnect() {
        scope.launch {
            try {
                activePeripheral?.disconnect()
            } catch (_: Throwable) {}
            activePeripheral = null
            _connectionState.value = BleConnectionState.Disconnected
        }
    }

    override fun discoverServices(): Flow<List<BleGattService>> = flow {
        val p = activePeripheral
        if (p == null) {
            emit(emptyList())
            return@flow
        }

        try {
            val kableServices = p.services.value ?: emptyList()
            val mappedServices = kableServices.map { service ->
                BleGattService(
                    uuid = service.serviceUuid.toString(),
                    characteristics = service.characteristics.map { chara ->
                        BleGattCharacteristic(
                            uuid = chara.characteristicUuid.toString(),
                            properties = 0,
                            isReadable = true,
                            isWritable = true,
                            isNotifiable = true
                        )
                    }
                )
            }
            emit(mappedServices)
        } catch (_: Throwable) {
            emit(emptyList())
        }
    }

    override fun readCharacteristic(serviceUuid: String, characteristicUuid: String): Flow<ByteArray> = flow {
        val p = activePeripheral ?: return@flow
        try {
            val chara = characteristicOf(serviceUuid, characteristicUuid)
            val bytes = p.read(chara)
            emit(bytes)
        } catch (_: Throwable) {
            emit(byteArrayOf())
        }
    }

    override fun writeCharacteristic(serviceUuid: String, characteristicUuid: String, value: ByteArray): Boolean {
        val p = activePeripheral ?: return false
        scope.launch {
            try {
                val chara = characteristicOf(serviceUuid, characteristicUuid)
                p.write(chara, value)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
        return true
    }

    override fun observeNotification(serviceUuid: String, characteristicUuid: String, enable: Boolean): Flow<ByteArray> = flow {
        val p = activePeripheral ?: return@flow
        try {
            val chara = characteristicOf(serviceUuid, characteristicUuid)
            p.observe(chara).collect { bytes ->
                emit(bytes)
            }
        } catch (_: Throwable) {
            emit(byteArrayOf())
        }
    }
}

actual fun createPlatformBleManager(): BleManager = DesktopBleManager()
