package com.bhushantechsolutions.ninja.data.ble

import com.bhushantechsolutions.ninja.domain.ble.BleConnectionState
import com.bhushantechsolutions.ninja.domain.ble.BleDevice
import com.bhushantechsolutions.ninja.domain.ble.BleGattService
import com.bhushantechsolutions.ninja.domain.ble.BleManager
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import platform.CoreBluetooth.CBCentralManager
import platform.CoreBluetooth.CBCentralManagerDelegateProtocol
import platform.CoreBluetooth.CBCentralManagerStatePoweredOn
import platform.CoreBluetooth.CBPeripheral
import platform.CoreBluetooth.CBPeripheralDelegateProtocol
import platform.Foundation.NSError
import platform.Foundation.NSNumber
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
class IosBleManager : NSObject(), BleManager, CBCentralManagerDelegateProtocol, CBPeripheralDelegateProtocol {

    private val centralManager = CBCentralManager(this, null)

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BleDevice>>(emptyList())
    override val discoveredDevices: StateFlow<List<BleDevice>> = _discoveredDevices.asStateFlow()

    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    override val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private var activePeripheral: CBPeripheral? = null
    private val discoveredPeripherals = mutableMapOf<String, CBPeripheral>()

    override fun centralManagerDidUpdateState(central: CBCentralManager) {
        if (central.state != CBCentralManagerStatePoweredOn) {
            _isScanning.value = false
            _connectionState.value = BleConnectionState.Error("Bluetooth is unavailable or powered off on iOS.")
        }
    }

    override fun centralManager(
        central: CBCentralManager,
        didDiscoverPeripheral: CBPeripheral,
        advertisementData: Map<Any?, *>,
        RSSI: NSNumber
    ) {
        val uuidString = didDiscoverPeripheral.identifier.UUIDString
        discoveredPeripherals[uuidString] = didDiscoverPeripheral

        val newDevice = BleDevice(
            address = uuidString,
            name = didDiscoverPeripheral.name ?: "Unknown Peripheral",
            rssi = RSSI.intValue
        )

        val currentList = _discoveredDevices.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.address == uuidString }
        if (existingIndex >= 0) {
            currentList[existingIndex] = newDevice
        } else {
            currentList.add(newDevice)
        }
        _discoveredDevices.value = currentList
    }

    override fun centralManager(central: CBCentralManager, didConnectPeripheral: CBPeripheral) {
        activePeripheral = didConnectPeripheral
        didConnectPeripheral.delegate = this
        _connectionState.value = BleConnectionState.Connected(
            address = didConnectPeripheral.identifier.UUIDString,
            deviceName = didConnectPeripheral.name
        )
    }

    @ObjCSignatureOverride
    override fun centralManager(central: CBCentralManager, didFailToConnectPeripheral: CBPeripheral, error: NSError?) {
        _connectionState.value = BleConnectionState.Error("iOS Connection Failed: ${error?.localizedDescription}")
    }

    @ObjCSignatureOverride
    override fun centralManager(central: CBCentralManager, didDisconnectPeripheral: CBPeripheral, error: NSError?) {
        activePeripheral = null
        _connectionState.value = BleConnectionState.Disconnected
    }

    override fun startScan() {
        if (centralManager.state != CBCentralManagerStatePoweredOn) {
            _connectionState.value = BleConnectionState.Error("Bluetooth is powered off on iOS.")
            return
        }

        _discoveredDevices.value = emptyList()
        discoveredPeripherals.clear()
        _isScanning.value = true
        centralManager.scanForPeripheralsWithServices(null, null)
    }

    override fun stopScan() {
        if (_isScanning.value) {
            centralManager.stopScan()
            _isScanning.value = false
        }
    }

    override fun connect(address: String) {
        val peripheral = discoveredPeripherals[address]
        if (peripheral == null) {
            _connectionState.value = BleConnectionState.Error("Peripheral not found for UUID: $address")
            return
        }

        stopScan()
        _connectionState.value = BleConnectionState.Connecting(address)
        centralManager.connectPeripheral(peripheral, null)
    }

    override fun disconnect() {
        activePeripheral?.let {
            centralManager.cancelPeripheralConnection(it)
        }
        activePeripheral = null
        _connectionState.value = BleConnectionState.Disconnected
    }

    override fun discoverServices(): Flow<List<BleGattService>> = callbackFlow {
        val peripheral = activePeripheral
        if (peripheral == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        peripheral.discoverServices(null)
        trySend(emptyList())
        close()
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

actual fun createPlatformBleManager(): BleManager = IosBleManager()
