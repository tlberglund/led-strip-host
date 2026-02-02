package com.timberglund.ledhost.web

import com.timberglund.ledhost.viewport.Viewport
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Base64

/**
 * Message containing viewport state for WebSocket transmission.
 * Uses Base64-encoded bitmap for efficiency.
 */
@Serializable
data class ViewportMessage(
   val type: String = "viewport",
   val width: Int,
   val height: Int,
   val data: String // Base64-encoded RGBA bitmap
)

/**
 * Manages WebSocket connections and broadcasts viewport updates to all connected clients.
 */
class WebSocketBroadcaster {
   private val clients = mutableListOf<WebSocketSession>()
   private val lock = Any()
   private val json = Json { encodeDefaults = true }

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
      val message = Frame.Text(data)

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
    * Encodes the viewport as JSON with Base64-encoded RGBA bitmap.
    */
   private fun encodeViewport(viewport: Viewport): String {
      val pixelCount = viewport.width * viewport.height
      val buffer = ByteArray(pixelCount * 4) // RGBA: 4 bytes per pixel

      var offset = 0
      for(y in 0 until viewport.height) {
         for(x in 0 until viewport.width) {
            val color = viewport.getPixel(x, y)
            buffer[offset++] = color.r.toByte()
            buffer[offset++] = color.g.toByte()
            buffer[offset++] = color.b.toByte()
            buffer[offset++] = 255.toByte() // Alpha channel (fully opaque)
         }
      }

      val base64Data = Base64.getEncoder().encodeToString(buffer)

      val message = ViewportMessage(
         width = viewport.width,
         height = viewport.height,
         data = base64Data
      )

      return json.encodeToString(message)
   }
}
