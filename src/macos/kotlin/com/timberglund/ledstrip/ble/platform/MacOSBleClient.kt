package com.timberglund.ledstrip.ble.platform

import com.timberglund.ledstrip.ble.BleClient
import com.welie.blessed.*
import kotlinx.coroutines.*
import mu.KotlinLogging
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private val logger = KotlinLogging.logger {}

class MacOSBleClient(private val address: String) : BleClient {

   private var central: BluetoothCentralManager? = null
   private var peripheral: BluetoothPeripheral? = null
   private val notificationCallbacks = mutableMapOf<UUID, (String, ByteArray) -> Unit>()

   override var isConnected: Boolean = false
      private set

   /**
    * Find a characteristic by UUID across all services
    */
   private fun findCharacteristic(uuid: UUID): BluetoothGattCharacteristic? {
      peripheral?.let { p ->
         for(service in p.services) {
            val characteristic = service.getCharacteristic(uuid)
            if(characteristic != null) {
               return characteristic
            }
         }
      }
      return null
   }


   override suspend fun connect() = suspendCoroutine<Unit> { continuation ->
      var resumed = false

      val peripheralCallback = object : BluetoothPeripheralCallback() {
         override fun onServicesDiscovered(peripheral: BluetoothPeripheral,
                                           services: List<BluetoothGattService>) {
            logger.info { "Services discovered for ${peripheral.address}: ${services.size} services" }
            isConnected = true
            if(!resumed) {
               resumed = true
               continuation.resume(Unit)
            }
         }

         override fun onNotificationStateUpdate(peripheral: BluetoothPeripheral,
                                                characteristic: BluetoothGattCharacteristic,
                                                status: BluetoothCommandStatus) {
               if (status == BluetoothCommandStatus.COMMAND_SUCCESS) {
                  logger.info { "Notifications enabled for ${characteristic.uuid}" }
               } else {
                  logger.warn { "Failed to enable notifications for ${characteristic.uuid}: $status" }
               }
         }

         override fun onCharacteristicUpdate(peripheral: BluetoothPeripheral,
                                             value: ByteArray,
                                             characteristic: BluetoothGattCharacteristic,
                                             status: BluetoothCommandStatus) {
            if(status == BluetoothCommandStatus.COMMAND_SUCCESS) {
               notificationCallbacks[characteristic.uuid]?.invoke(peripheral.address, value)
            }
         }

         override fun onCharacteristicWrite(peripheral: BluetoothPeripheral,
                                            value: ByteArray,
                                            characteristic: BluetoothGattCharacteristic,
                                            status: BluetoothCommandStatus) {
            if(status == BluetoothCommandStatus.COMMAND_SUCCESS) {
               logger.debug { "Wrote ${value.size} bytes to ${characteristic.uuid}" }
            }
            else {
               logger.warn { "Write failed to ${characteristic.uuid}: $status" }
            }
         }
      }

      val connectionCallback = object : BluetoothCentralManagerCallback() {
         override fun onConnectedPeripheral(peripheral: BluetoothPeripheral) {
            logger.info { "Connected to ${peripheral.address}" }
         }

         override fun onConnectionFailed(peripheral: BluetoothPeripheral,
                                         status: BluetoothCommandStatus) {
            logger.error { "Connection failed: $status" }
            if(!resumed) {
               resumed = true
               continuation.resumeWithException(Exception("Connection failed: $status"))
            }
         }

         override fun onDisconnectedPeripheral(peripheral: BluetoothPeripheral,
                                               status: BluetoothCommandStatus) {
            logger.info { "Disconnected from ${peripheral.address}: $status" }
            isConnected = false
         }
      }

      // Create central manager with callback
      central = BluetoothCentralManager(connectionCallback)

      // Get the peripheral and connect
      peripheral = central?.getPeripheral(address)
      peripheral?.let { p ->
         central?.connectPeripheral(p, peripheralCallback)
      } ?: run {
         if(!resumed) {
            resumed = true
            continuation.resumeWithException(Exception("Could not get peripheral with address $address"))
         }
      }
   }

   override suspend fun disconnect(): Unit = withContext(Dispatchers.IO) {
      peripheral?.let { p ->
         central?.cancelConnection(p)
         isConnected = false
      }
   }

   override suspend fun startNotify(characteristicUuid: String, 
                                    callback: (String, ByteArray) -> Unit): Unit = withContext(Dispatchers.IO) {
      val uuid = UUID.fromString(characteristicUuid)
      notificationCallbacks[uuid] = callback

      val characteristic = findCharacteristic(uuid)
      characteristic?.let { char ->
         peripheral?.setNotify(char, true)
      } ?: throw Exception("Characteristic $characteristicUuid not found")
   }

   override suspend fun stopNotify(characteristicUuid: String): Unit = withContext(Dispatchers.IO) {
      val uuid = UUID.fromString(characteristicUuid)
      notificationCallbacks.remove(uuid)

      val characteristic = findCharacteristic(uuid)
      characteristic?.let { char ->
         peripheral?.setNotify(char, false)
      }
   }

   override suspend fun writeGattCharacteristic(characteristicUuid: String,
                                                data: ByteArray,
                                                withResponse: Boolean): Unit = withContext(Dispatchers.IO) {
      val uuid = UUID.fromString(characteristicUuid)

      val characteristic = findCharacteristic(uuid)
      characteristic?.let { char ->
         val writeType = if(withResponse) {
            BluetoothGattCharacteristic.WriteType.WITH_RESPONSE
         }
         else {
            BluetoothGattCharacteristic.WriteType.WITHOUT_RESPONSE
         }

         peripheral?.writeCharacteristic(char, data, writeType)
      } ?: throw Exception("Characteristic $characteristicUuid not found")
   }
}
