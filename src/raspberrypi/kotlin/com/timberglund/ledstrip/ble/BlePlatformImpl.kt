package com.timberglund.ledstrip.ble

import com.timberglund.ledstrip.ble.platform.RaspberryPiBleScanner
import com.timberglund.ledstrip.ble.platform.RaspberryPiBleClient

class BlePlatformImpl : BlePlatform {
   private val scanner = RaspberryPiBleScanner()
   override fun createScanner(): BleScanner = scanner
   override fun createClient(address: String): BleClient = RaspberryPiBleClient(address)
}
