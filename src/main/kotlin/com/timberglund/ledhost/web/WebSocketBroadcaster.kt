package com.timberglund.ledhost.web

import com.timberglund.ledhost.viewport.Viewport
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedSendChannelException
import java.util.zip.Deflater

/**
 * Manages WebSocket connections and broadcasts viewport updates to all connected clients.
 *
 * Wire format (binary frame):
 *   Byte 0:     flags (0x00 = uncompressed, 0x01 = deflate-compressed)
 *   Bytes 1-2:  width  (big-endian unsigned 16-bit)
 *   Bytes 3-4:  height (big-endian unsigned 16-bit)
 *   Bytes 5+:   RGB pixel data (3 bytes per pixel, row-major), optionally deflate-compressed
 */
class WebSocketBroadcaster {
   private val clients = mutableListOf<WebSocketSession>()
   private val lock = Any()

   /**
    * Registers a new WebSocket client.
    */
   fun addClient(session: WebSocketSession) {
      synchronized(lock) {
         clients.add(session)
      }
   }

   /**
    * Removes a WebSocket client.
    */
   fun removeClient(session: WebSocketSession) {
      synchronized(lock) {
         clients.remove(session)
      }
   }

   /**
    * Returns the number of connected clients.
    */
   fun getClientCount(): Int {
      synchronized(lock) {
         return clients.size
      }
   }

   /**
    * Broadcasts the current viewport state to all connected clients.
    */
   suspend fun broadcastViewport(viewport: Viewport) {
      val data = encodeViewport(viewport)
      val message = Frame.Binary(true, data)

      // Get snapshot of clients to avoid holding lock during broadcast
      val clientsSnapshot = synchronized(lock) {
         clients.toList()
      }

      // Broadcast to all clients, removing any that fail
      val failedClients = mutableListOf<WebSocketSession>()
      for(client in clientsSnapshot) {
         try {
            client.send(message)
         }
         catch(e: ClosedSendChannelException) {
            // Client disconnected
            failedClients.add(client)
         }
         catch(e: Exception) {
            // Other error, also remove client
            failedClients.add(client)
         }
      }

      // Remove failed clients
      if(failedClients.isNotEmpty()) {
         synchronized(lock) {
            clients.removeAll(failedClients)
         }
      }
   }

   /**
    * Encodes the viewport as a deflate-compressed binary frame.
    * Format: [0x01][width:2B][height:2B][deflated RGB data...]
    */
   private fun encodeViewport(viewport: Viewport): ByteArray {
      val pixelCount = viewport.width * viewport.height
      val rgbData = ByteArray(pixelCount * 3)

      // Build raw RGB pixel data
      var offset = 0
      for(y in 0 until viewport.height) {
         for(x in 0 until viewport.width) {
            val color = viewport.getPixel(x, y)
            rgbData[offset++] = color.r.toByte()
            rgbData[offset++] = color.g.toByte()
            rgbData[offset++] = color.b.toByte()
         }
      }

      // Compress with deflate (BEST_SPEED for minimal latency)
      val compressed = deflate(rgbData)

      // Assemble frame: header + compressed data
      val buffer = ByteArray(5 + compressed.size)
      buffer[0] = 0x01 // flags: deflate-compressed
      buffer[1] = (viewport.width shr 8).toByte()
      buffer[2] = (viewport.width and 0xFF).toByte()
      buffer[3] = (viewport.height shr 8).toByte()
      buffer[4] = (viewport.height and 0xFF).toByte()
      System.arraycopy(compressed, 0, buffer, 5, compressed.size)

      return buffer
   }

   /**
    * Compresses data using deflate at BEST_SPEED level.
    */
   private fun deflate(data: ByteArray): ByteArray {
      val deflater = Deflater(Deflater.BEST_SPEED)
      try {
         deflater.setInput(data)
         deflater.finish()
         val output = ByteArray(data.size)
         val compressedSize = deflater.deflate(output)
         return output.copyOf(compressedSize)
      } finally {
         deflater.end()
      }
   }
}
