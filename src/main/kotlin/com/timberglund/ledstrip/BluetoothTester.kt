package com.timberglund.ledstrip

import com.timberglund.ledstrip.ble.*
import kotlinx.coroutines.*
import mu.KotlinLogging

// UUIDs from data_service.gatt
private const val CHAR_UUID = "b8e3c9f2-4d5c-4b9f-c6d7-2e3f4d5c6b7a"

private val logger = KotlinLogging.logger {}

data class StripConnectionInfo(
   val id: Int,
   val name: String,
   val address: String,
   val connected: Boolean
)

/**
 * Manages BLE connections to LED strip devices.
 *
 * Scans for devices whose names start with "strip" (e.g. strip00, strip01),
 * connects to all of them, and maps each one by the numeric ID parsed from
 * the device name, corresponding to strip IDs in the YAML config.
 */
class BluetoothTester {
   private val platform = BlePlatform.getInstance()
   private val discoveredDevices: MutableMap<Int, BleDevice> = mutableMapOf()
   private val clients: MutableMap<Int, BleClient> = mutableMapOf()

   val isAnyConnected: Boolean get() = clients.values.any { it.isConnected }

   private fun notificationHandler(sender: String, data: ByteArray) {
      logger.debug { "Notification from $sender: ${data.size} bytes" }
   }

   private fun parseStripId(name: String): Int? =
      name.removePrefix("strip").toIntOrNull()

   /**
    * Scans for all strip devices and connects to each one.
    * Returns the number of successfully connected strips.
    */
   suspend fun scanAndConnect(): Int {
      logger.info { "Scanning for strip devices..." }

      val scanner = platform.createScanner()
      val devices = scanner.discover(timeout = 10000)
      var connected = 0

      for(device in devices) {
         val name = device.name ?: continue
         if(!name.startsWith("strip")) continue

         val stripId = parseStripId(name) ?: continue

         logger.info { "Found: $name (${device.address}), strip ID $stripId" }
         discoveredDevices[stripId] = device

         try {
            val client = platform.createClient(device.address)
            client.connect()
            client.startNotify(CHAR_UUID) { sender, data -> notificationHandler(sender, data) }
            clients[stripId] = client
            connected++
            logger.info { "Connected to $name (${device.address})" }
         }
         catch(e: Exception) {
            logger.error(e) { "Failed to connect to $name: ${e.message}" }
         }
      }

      if(connected == 0) {
         logger.warn { "No strip devices found" }
      }
      else {
         logger.info { "Connected to $connected strip(s): IDs ${clients.keys.sorted()}" }
      }

      return connected
   }

   /**
    * Sends a pre-built frame to a single strip.
    */
   suspend fun sendFrame(stripId: Int, data: ByteArray) {
      val client = clients[stripId] ?: return
      if(!client.isConnected) return
      try {
         client.writeGattCharacteristic(CHAR_UUID, data, withResponse = false)
      }
      catch(e: Exception) {
         logger.error(e) { "Failed to send frame to strip $stripId: ${e.message}" }
      }
   }

   /**
    * Returns info on every strip discovered during the last scan.
    */
   fun getStripInfos(): List<StripConnectionInfo> =
      discoveredDevices.entries.sortedBy { it.key }.map { (id, device) ->
         StripConnectionInfo(
            id = id,
            name = device.name ?: "unknown",
            address = device.address,
            connected = clients[id]?.isConnected == true
         )
      }

   /**
    * Connects to a previously discovered strip by ID.
    */
   suspend fun connectStrip(stripId: Int): Boolean {
      val device = discoveredDevices[stripId] ?: return false
      if(clients[stripId]?.isConnected == true) return true
      return try {
         val client = platform.createClient(device.address)
         client.connect()
         client.startNotify(CHAR_UUID) { sender, data -> notificationHandler(sender, data) }
         clients[stripId] = client
         logger.info { "Reconnected to strip $stripId" }
         true
      }
      catch(e: Exception) {
         logger.error(e) { "Failed to connect to strip $stripId: ${e.message}" }
         false
      }
   }

   /**
    * Disconnects from a single strip by ID.
    */
   suspend fun disconnectStrip(stripId: Int) {
      val client = clients[stripId] ?: return
      try {
         if(client.isConnected) {
            client.stopNotify(CHAR_UUID)
            client.disconnect()
            logger.info { "Disconnected from strip $stripId" }
         }
      }
      catch(e: Exception) {
         logger.error(e) { "Error disconnecting from strip $stripId: ${e.message}" }
      }
      clients.remove(stripId)
   }

   /**
    * Disconnects from all connected strips.
    */
   suspend fun disconnectAll() {
      for((stripId, client) in clients) {
         try {
            if(client.isConnected) {
               client.stopNotify(CHAR_UUID)
               client.disconnect()
               logger.info { "Disconnected from strip $stripId" }
            }
         }
         catch(e: Exception) {
            logger.error(e) { "Error disconnecting from strip $stripId: ${e.message}" }
         }
      }
      clients.clear()
   }
}
