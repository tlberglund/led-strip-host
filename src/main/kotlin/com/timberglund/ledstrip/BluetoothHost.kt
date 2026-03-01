package com.timberglund.ledstrip

import com.timberglund.ledstrip.ble.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import mu.KotlinLogging

// UUIDs from data_service.gatt
private const val CHAR_UUID = "b8e3c9f2-4d5c-4b9f-c6d7-2e3f4d5c6b7a"

private val logger = KotlinLogging.logger {}

sealed class StripDiscoveryEvent {
   object ScanStarted : StripDiscoveryEvent()
   data class NewDeviceFound(val name: String, val address: String) : StripDiscoveryEvent()
   data class ScanCompleted(val found: Int) : StripDiscoveryEvent()
   data class ScanError(val message: String) : StripDiscoveryEvent()
   data class ReconnectAttempted(val id: Int, val name: String) : StripDiscoveryEvent()
   data class ReconnectSucceeded(val id: Int, val name: String) : StripDiscoveryEvent()
   data class ReconnectFailed(val id: Int, val name: String, val reason: String) : StripDiscoveryEvent()
}

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

   private val _discoveryEvents = MutableSharedFlow<StripDiscoveryEvent>(extraBufferCapacity = 64)
   val discoveryEvents: SharedFlow<StripDiscoveryEvent> = _discoveryEvents.asSharedFlow()

   // Strips the user explicitly disconnected — skipped by the auto-reconnect loop
   private val manuallyDisconnected: MutableSet<Int> = mutableSetOf()

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
    * Launches a coroutine that continuously scans for new strip devices at the given interval.
    * Only devices not already in the registry are added. Emits events on discoveryEvents.
    */
   fun startBackgroundScanning(scope: CoroutineScope, intervalMs: Long) {
      scope.launch(Dispatchers.IO) {
         while(isActive) {
            delay(intervalMs)
            try {
               logger.debug { "Background scan starting..." }
               _discoveryEvents.tryEmit(StripDiscoveryEvent.ScanStarted)
               val scanner = platform.createScanner()
               val devices = scanner.discover(timeout = 5000)
               var newFound = 0
               for(device in devices) {
                  val name = device.name ?: continue
                  if(!name.startsWith("strip")) continue
                  val stripId = parseStripId(name) ?: continue
                  if(!discoveredDevices.containsKey(stripId)) {
                     discoveredDevices[stripId] = device
                     newFound++
                     logger.info { "Background scan found new strip: $name (${device.address})" }
                     _discoveryEvents.tryEmit(StripDiscoveryEvent.NewDeviceFound(name, device.address))
                  }
               }
               _discoveryEvents.tryEmit(StripDiscoveryEvent.ScanCompleted(newFound))

               // Auto-reconnect: attempt to restore any dropped connections
               for((id, device) in discoveredDevices) {
                  if(manuallyDisconnected.contains(id)) continue
                  val client = clients[id]
                  if(client != null && client.isConnected) continue
                  val name = device.name ?: "strip$id"
                  _discoveryEvents.tryEmit(StripDiscoveryEvent.ReconnectAttempted(id, name))
                  val success = try {
                     connectStrip(id)
                  }
                  catch(e: Exception) {
                     logger.error(e) { "Auto-reconnect failed for strip $id: ${e.message}" }
                     false
                  }
                  if(success) {
                     logger.info { "Auto-reconnected strip $id ($name)" }
                     _discoveryEvents.tryEmit(StripDiscoveryEvent.ReconnectSucceeded(id, name))
                  }
                  else {
                     _discoveryEvents.tryEmit(
                        StripDiscoveryEvent.ReconnectFailed(id, name, "Connection attempt failed")
                     )
                  }
               }
            }
            catch(e: Exception) {
               logger.error(e) { "Background scan error: ${e.message}" }
               _discoveryEvents.tryEmit(StripDiscoveryEvent.ScanError(e.message ?: "Unknown error"))
            }
         }
      }
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
      manuallyDisconnected.remove(stripId)
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
      manuallyDisconnected.add(stripId)
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
