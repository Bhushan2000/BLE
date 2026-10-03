package com.bhushantechsolutions.ninja.domain.ble

object BleGattResolver {

    private val serviceNames = mapOf(
        "1800" to "Generic Access Service",
        "1801" to "Generic Attribute Service",
        "1802" to "Immediate Alert Service",
        "1803" to "Link Loss Service",
        "1804" to "Tx Power Service",
        "1805" to "Current Time Service",
        "1806" to "Reference Time Update Service",
        "1807" to "Next DST Change Service",
        "1808" to "Glucose Service",
        "1809" to "Health Thermometer Service",
        "180A" to "Device Information Service",
        "180D" to "Heart Rate Service",
        "180E" to "Phone Alert Status Service",
        "180F" to "Battery Service",
        "1810" to "Blood Pressure Service",
        "1811" to "Alert Notification Service",
        "1812" to "Human Interface Device (HID) Service",
        "1813" to "Scan Parameters Service",
        "1814" to "Running Speed & Cadence Service",
        "1815" to "Automation IO Service",
        "1816" to "Cycling Speed & Cadence Service",
        "1818" to "Cycling Power Service",
        "1819" to "Location & Navigation Service",
        "181A" to "Environmental Sensing Service",
        "181B" to "Body Composition Service",
        "181C" to "User Data Service",
        "181D" to "Weight Scale Service",
        "181E" to "Bond Management Service",
        "181F" to "Continuous Glucose Monitoring Service",
        "1820" to "Internet Protocol Support Service",
        "1821" to "Indoor Positioning Service",
        "1822" to "Pulse Oximeter Service",
        "1823" to "HTTP Proxy Service"
    )

    private val characteristicNames = mapOf(
        "2A00" to "Device Name",
        "2A01" to "Appearance",
        "2A02" to "Peripheral Privacy Flag",
        "2A03" to "Reconnection Address",
        "2A04" to "Peripheral Preferred Connection Parameters",
        "2A05" to "Service Changed",
        "2A06" to "Alert Level",
        "2A07" to "Tx Power Level",
        "2A08" to "Date Time",
        "2A09" to "Day of Week",
        "2A0A" to "Day Date Time",
        "2A19" to "Battery Level",
        "2A1C" to "Temperature Measurement",
        "2A1D" to "Temperature Type",
        "2A1E" to "Intermediate Temperature",
        "2A21" to "Measurement Interval",
        "2A23" to "System ID",
        "2A24" to "Model Number String",
        "2A25" to "Serial Number String",
        "2A26" to "Firmware Revision String",
        "2A27" to "Hardware Revision String",
        "2A28" to "Software Revision String",
        "2A29" to "Manufacturer Name String",
        "2A2A" to "Regulatory Certification Data List",
        "2A37" to "Heart Rate Measurement",
        "2A38" to "Body Sensor Location",
        "2A39" to "Heart Rate Control Point",
        "2A4A" to "HID Information",
        "2A4B" to "Report Map",
        "2A4C" to "HID Control Point",
        "2A4D" to "Report",
        "2A4E" to "Protocol Mode",
        "2A50" to "PnP ID"
    )

    fun getShortUuid(fullUuid: String): String {
        val clean = fullUuid.uppercase()
        return if (clean.length >= 8 && clean.startsWith("0000")) {
            "0x" + clean.substring(4, 8)
        } else if (clean.length == 4) {
            "0x$clean"
        } else {
            "0x" + clean.take(8)
        }
    }

    private fun extract16BitKey(fullUuid: String): String {
        val clean = fullUuid.uppercase()
        return if (clean.length >= 8 && clean.startsWith("0000")) {
            clean.substring(4, 8)
        } else {
            clean.take(4)
        }
    }

    fun resolveServiceName(fullUuid: String): String {
        val key = extract16BitKey(fullUuid)
        return serviceNames[key] ?: "Custom Service"
    }

    fun resolveCharacteristicName(fullUuid: String): String {
        val key = extract16BitKey(fullUuid)
        return characteristicNames[key] ?: "Custom Characteristic"
    }
}
