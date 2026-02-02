package com.timberglund.ledstrip.ble

interface BleClient {
   val isConnected: Boolean

   suspend fun connect()
   suspend fun disconnect()
   suspend fun startNotify(
      characteristicUuid: String,
      callback: (String, ByteArray) -> Unit
   )
   suspend fun stopNotify(characteristicUuid: String)
   suspend fun writeGattCharacteristic(
      characteristicUuid: String,
      data: ByteArray,
      withResponse: Boolean
   )
}
