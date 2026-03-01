package com.timberglund.ledstrip.ble.platform

import com.timberglund.ledstrip.ble.BleDevice
import com.timberglund.ledstrip.ble.BleScanner
import com.welie.blessed.*
import kotlinx.coroutines.*
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

class RaspberryPiBleScanner : BleScanner {

   // Handler swapped by each scan call; all discovered peripherals are forwarded to it.
   private var onDiscovered: (BluetoothPeripheral, ScanResult) -> Unit = { _, _ -> }

   private val callback = object : BluetoothCentralManagerCallback() {
      override fun onDiscoveredPeripheral(peripheral: BluetoothPeripheral,
                                          scanResult: ScanResult) {
         onDiscovered(peripheral, scanResult)
      }
   }

   // BluetoothCentralManager registers a D-Bus PairingAgent in its constructor.
   // Create it exactly once so repeated scans don't trigger "Object already exported".
   private val central: BluetoothCentralManager by lazy {
      BluetoothCentralManager(callback)
   }

   override suspend fun discover(timeout: Long): List<BleDevice> = withContext(Dispatchers.IO) {
      val devices = ConcurrentHashMap<String, BleDevice>()

      onDiscovered = { peripheral, scanResult ->
         val device = BleDevice(name = peripheral.name,
                                address = peripheral.address,
                                rssi = scanResult.rssi)
         devices[peripheral.address] = device
         logger.debug { "Discovered: ${device.name} - ${device.address}" }
      }

      central.scanForPeripherals()
      delay(timeout)
      central.stopScan()

      onDiscovered = { _, _ -> }
      devices.values.toList()
   }

   override suspend fun findDeviceByAddress(address: String,
                                            timeout: Long): BleDevice? = withContext(Dispatchers.IO) {
      var foundDevice: BleDevice? = null

      onDiscovered = { peripheral, scanResult ->
         if(peripheral.address == address) {
            foundDevice = BleDevice(name = peripheral.name,
                                    address = peripheral.address,
                                    rssi = scanResult.rssi)
         }
      }

      central.scanForPeripheralsWithAddresses(arrayOf(address))

      withTimeoutOrNull(timeout) {
         while(foundDevice == null && isActive) {
            delay(100)
         }
      }

      central.stopScan()
      onDiscovered = { _, _ -> }
      foundDevice
   }
}
