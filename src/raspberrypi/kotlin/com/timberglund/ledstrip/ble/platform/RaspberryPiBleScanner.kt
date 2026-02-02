package com.timberglund.ledstrip.ble.platform

import com.timberglund.ledstrip.ble.BleDevice
import com.timberglund.ledstrip.ble.BleScanner
import com.welie.blessed.*
import kotlinx.coroutines.*
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

class RaspberryPiBleScanner : BleScanner {

   override suspend fun discover(timeout: Long): List<BleDevice> = withContext(Dispatchers.IO) {
      val devices = ConcurrentHashMap<String, BleDevice>()

      val callback = object : BluetoothCentralManagerCallback() {
         override fun onDiscoveredPeripheral(peripheral: BluetoothPeripheral,
                                             scanResult: ScanResult) {
            val device = BleDevice(name = peripheral.name,
                                   address = peripheral.address,
                                   rssi = scanResult.rssi)
            devices[peripheral.address] = device
            logger.debug { "Discovered: ${device.name} - ${device.address}" }
         }
      }

      val central = BluetoothCentralManager(callback)
      central.scanForPeripherals()

      // Wait for timeout
      delay(timeout)

      central.stopScan()
      devices.values.toList()
   }

   override suspend fun findDeviceByAddress(address: String,
                                            timeout: Long): BleDevice? = withContext(Dispatchers.IO) {
      var foundDevice: BleDevice? = null

      val callback = object : BluetoothCentralManagerCallback() {
         override fun onDiscoveredPeripheral(peripheral: BluetoothPeripheral,
                                             scanResult: ScanResult) {
            if(peripheral.address == address) {
               foundDevice = BleDevice(
                  name = peripheral.name,
                  address = peripheral.address,
                  rssi = scanResult.rssi
               )
            }
         }
      }

      val central = BluetoothCentralManager(callback)
      central.scanForPeripheralsWithAddresses(arrayOf(address))

      // Wait for result or timeout
      withTimeoutOrNull(timeout) {
         while(foundDevice == null && isActive) {
            delay(100)
         }
      }

      central.stopScan()
      foundDevice
   }
}
