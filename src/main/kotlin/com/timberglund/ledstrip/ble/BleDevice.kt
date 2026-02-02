package com.timberglund.ledstrip.ble

data class BleDevice(
   val name: String?,
   val address: String,
   val rssi: Int? = null
)
