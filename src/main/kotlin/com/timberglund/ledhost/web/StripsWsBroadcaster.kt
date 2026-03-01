package com.timberglund.ledhost.web

import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val stripsJson = Json { encodeDefaults = true }

class StripsWsBroadcaster {
   private val clients = mutableListOf<WebSocketSession>()
   private val lock = Any()

   fun addClient(session: WebSocketSession) {
      synchronized(lock) {
         clients.add(session)
      }
   }

   fun removeClient(session: WebSocketSession) {
      synchronized(lock) {
         clients.remove(session)
      }
   }

   suspend fun broadcast(message: StripsUpdateMessage) = broadcastText(stripsJson.encodeToString(message))
   suspend fun broadcast(message: DiscoveryEventMessage) = broadcastText(stripsJson.encodeToString(message))

   suspend fun sendTo(session: WebSocketSession, message: StripsUpdateMessage) {
      sendTextTo(session, stripsJson.encodeToString(message))
   }

   private suspend fun broadcastText(text: String) {
      val frame = Frame.Text(text)
      val snapshot = synchronized(lock) { clients.toList() }
      val failed = mutableListOf<WebSocketSession>()
      for(client in snapshot) {
         try {
            client.send(frame)
         }
         catch(e: ClosedSendChannelException) {
            failed.add(client)
         }
         catch(e: Exception) {
            failed.add(client)
         }
      }
      if(failed.isNotEmpty()) {
         synchronized(lock) { clients.removeAll(failed) }
      }
   }

   private suspend fun sendTextTo(session: WebSocketSession, text: String) {
      try {
         session.send(Frame.Text(text))
      }
      catch(e: Exception) {
         synchronized(lock) { clients.remove(session) }
      }
   }
}
