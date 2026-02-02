package com.timberglund.ledstrip.ble

interface BleScanner {
   suspend fun discover(timeout: Long): List<BleDevice>
   suspend fun findDeviceByAddress(address: String, timeout: Long): BleDevice?
}
