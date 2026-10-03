package com.bhushantechsolutions.ninja

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.bhushantechsolutions.ninja.data.ble.AndroidBleManager
import com.bhushantechsolutions.ninja.ui.ConnectivityViewModel

class MainActivity : ComponentActivity() {

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
            checkBluetoothAndLocationHardwareState()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions()

        val bleManager = AndroidBleManager(applicationContext)

        setContent {
            val viewModel = remember {
                ConnectivityViewModel(bleManager = bleManager)
            }
            App(viewModel = viewModel)
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            checkBluetoothAndLocationHardwareState()
        }
    }

    @SuppressLint("MissingPermission")
    private fun checkBluetoothAndLocationHardwareState() {
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as? BluetoothManager
        val bluetoothAdapter = bluetoothManager?.adapter

        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled) {
            try {
                @Suppress("DEPRECATION")
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivity(enableBtIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val locationManager = getSystemService(LOCATION_SERVICE) as? LocationManager
        val isGpsEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true

        if (!isGpsEnabled && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            try {
                val locationIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(locationIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
