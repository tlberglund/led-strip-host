package com.timberglund.ledstrip.ble

import com.timberglund.ledstrip.ble.platform.RaspberryPiBleScanner
import com.timberglund.ledstrip.ble.platform.RaspberryPiBleClient

class BlePlatformImpl : BlePlatform {
   override fun createScanner(): BleScanner = RaspberryPiBleScanner()
   override fun createClient(address: String): BleClient = RaspberryPiBleClient(address)
}
