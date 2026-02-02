package com.timberglund.ledstrip.ble

import com.timberglund.ledstrip.ble.platform.MacOSBleScanner
import com.timberglund.ledstrip.ble.platform.MacOSBleClient

class BlePlatformImpl : BlePlatform {
   override fun createScanner(): BleScanner = MacOSBleScanner()
   override fun createClient(address: String): BleClient = MacOSBleClient(address)
}
