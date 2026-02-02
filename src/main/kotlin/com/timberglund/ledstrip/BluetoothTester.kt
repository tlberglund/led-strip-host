package com.timberglund.ledstrip

import com.timberglund.ledstrip.ble.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.random.Random
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import mu.KotlinLogging

// UUIDs from data_service.gatt
private const val CHAR_UUID = "b8e3c9f2-4d5c-4b9f-c6d7-2e3f4d5c6b7a"

private val logger = KotlinLogging.logger {}


/**
 * Bluetooth Low Energy tester for Pico devices.
 *
 * Uses platform-specific BLE implementations via the BlePlatform factory.
 * Supported platforms: macOS (with USB dongle), Raspberry Pi
 */
class BluetoothTester {
   private val platform = BlePlatform.getInstance()
   private var client: BleClient? = null
   private var lastResponse: ByteArray? = null
   private val responseChannel = Channel<ByteArray>(Channel.CONFLATED)

   /** Callback for when notifications are received */
   private fun notificationHandler(sender: String, data: ByteArray) {
      lastResponse = data
      responseChannel.trySend(data)

      println("\n<< Received: ${data.size} bytes")
      println("   Hex: ${data.toHexString()}")
      println("   Data: ${data.contentToString()}")
   }

   /** Scan for and connect to a Pico device */
   suspend fun scanAndConnect(): Boolean {
      println("Scanning for Pico devices...")

      val scanner = platform.createScanner()
      val devices = scanner.discover(timeout = 10000)

      // Alternative: find by specific address
      // val device = scanner.findDeviceByAddress("2C:CF:67:CA:97:70", timeout = 10000)
      // val devices = if (device != null) listOf(device) else emptyList()

      for (device in devices) {
         if (device.name?.startsWith("strip") == true) {
            println("Found: ${device.name} - ${device.address}")

            try {
               client = platform.createClient(device.address)
               client?.connect()

               // Subscribe to notifications
               client?.startNotify(CHAR_UUID) { sender, data -> notificationHandler(sender, data) }

               println("Connected to ${device.name} (${device.address})")
               println("Subscribed to notifications on $CHAR_UUID\n")
               return true
            } catch (e: Exception) {
               println("Failed to connect to ${device.name}: ${e.message}")
               return false
            }
         }
      }

      println("No Pico devices found")
      return false
   }

   /** Send 244-byte message and wait for response */
   suspend fun sendAndReceive() {
      val currentClient = client
      if(currentClient == null || !currentClient.isConnected) {
         println("Not connected!")
         return
      }

      // Create 244-byte message: 4 bytes length + 240 bytes random data
      val payloadLen = 240
      val randomBytes = Random.nextBytes(payloadLen)

      // Pack the message: 2 bytes for ID (1), 2 bytes for length (payloadLen + 2), then payload
      val message =
              ByteBuffer.allocate(4 + payloadLen)
                      .apply {
                         order(ByteOrder.LITTLE_ENDIAN)
                         putShort(1) // ID
                         putShort((payloadLen + 2).toShort()) // Length
                         put(randomBytes)
                      }
                      .array()

      println(">> Sending: ${message.size} bytes (4 byte length + $payloadLen random bytes)")

      // Extract the length header for display
      val lengthHeader = ByteBuffer.wrap(message, 0, 4).apply { order(ByteOrder.LITTLE_ENDIAN) }.int
      println("   Length header: $lengthHeader")
      println("   First 16 bytes: ${message.take(16).toByteArray().toHexString()}")

      try {
         // Clear previous response
         lastResponse = null
         responseChannel.tryReceive() // Clear channel

         // Send the data
         currentClient.writeGattCharacteristic(CHAR_UUID, message, withResponse = false)

         // Wait for response (with timeout)
         try {
            withTimeout(2000) { responseChannel.receive() }
         }
         catch (e: TimeoutCancellationException) {
            println("   (No response received within 2 seconds)")
         }
      }
      catch (e: Exception) {
         println("Error sending data: ${e.message}")
      }
   }

   /** Disconnect from device */
   suspend fun disconnect() {
      client?.let { c ->
         try {
            if (c.isConnected) {
               c.stopNotify(CHAR_UUID)
               c.disconnect()
               println("\nDisconnected")
            }
         } catch (e: Exception) {
            println("Error disconnecting: ${e.message}")
         }
      }
   }

   /** Main test loop */
   suspend fun run() {
      // Configure logging level for BLE library
      // (This would depend on your chosen BLE library)

      if (!scanAndConnect()) {
         return
      }

      println("Press Enter to send a message, 'q' to quit\n")

      try {
         while (true) {
            // Read input in non-blocking way
            val userInput = withContext(Dispatchers.IO) { readLine() ?: "" }

            if (userInput.trim().lowercase() == "q") {
               break
            }

            sendAndReceive()
            println() // Extra newline for readability
         }
      } catch (e: CancellationException) {
         println("\nInterrupted by user")
      } finally {
         disconnect()
      }
   }
}

/** Extension function to convert ByteArray to hex string */
private fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }

/** Main entry point */
suspend fun main() {
   val tester = BluetoothTester()
   tester.run()
}
