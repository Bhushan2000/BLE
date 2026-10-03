package com.bhushantechsolutions.ninja.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhushantechsolutions.ninja.domain.ble.BleConnectionState
import com.bhushantechsolutions.ninja.domain.ble.BleDevice
import com.bhushantechsolutions.ninja.domain.ble.BleGattCharacteristic
import com.bhushantechsolutions.ninja.domain.ble.BleGattService

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConnectivityScreen(
    viewModel: ConnectivityViewModel,
    modifier: Modifier = Modifier
) {
    val isScanning by viewModel.isBleScanning.collectAsState()
    val devices by viewModel.bleDevices.collectAsState()
    val connectionState by viewModel.bleConnectionState.collectAsState()
    val services by viewModel.discoveredServices.collectAsState()
    val readValue by viewModel.activeReadValue.collectAsState()
    val popupMessage by viewModel.gattPopupDialogMessage.collectAsState()
    val notificationMap by viewModel.notificationStreamMap.collectAsState()
    val activeNotifyingSet by viewModel.activeNotifyingCharUuids.collectAsState()

    val isConnected = connectionState is BleConnectionState.Connected

    // Popup Modal Dialog for GATT Read/Write Output
    if (popupMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissGattPopup() },
            title = {
                Text(
                    text = "GATT Output Response",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = popupMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissGattPopup() },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        val screenWidth = maxWidth
        val maxContainerWidth = 840.dp
        val isCompact = screenWidth < 500.dp
        val horizontalPadding = if (screenWidth > 600.dp) 24.dp else 16.dp

        LazyColumn(
            modifier = Modifier
                .widthIn(max = maxContainerWidth)
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                HeaderTitleSection()
            }

            // Show Scan Button ONLY when disconnected
            if (!isConnected) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Nearby Peripherals",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            if (isScanning) {
                                Spacer(modifier = Modifier.width(10.dp))
                                PulsingScanBadge()
                            }
                        }

                        Button(
                            onClick = { viewModel.toggleBleScan() },
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isScanning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = if (isScanning) "Stop Scan" else "Start Scan",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            item {
                BleStatusCard(
                    connectionState = connectionState,
                    onDisconnect = { viewModel.disconnectBle() }
                )
            }

            // Show GATT Inspector when connected
            if (isConnected) {
                item {
                    GattInspectorSection(
                        services = services,
                        readValue = readValue,
                        notificationMap = notificationMap,
                        activeNotifyingSet = activeNotifyingSet,
                        isCompact = isCompact,
                        onDiscoverServices = { viewModel.discoverServices() },
                        onReadCharacteristic = { serviceUuid, charUuid -> viewModel.readCharacteristic(serviceUuid, charUuid) },
                        onWriteCharacteristic = { serviceUuid, charUuid, text -> viewModel.writeCharacteristic(serviceUuid, charUuid, text) },
                        onToggleNotification = { serviceUuid, charUuid -> viewModel.toggleNotification(serviceUuid, charUuid) }
                    )
                }
            } else {
                // Show Discovered Devices list ONLY when disconnected
                item {
                    Text(
                        text = "Discovered Devices (${devices.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                if (devices.isEmpty()) {
                    item {
                        EmptyScanState(
                            isScanning = isScanning,
                            title = "No BLE Devices Discovered",
                            subtitle = "Click 'Start Scan' to search for nearby Bluetooth Low Energy devices."
                        )
                    }
                } else {
                    items(devices, key = { it.address }) { device ->
                        BleDeviceCard(
                            device = device,
                            onConnect = { viewModel.connectBleDevice(device) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderTitleSection() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "NINJA CONNECTIVITY HUB",
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = "Bluetooth Low Energy",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GattInspectorSection(
    services: List<BleGattService>,
    readValue: String,
    notificationMap: Map<String, String>,
    activeNotifyingSet: Set<String>,
    isCompact: Boolean,
    onDiscoverServices: () -> Unit,
    onReadCharacteristic: (String, String) -> Unit,
    onWriteCharacteristic: (String, String, String) -> Boolean,
    onToggleNotification: (String, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (isCompact) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "GATT SERVICES EXPLORER",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (services.isEmpty()) "Tap 'Discover Services' to query GATT profile" else "Discovered Services (${services.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    FilledTonalButton(
                        onClick = onDiscoverServices,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Discover Services", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "GATT SERVICES EXPLORER",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (services.isEmpty()) "Tap 'Discover Services' to query GATT profile" else "Discovered Services (${services.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    FilledTonalButton(
                        onClick = onDiscoverServices,
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Discover Services", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }

            if (readValue.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "GATT RESPONSE OUTPUT",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = readValue,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            services.forEach { service ->
                GattServiceCard(
                    service = service,
                    notificationMap = notificationMap,
                    activeNotifyingSet = activeNotifyingSet,
                    isCompact = isCompact,
                    onReadCharacteristic = { charUuid -> onReadCharacteristic(service.uuid, charUuid) },
                    onWriteCharacteristic = { charUuid, text -> onWriteCharacteristic(service.uuid, charUuid, text) },
                    onToggleNotification = { charUuid -> onToggleNotification(service.uuid, charUuid) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun GattServiceCard(
    service: BleGattService,
    notificationMap: Map<String, String>,
    activeNotifyingSet: Set<String>,
    isCompact: Boolean,
    onReadCharacteristic: (String) -> Unit,
    onWriteCharacteristic: (String, String) -> Boolean,
    onToggleNotification: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = service.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                PropertyBadge(label = service.shortUuid, color = MaterialTheme.colorScheme.primary)
            }

            Text(
                text = "UUID: ${service.uuid}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            service.characteristics.forEach { chara ->
                GattCharacteristicItem(
                    characteristic = chara,
                    isNotifying = activeNotifyingSet.contains(chara.uuid),
                    notificationText = notificationMap[chara.uuid],
                    isCompact = isCompact,
                    onRead = { onReadCharacteristic(chara.uuid) },
                    onWrite = { text -> onWriteCharacteristic(chara.uuid, text) },
                    onToggleNotify = { onToggleNotification(chara.uuid) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GattCharacteristicItem(
    characteristic: BleGattCharacteristic,
    isNotifying: Boolean,
    notificationText: String?,
    isCompact: Boolean,
    onRead: () -> Unit,
    onWrite: (String) -> Boolean,
    onToggleNotify: () -> Unit
) {
    var writeText by remember { mutableStateOf("") }
    val isCustomUuid = !characteristic.uuid.uppercase().startsWith("0000")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = characteristic.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            PropertyBadge(label = characteristic.shortUuid, color = MaterialTheme.colorScheme.primary)
        }

        if (isCustomUuid) {
            Text(
                text = "UUID: ${characteristic.uuid}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Static Property Capability Badges (Wraps cleanly on narrow screens)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (characteristic.isReadable) PropertyBadge(label = "READABLE", color = MaterialTheme.colorScheme.primary)
            if (characteristic.isWritable) PropertyBadge(label = "WRITABLE", color = MaterialTheme.colorScheme.secondary)
            if (characteristic.isNotifiable) PropertyBadge(label = "NOTIFIABLE", color = MaterialTheme.colorScheme.tertiary)
        }

        if (notificationText != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PulsingScanBadge()
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = notificationText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Action Buttons Row / Flow (Adaptive for mobile vs desktop)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (characteristic.isReadable) {
                OutlinedButton(
                    onClick = onRead,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = if (isCompact) Modifier.fillMaxWidth() else Modifier
                ) {
                    Text("Read Value", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            }

            if (characteristic.isNotifiable) {
                Button(
                    onClick = onToggleNotify,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = if (isCompact) Modifier.fillMaxWidth() else Modifier,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isNotifying) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(
                        text = if (isNotifying) "Unsubscribe" else "Subscribe / Notify",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        if (characteristic.isWritable) {
            Spacer(modifier = Modifier.height(8.dp))

            if (isCompact) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = writeText,
                        onValueChange = { writeText = it },
                        label = { Text("Data to write") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Button(
                        onClick = { onWrite(writeText) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Write Value", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = writeText,
                        onValueChange = { writeText = it },
                        label = { Text("Data to write") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    Button(
                        onClick = { onWrite(writeText) },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text("Write Value", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

@Composable
private fun PropertyBadge(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.12f),
        contentColor = color,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun BleStatusCard(
    connectionState: BleConnectionState,
    onDisconnect: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = when (connectionState) {
            is BleConnectionState.Connected -> MaterialTheme.colorScheme.primaryContainer
            is BleConnectionState.Connecting -> MaterialTheme.colorScheme.tertiaryContainer
            is BleConnectionState.Error -> MaterialTheme.colorScheme.errorContainer
            is BleConnectionState.Disconnected -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        },
        animationSpec = tween(350)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "CONNECTION STATUS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (connectionState) {
                        is BleConnectionState.Connected -> "Connected: ${connectionState.deviceName ?: connectionState.address}"
                        is BleConnectionState.Connecting -> "Connecting to ${connectionState.address}..."
                        is BleConnectionState.Error -> "Error: ${connectionState.message}"
                        is BleConnectionState.Disconnected -> "Not Connected"
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            AnimatedVisibility(visible = connectionState is BleConnectionState.Connected) {
                OutlinedButton(
                    onClick = onDisconnect,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Disconnect", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

@Composable
private fun BleDeviceCard(
    device: BleDevice,
    onConnect: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(interactionSource = interactionSource, indication = null) { onConnect() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name ?: "Unknown Peripheral",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                RssiSignalBarIndicator(rssi = device.rssi)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${device.rssi} dBm",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun PulsingScanBadge() {
    val infiniteTransition = rememberInfiniteTransition(label = "ScanPulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AlphaPulse"
    )

    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
    )
}

@Composable
private fun RssiSignalBarIndicator(rssi: Int) {
    val bars = when {
        rssi >= -50 -> 4
        rssi >= -65 -> 3
        rssi >= -80 -> 2
        else -> 1
    }
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(modifier = Modifier.size(20.dp)) {
        val barWidth = 3.dp.toPx()
        val spacing = 2.dp.toPx()
        for (i in 0 until 4) {
            val barHeight = ((i + 1) * 4).dp.toPx()
            val x = i * (barWidth + spacing)
            val y = size.height - barHeight
            drawRoundRect(
                color = if (i < bars) activeColor else inactiveColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(2f, 2f)
            )
        }
    }
}

@Composable
private fun EmptyScanState(
    isScanning: Boolean,
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    strokeWidth = 3.dp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Scanning in progress...",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )
            } else {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
