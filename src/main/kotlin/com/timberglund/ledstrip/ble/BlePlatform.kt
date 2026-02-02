package com.timberglund.ledstrip.ble

/**
 * Platform abstraction for BLE operations.
 * Each platform provides its own implementation via BlePlatformImpl.
 */
interface BlePlatform {
   fun createScanner(): BleScanner
   fun createClient(address: String): BleClient

   companion object {
      /**
      * Get the platform-specific implementation.
      * The concrete BlePlatformImpl class is resolved at compile-time
      * based on which source set is included (macos or raspberrypi).
      */
      fun getInstance(): BlePlatform = BlePlatformImpl()
   }
}
