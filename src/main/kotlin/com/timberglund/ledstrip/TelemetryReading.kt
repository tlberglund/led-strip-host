package com.timberglund.ledstrip

import mu.KotlinLogging
import java.nio.ByteBuffer
import java.nio.ByteOrder

private val logger = KotlinLogging.logger {}

data class TelemetryReading(
   val timestamp: Long,
   val status: Int,
   val temperature: Float,
   val current: Float,
   val uptimeMs: Long,
   val frames: Long
)

/**
 * Parses a BTReply binary payload from a BLE notification.
 *
 * Handles two layouts (little-endian):
 *   Padded (32 bytes): status@0, padding@2, temperature@4, current@8, padding@12, uptime@16, frames@24
 *   Packed (26 bytes): status@0, temperature@2, current@6, uptime@10, frames@18
 *
 * Returns null and logs a warning for unexpected payload sizes.
 */
fun parseTelemetryReply(data: ByteArray): TelemetryReading? {
   val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
   return when(data.size) {
      32 -> TelemetryReading(
         timestamp = System.currentTimeMillis(),
         status = buf.getShort(0).toInt() and 0xFFFF,
         temperature = buf.getFloat(4),
         current = buf.getFloat(8),
         uptimeMs = buf.getLong(16),
         frames = buf.getLong(24)
      )
      26 -> TelemetryReading(
         timestamp = System.currentTimeMillis(),
         status = buf.getShort(0).toInt() and 0xFFFF,
         temperature = buf.getFloat(2),
         current = buf.getFloat(6),
         uptimeMs = buf.getLong(10),
         frames = buf.getLong(18)
      )
      else -> {
         logger.warn { "Unexpected telemetry payload size: ${data.size} bytes (expected 26 or 32)" }
         null
      }
   }
}
