package com.timberglund.ledhost

import com.timberglund.ledstrip.TelemetryReading

class TelemetryStore {
   private val history: MutableMap<Int, ArrayDeque<TelemetryReading>> = mutableMapOf()

   fun record(stripId: Int, reading: TelemetryReading) {
      val buf = history.getOrPut(stripId) { ArrayDeque(MAX_HISTORY + 1) }
      if(buf.size >= MAX_HISTORY) buf.removeFirst()
      buf.addLast(reading)
   }

   fun getHistory(stripId: Int): List<TelemetryReading> =
      history[stripId]?.toList() ?: emptyList()

   companion object {
      const val MAX_HISTORY = 150
   }
}
