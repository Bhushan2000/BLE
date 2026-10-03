# ⚡ Ninja - Bluetooth Low Energy (BLE) Multiplatform Hub

A production-grade, clean-architecture Kotlin Multiplatform (KMP) & Compose Multiplatform application designed for discovering, connecting to, and interacting with real physical Bluetooth Low Energy (BLE) peripherals across **Android, iOS, Web, and Desktop (JVM)**.

---

## 📖 What is BLE (Bluetooth Low Energy)?

**Bluetooth Low Energy (BLE)** (also known as *Bluetooth Smart*) is a wireless Personal Area Network (PAN) technology designed by the Bluetooth Special Interest Group (SIG).

Unlike **Classic Bluetooth** (designed for continuous high-bitrate streaming like phone calls), **BLE** is optimized for:
- 🔋 **Ultra-Low Power Consumption**: Devices run for months or years on a single coin-cell battery.
- ⚡ **Short Data Bursts**: Transmits small data packets periodically or on demand.
- 🔄 **Instant Connections**: Discovers, connects, and exchanges data in milliseconds.

---

## 🌍 Where is BLE Used?

BLE powers millions of smart devices around us every day:
- ⌚ **Smart Wearables**: Smartwatches, fitness trackers, pulse oximeters, and wireless audio.
- 🏥 **Healthcare & Medical**: Heart rate monitors, continuous glucose monitors (CGM), and digital thermometers.
- 🏠 **Smart Home & IoT**: Smart locks, ambient environmental sensors, and smart lighting.
- 📍 **Asset Tracking**: Apple AirTags, Tile beacons, and indoor airport navigation tags.

---

## 🏗️ BLE Architecture & Hierarchy

BLE communication operates on two primary layers: **GAP** and **GATT**.

### 1. GAP (Generic Access Profile)
GAP defines how BLE devices advertise their presence and establish connections.
- **Peripheral (Advertiser)**: Broadcasts short advertising packets into the air (*"I'm a Heart Rate Monitor"*).
- **Central (Scanner)**: Smartphone or computer that scans for advertising peripherals and initiates connections.

---

### 2. GATT (Generic Attribute Profile)
Once connected, **GATT** structures data hierarchically using **UUIDs**:

```
[ BLE Peripheral Device ]
   │
   └── 📁 GATT Profile
         │
         ├── 📦 Service: Heart Rate Service (UUID: 0x180D / 0000180D-0000-1000-8000-00805F9B34FB)
         │      │
         │      ├── 📄 Characteristic: Heart Rate Measurement (UUID: 0x2A37)
         │      │      ├── Properties: [READ, NOTIFY]
         │      │      └── Value: "78 BPM"
         │      │
         │      └── 📄 Characteristic: Body Sensor Location (UUID: 0x2A38)
         │             ├── Properties: [READ]
         │             └── Value: "Chest"
         │
         └── 📦 Service: Battery Service (UUID: 0x180F)
                │
                └── 📄 Characteristic: Battery Level (UUID: 0x2A19)
                       ├── Properties: [READ, WRITE, NOTIFY]
                       └── Value: "88%"
```

#### Characteristic Properties:
- 📖 **READ**: Fetches the current byte value from the peripheral.
- ✏️ **WRITE**: Sends data/commands to the peripheral.
- 🔔 **NOTIFY / SUBSCRIBE**: Subscribes to live streaming telemetry updates.

---

## ✨ Project Features & Coverage

This application provides a real physical hardware BLE management engine across all platforms:

1. 🔍 **Real Peripheral Discovery & RSSI Signal Metering**:
   - Live hardware scanning for nearby BLE devices across Android, iOS, Web, and Desktop.
   - Dynamic 4-bar RSSI signal strength meter painted via Compose `Canvas`.

2. 🔗 **Stable Connection Lifecycle**:
   - LE transport connection configuration (`BluetoothDevice.TRANSPORT_LE`) preventing connection timeouts and status 133 errors.
   - Status banners that automatically hide scan buttons and discovered lists when connected.

3. 🧭 **GATT Profile & SIG Name Resolver**:
   - Asynchronous GATT service discovery (`discoverServices()`).
   - Human-readable name resolver ([BleGattResolver.kt](file:///D:/AndroidProjects/Ninja/shared/src/commonMain/kotlin/com/bhushantechsolutions/ninja/domain/ble/BleGattResolver.kt)) mapping standard BLE SIG UUIDs to names (e.g. `0x180F` -> *"Battery Service"*, `0x2A19` -> *"Battery Level"*).
   - Clean 16-bit short UUID badges (`0xXXXX`) alongside 128-bit base UUIDs.

4. 🧪 **Full GATT Interactive Operations**:
   - 📖 **Read Value**: Reads characteristic data and displays formatted ASCII string + Hexadecimal byte output in an interactive **GATT Output Modal Popup**.
   - ✏️ **Write Value**: Sends text or raw byte arrays to writable characteristics with instant response feedback.
   - 🔔 **Multi-Characteristic Live Notifications**: Concurrent map listener supporting subscribing to multiple live notification streams simultaneously across any service in parallel.

5. 🎨 **Material 3 Expressive UI & Day/Night Dark/Light Theme**:
   - Fully adaptive Dark Mode and Light Mode with custom Material 3 palettes.
   - Status bar and navigation bar transparent edge-to-edge layout (`safeDrawingPadding()`) with auto-adapting system bar icon contrast (`PlatformSystemBarTheme`).
   - Single unified `LazyColumn` container for smooth scrolling without clipping or overflow.

---

## 💻 KMP Real Hardware BLE Matrix

| Target Platform | Native Engine / Framework | Hardware BLE Implementation File |
| :--- | :--- | :--- |
| 🤖 **Android** | `BluetoothLeScanner` & `BluetoothGatt` | [AndroidBleManager.kt](file:///D:/AndroidProjects/Ninja/shared/src/androidMain/kotlin/com/bhushantechsolutions/ninja/data/ble/AndroidBleManager.kt) |
| 🍎 **iOS** | Apple `CoreBluetooth` (`CBCentralManager`) | [IosBleManager.kt](file:///D:/AndroidProjects/Ninja/shared/src/iosMain/kotlin/com/bhushantechsolutions/ninja/data/ble/IosBleManager.kt) |
| 🌐 **Web (JS/Wasm)** | W3C `Web Bluetooth API` (`navigator.bluetooth`) | [WebBleManager.js.kt](file:///D:/AndroidProjects/Ninja/shared/src/jsMain/kotlin/com/bhushantechsolutions/ninja/data/ble/WebBleManager.js.kt) |
| 🖥️ **Desktop (JVM)** | Kable Engine (`com.juul.kable:kable`) | [DesktopBleManager.kt](file:///D:/AndroidProjects/Ninja/shared/src/jvmMain/kotlin/com/bhushantechsolutions/ninja/data/ble/DesktopBleManager.kt) |

---

## 🛠️ Module Architecture

```
Ninja/
├── androidApp/                       # Native Android Application module
│   └── src/main/kotlin/.../
│       ├── MainActivity.kt           # Edge-to-edge entry point & hardware prompts
│       └── AndroidManifest.xml       # BLE & Location permissions & DayNight theme
│
├── shared/                           # Kotlin Multiplatform Shared module
│   ├── src/commonMain/               # Common Kotlin domain models & Compose UI
│   │   └── com/bhushantechsolutions/ninja/
│   │       ├── domain/ble/           # BleManager, BleDevice, BleGattService, BleGattResolver
│   │       ├── ui/                   # ConnectivityScreen, ConnectivityViewModel, App
│   │       └── data/ble/             # PlatformBleManager factory (expect)
│   │
│   ├── src/androidMain/              # Android BLE & System Bar Inset Controllers
│   ├── src/iosMain/                  # iOS CoreBluetooth Swift/ObjC Interop
│   ├── src/jsMain/                   # Web Bluetooth API JS Promises -> Coroutine Flows
│   └── src/jvmMain/                  # Desktop JVM Kable BLE Engine (WinRT / CoreBluetooth / BlueZ)
│
└── desktopApp/                       # Desktop (JVM) target launcher
```

---

## 🚀 Getting Started & How to Run

### Prerequisites
- Android Studio 2024.1 or newer / IntelliJ IDEA.
- JDK 17 or higher.
- Physical device with Bluetooth turned on or Bluetooth USB dongle.

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
