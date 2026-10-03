package com.bhushantechsolutions.ninja.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.os.Build
import com.bhushantechsolutions.ninja.domain.ble.BleConnectionState
import com.bhushantechsolutions.ninja.domain.ble.BleDevice
import com.bhushantechsolutions.ninja.domain.ble.BleGattCharacteristic
import com.bhushantechsolutions.ninja.domain.ble.BleGattService
import com.bhushantechsolutions.ninja.domain.ble.BleManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class AndroidBleManager(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) : BleManager {

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BleDevice>>(emptyList())
    override val discoveredDevices: StateFlow<List<BleDevice>> = _discoveredDevices.asStateFlow()

    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    override val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private var activeGatt: BluetoothGatt? = null
    private val notificationListenersMap = ConcurrentHashMap<String, (BluetoothGattCharacteristic, ByteArray) -> Unit>()
    private val readCallbackMap = ConcurrentHashMap<String, (ByteArray, Int) -> Unit>()
    private var serviceDiscoveryCallback: ((List<BleGattService>) -> Unit)? = null

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let { device ->
                val deviceName = device.name
                val address = device.address
                val rssi = result.rssi

                val newDevice = BleDevice(
                    address = address,
                    name = deviceName,
                    rssi = rssi
                )

                val currentList = _discoveredDevices.value.toMutableList()
                val existingIndex = currentList.indexOfFirst { it.address == address }
                if (existingIndex >= 0) {
                    currentList[existingIndex] = newDevice
                } else {
                    currentList.add(newDevice)
                }
                _discoveredDevices.value = currentList
            }
        }

        override fun onScanFailed(errorCode: Int) {
            _isScanning.value = false
            _connectionState.value = BleConnectionState.Error("BLE Scan failed with code: $errorCode")
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_DISCONNECTED) {
                activeGatt?.close()
                activeGatt = null
                notificationListenersMap.clear()
                readCallbackMap.clear()
                val errorMsg = when (status) {
                    133 -> "GATT Error 133 (Connection timed out / Radio busy)"
                    8 -> "GATT Error 8 (Connection timeout)"
                    19 -> "GATT Error 19 (Terminated by peripheral)"
                    22 -> "GATT Error 22 (Terminated by local host)"
                    137 -> "GATT Error 137 (Authentication required / Pairing needed)"
                    else -> "Connection dropped (GATT status code $status)"
                }
                _connectionState.value = BleConnectionState.Error(errorMsg)
                return
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                activeGatt = gatt
                gatt?.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
                val address = gatt?.device?.address ?: ""
                val name = gatt?.device?.name
                _connectionState.value = BleConnectionState.Connected(address, name)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                activeGatt?.close()
                activeGatt = null
                notificationListenersMap.clear()
                readCallbackMap.clear()
                _connectionState.value = BleConnectionState.Disconnected
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS && gatt != null) {
                val services = gatt.services.map { service ->
                    BleGattService(
                        uuid = service.uuid.toString(),
                        characteristics = service.characteristics.map { chara ->
                            BleGattCharacteristic(
                                uuid = chara.uuid.toString(),
                                properties = chara.properties,
                                isReadable = (chara.properties and BluetoothGattCharacteristic.PROPERTY_READ) != 0,
                                isWritable = (chara.properties and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0,
                                isNotifiable = (chara.properties and (BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_INDICATE)) != 0
                            )
                        }
                    )
                }
                serviceDiscoveryCallback?.invoke(services)
            } else {
                serviceDiscoveryCallback?.invoke(emptyList())
            }
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            val valBytes = if (status == BluetoothGatt.GATT_SUCCESS) {
                characteristic.value ?: byteArrayOf()
            } else {
                byteArrayOf()
            }
            readCallbackMap.remove(characteristic.uuid.toString().uppercase())?.invoke(valBytes, status)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            val valBytes = if (status == BluetoothGatt.GATT_SUCCESS) value else byteArrayOf()
            readCallbackMap.remove(characteristic.uuid.toString().uppercase())?.invoke(valBytes, status)
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val value = characteristic.value ?: byteArrayOf()
            notificationListenersMap.values.forEach { listener ->
                listener.invoke(characteristic, value)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            notificationListenersMap.values.forEach { listener ->
                listener.invoke(characteristic, value)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun startScan() {
        if (bluetoothAdapter == null) {
            _connectionState.value = BleConnectionState.Error("Bluetooth is not supported on this device.")
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            _connectionState.value = BleConnectionState.Error("Bluetooth is turned OFF. Please turn on Bluetooth.")
            promptEnableBluetooth()
            return
        }

        val scanner = bluetoothAdapter.bluetoothLeScanner
        if (scanner == null) {
            _connectionState.value = BleConnectionState.Error("Bluetooth LE Scanner is currently unavailable.")
            return
        }

        try {
            _discoveredDevices.value = emptyList()
            _isScanning.value = true
            if (_connectionState.value is BleConnectionState.Error) {
                _connectionState.value = BleConnectionState.Disconnected
            }
            scanner.startScan(scanCallback)
        } catch (e: Exception) {
            _isScanning.value = false
            _connectionState.value = BleConnectionState.Error("BLE Scan failed: ${e.message}")
        }
    }

    private fun promptEnableBluetooth() {
        try {
            @Suppress("DEPRECATION")
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(enableBtIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("MissingPermission")
    override fun stopScan() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (_isScanning.value && scanner != null) {
            scanner.stopScan(scanCallback)
            _isScanning.value = false
        }
    }

    @SuppressLint("MissingPermission")
    override fun connect(address: String) {
        val device = bluetoothAdapter?.getRemoteDevice(address)
        if (device == null) {
            _connectionState.value = BleConnectionState.Error("Device not found for address: $address")
            return
        }

        stopScan()
        _connectionState.value = BleConnectionState.Connecting(address)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activeGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            activeGatt = device.connectGatt(context, false, gattCallback)
        }
    }

    @SuppressLint("MissingPermission")
    override fun disconnect() {
        activeGatt?.disconnect()
        activeGatt?.close()
        activeGatt = null
        notificationListenersMap.clear()
        readCallbackMap.clear()
        _connectionState.value = BleConnectionState.Disconnected
    }

    @SuppressLint("MissingPermission")
    override fun discoverServices(): Flow<List<BleGattService>> = callbackFlow {
        val gatt = activeGatt
        if (gatt == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        serviceDiscoveryCallback = { services ->
            trySend(services)
            close()
        }

        val initiated = gatt.discoverServices()
        if (!initiated) {
            trySend(emptyList())
            close()
        }

        awaitClose {
            serviceDiscoveryCallback = null
        }
    }

    @SuppressLint("MissingPermission")
    override fun readCharacteristic(
        serviceUuid: String,
        characteristicUuid: String
    ): Flow<ByteArray> = callbackFlow {
        val gatt = activeGatt
        if (gatt == null) {
            trySend(byteArrayOf())
            close()
            return@callbackFlow
        }

        val service = gatt.getService(UUID.fromString(serviceUuid))
        val characteristic = service?.getCharacteristic(UUID.fromString(characteristicUuid))

        if (characteristic == null) {
            trySend(byteArrayOf())
            close()
            return@callbackFlow
        }

        val key = characteristic.uuid.toString().uppercase()
        readCallbackMap[key] = { bytes, status ->
            if (status == BluetoothGatt.GATT_SUCCESS) {
                trySend(bytes)
            } else {
                trySend("GATT Read Error (Status code: $status)".encodeToByteArray())
            }
            close()
        }

        val success = gatt.readCharacteristic(characteristic)
        if (!success) {
            readCallbackMap.remove(key)
            trySend("Failed to initiate GATT Read request".encodeToByteArray())
            close()
        }

        awaitClose {
            readCallbackMap.remove(key)
        }
    }

    @SuppressLint("MissingPermission")
    override fun writeCharacteristic(
        serviceUuid: String,
        characteristicUuid: String,
        value: ByteArray
    ): Boolean {
        val gatt = activeGatt ?: return false
        val service = gatt.getService(UUID.fromString(serviceUuid)) ?: return false
        val characteristic = service.getCharacteristic(UUID.fromString(characteristicUuid)) ?: return false

        val writeType = if ((characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        } else {
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        }
        characteristic.writeType = writeType

        @Suppress("DEPRECATION")
        characteristic.value = value
        @Suppress("DEPRECATION")
        return gatt.writeCharacteristic(characteristic)
    }

    @SuppressLint("MissingPermission")
    override fun observeNotification(
        serviceUuid: String,
        characteristicUuid: String,
        enable: Boolean
    ): Flow<ByteArray> = callbackFlow {
        val gatt = activeGatt ?: run {
            close()
            return@callbackFlow
        }

        val service = gatt.getService(UUID.fromString(serviceUuid)) ?: run {
            close()
            return@callbackFlow
        }

        val characteristic = service.getCharacteristic(UUID.fromString(characteristicUuid)) ?: run {
            close()
            return@callbackFlow
        }

        gatt.setCharacteristicNotification(characteristic, enable)

        val cccd = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
        if (cccd != null) {
            @Suppress("DEPRECATION")
            cccd.value = if (enable) BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE else BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
            @Suppress("DEPRECATION")
            gatt.writeDescriptor(cccd)
        }

        val listenerKey = "$serviceUuid:$characteristicUuid"
        if (enable) {
            notificationListenersMap[listenerKey] = { chara, data ->
                if (chara.uuid.toString().equals(characteristicUuid, ignoreCase = true)) {
                    trySend(data)
                }
            }
        } else {
            notificationListenersMap.remove(listenerKey)
        }

        awaitClose {
            notificationListenersMap.remove(listenerKey)
        }
    }
}
