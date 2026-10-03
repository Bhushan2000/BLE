# ⚡ Ninja - Bluetooth Low Energy (BLE) Multiplatform Hub

A modern, clean-architecture Kotlin Multiplatform (KMP) & Compose Multiplatform application designed for discovering, connecting to, and interacting with Bluetooth Low Energy (BLE) peripherals.

---

## 📖 What is BLE (Bluetooth Low Energy)?

**Bluetooth Low Energy (BLE)** (also known as *Bluetooth Smart*) is a wireless Personal Area Network (PAN) technology designed by the Bluetooth Special Interest Group (SIG).

Unlike **Classic Bluetooth** (which is designed for continuous high-data streaming like phone calls or high-bitrate audio), **BLE** is optimized for:
- 🔋 **Ultra-Low Power Consumption**: Devices can run for months or years on a single coin-cell battery.
- ⚡ **Short Data Bursts**: Designed to transmit small packets of data periodically or on demand.
- 🔄 **Fast Connection Times**: Connects and exchanges data in milliseconds.

---

## 🌍 Where is BLE Used?

BLE powers millions of smart devices around us every day:

- ⌚ **Smart Wearables**: Smartwatches, fitness bands, pulse oximeters, and wireless earbuds.
- 🏥 **Healthcare & Medical**: Heart rate monitors, continuous glucose monitors (CGM), digital thermometers, and blood pressure cuffs.
- 🏠 **Smart Home & IoT**: Smart locks, ambient temperature/humidity sensors, smart lightbulbs, and security beacons.
- 📍 **Location & Asset Tracking**: Apple AirTags, Tile beacons, and indoor navigation systems in airports and shopping malls.

---

## 🏗️ BLE Architecture Explained

BLE communication operates on two primary layers: **GAP** and **GATT**.

### 1. GAP (Generic Access Profile)
GAP defines how BLE devices advertise their presence and establish connections.
- **Peripheral (Advertiser)**: Small devices (e.g., a Heart Rate Monitor) that broadcast short "advertising packets" into the air saying *"I'm here!"*.
- **Central (Scanner)**: Your smartphone or computer that scans the air for advertising peripherals and initiates a connection.

---

### 2. GATT (Generic Attribute Profile)
Once a Central device connects to a Peripheral, **GATT** defines how data is organized and exchanged using a hierarchical structure:

```
[ BLE Peripheral Device ]
   │
   └── 📁 GATT Profile
         │
         ├── 📦 Service 1: Heart Rate Service (UUID: 0x180D)
         │      │
         │      ├── 📄 Characteristic: Heart Rate Measurement (UUID: 0x2A37)
         │      │      ├── Properties: [READ, NOTIFY]
         │      │      └── Value: "78 BPM"
         │      │
         │      └── 📄 Characteristic: Body Sensor Location (UUID: 0x2A38)
         │             ├── Properties: [READ]
         │             └── Value: "Chest"
         │
         └── 📦 Service 2: Battery Service (UUID: 0x180F)
                │
                └── 📄 Characteristic: Battery Level (UUID: 0x2A19)
                       ├── Properties: [READ, WRITE, NOTIFY]
                       └── Value: "88%"
```

#### Hierarchy Breakdown:
1. **Service**: A collection of related data and behaviors (e.g., *Battery Service* or *Heart Rate Service*). Each service is identified by a unique **UUID**.
2. **Characteristic**: A specific data point inside a service (e.g., *Battery Level* = `88%`). Each characteristic has **Properties**:
   - 📖 **READ**: Tapping this fetches the current value from the device.
   - ✏️ **WRITE**: Sending new data/settings to the device.
   - 🔔 **NOTIFY / SUBSCRIBE**: Subscribing to live telemetry streaming updates from the device whenever its value changes.
3. **Descriptor**: Defined metadata attached to a characteristic (e.g., *Client Characteristic Configuration Descriptor (CCCD)* used to enable or disable notifications).

---

## ✨ What This Project Covers

This application provides a production-grade, end-to-end BLE management dashboard built with Kotlin Multiplatform:

1. 🔍 **Peripheral Discovery & RSSI Signal Metering**:
   - Real-time scanning for nearby BLE peripherals.
   - Live RSSI signal strength meters with dynamic dBm visual bar indicators.

2. 🔗 **Stable Connection Lifecycle**:
   - Pure LE transport connection configuration (`BluetoothDevice.TRANSPORT_LE`) preventing connection timeouts or status 133 errors.
   - Real-time status banners with interactive connect/disconnect controls.

3. 🧭 **GATT Profile & Service Explorer**:
   - Asynchronous GATT service discovery (`discoverServices()`).
   - Human-readable name resolver for standard Bluetooth SIG UUIDs (e.g. mapping `0x180F` to *"Battery Service"*, `0x2A19` to *"Battery Level"*).
   - Clean 16-bit short UUID badges (`0xXXXX`) alongside 128-bit base UUIDs.

4. 🧪 **Full GATT Interactive Operations**:
   - 📖 **Read Value**: Reads characteristic data and displays formatted ASCII string + Hexadecimal byte output in an interactive popup modal.
   - ✏️ **Write Value**: Sends text or raw byte arrays to writable characteristics with instant response feedback.
   - 🔔 **Multi-Characteristic Live Notifications**: Subscribe to multiple live notification streams simultaneously across any service in parallel.

5. 🎨 **Material 3 Expressive UI & Dark/Light Theme**:
   - Fully adaptive Dark Mode and Light Mode with vibrant Material 3 color schemes.
   - Status bar and navigation bar edge-to-edge transparent layout (`safeDrawingPadding()`) with auto-adapting system bar icon contrast.
   - Single unified `LazyColumn` for smooth scrolling across all screen sizes.

---

## 🛠️ Tech Stack & Project Architecture

Built following **Clean Architecture** and **MVI/MVVM** principles in Kotlin Multiplatform:

- **Kotlin Multiplatform (KMP)**: Core business logic and domain interfaces shared across Android, Desktop, and Web.
- **Compose Multiplatform**: Declarative Material 3 UI rendering consistently across all target platforms.
- **Kotlin Coroutines & Flow**: Asynchronous BLE scanning, GATT callbacks (`callbackFlow`), and reactive state streams (`StateFlow`).
- **Android Bluetooth LE API**: `BluetoothLeScanner`, `BluetoothGatt`, and `BluetoothGattCallback`.

### Module Structure
```
Ninja/
├── androidApp/                       # Native Android Application module
│   └── src/main/kotlin/.../
│       ├── MainActivity.kt           # Edge-to-edge entry point & runtime permissions
│       └── AndroidManifest.xml       # BLE & Location permissions & DayNight theme
│
├── shared/                           # Kotlin Multiplatform Shared module
│   ├── src/commonMain/               # Common Kotlin domain models & Compose UI
│   │   └── com/bhushantechsolutions/ninja/
│   │       ├── domain/ble/           # BleManager, BleDevice, BleGattService, BleGattResolver
│   │       ├── ui/                   # ConnectivityScreen, ConnectivityViewModel, App
│   │       └── data/mock/            # MockBleManager for Desktop & Web previews
│   │
│   └── src/androidMain/              # Android-specific implementations
│       └── com/bhushantechsolutions/ninja/
│           ├── data/ble/             # AndroidBleManager (BluetoothGatt API)
│           └── ui/                   # PlatformSystemBarTheme (System bar icon controller)
│
└── desktopApp/                       # Desktop (JVM) target launcher
```

---

## 🚀 Getting Started & How to Run

### Prerequisites
- Android Studio 2024.1 or newer / IntelliJ IDEA.
- JDK 17 or higher.
- Physical Android device with Bluetooth turned on (for testing real BLE peripherals) or Android Emulator.

### Running on Android
1. Open the project in Android Studio.
2. Select `:androidApp` from the run configurations dropdown.
3. Choose a connected Android device or emulator.
4. Click **Run** (`Shift + F10`).

### Running on Desktop (JVM)
Run the Desktop application via Gradle:
```powershell
./gradlew :desktopApp:run
```

---

## 📋 License

Designed and developed by **Bhushan Tech Solutions**. Distributed under the MIT License.
