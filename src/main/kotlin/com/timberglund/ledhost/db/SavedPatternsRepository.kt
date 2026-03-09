package com.timberglund.ledhost.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import mu.KotlinLogging
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

private val logger = KotlinLogging.logger {}

// ──────────────────────────────────────────────
// Table definition
// ──────────────────────────────────────────────

object SavedPatternsTable : IntIdTable("saved_patterns") {
   val presetName = varchar("preset_name", 255).uniqueIndex()
   val patternName = varchar("pattern_name", 255)
   val params = text("params") // JSON string
   val updatedAt = long("updated_at")
}

// ──────────────────────────────────────────────
// Data model
// ──────────────────────────────────────────────

data class SavedPatternRow(
   val id: Int,
   val presetName: String,
   val patternName: String,
   val params: JsonObject,
   val updatedAt: Long
)

// ──────────────────────────────────────────────
// Repository
// ──────────────────────────────────────────────

class SavedPatternsRepository(private val database: Database) {

   fun createTable() {
      transaction(database) {
         SchemaUtils.createMissingTablesAndColumns(SavedPatternsTable)
         logger.info { "saved_patterns table ready" }
      }
   }

   suspend fun getAllPresets(): List<SavedPatternRow> = withContext(Dispatchers.IO) {
      transaction(database) {
         SavedPatternsTable
            .selectAll()
            .orderBy(SavedPatternsTable.presetName to SortOrder.ASC)
            .map { it.toRow() }
      }
   }

   suspend fun createPreset(
      presetName: String,
      patternName: String,
      params: JsonObject
   ): SavedPatternRow = withContext(Dispatchers.IO) {
      if(presetName.isBlank()) throw IllegalArgumentException("Preset name cannot be blank")
      val now = System.currentTimeMillis()
      try {
         val id = transaction(database) {
            SavedPatternsTable.insertAndGetId {
               it[SavedPatternsTable.presetName] = presetName
               it[SavedPatternsTable.patternName] = patternName
               it[SavedPatternsTable.params] = Json.encodeToString(params)
               it[SavedPatternsTable.updatedAt] = now
            }.value
         }
         SavedPatternRow(id = id, presetName = presetName, patternName = patternName, params = params, updatedAt = now)
      }
      catch(e: ExposedSQLException) {
         if(e.isUniqueViolation()) throw IllegalArgumentException("A preset named '$presetName' already exists")
         throw e
      }
   }

   suspend fun updatePreset(
      presetId: Int,
      presetName: String? = null,
      patternName: String? = null,
      params: JsonObject? = null
   ): SavedPatternRow? = withContext(Dispatchers.IO) {
      val now = System.currentTimeMillis()
      try {
         transaction(database) {
            val updated = SavedPatternsTable.update({ SavedPatternsTable.id eq presetId }) {
               if(presetName != null) it[SavedPatternsTable.presetName] = presetName
               if(patternName != null) it[SavedPatternsTable.patternName] = patternName
               if(params != null) it[SavedPatternsTable.params] = Json.encodeToString(params)
               it[SavedPatternsTable.updatedAt] = now
            }
            if(updated == 0) return@transaction null
            SavedPatternsTable
               .selectAll().where { SavedPatternsTable.id eq presetId }
               .firstOrNull()?.toRow()
         }
      }
      catch(e: ExposedSQLException) {
         if(e.isUniqueViolation()) throw IllegalArgumentException("A preset with that name already exists")
         throw e
      }
   }

   suspend fun deletePreset(presetId: Int): Boolean = withContext(Dispatchers.IO) {
      transaction(database) {
         SavedPatternsTable.deleteWhere { SavedPatternsTable.id eq presetId } > 0
      }
   }

   private fun ResultRow.toRow(): SavedPatternRow {
      val row = this
      return SavedPatternRow(
         id = row[SavedPatternsTable.id].value,
         presetName = row[SavedPatternsTable.presetName],
         patternName = row[SavedPatternsTable.patternName],
         params = Json.decodeFromString<JsonObject>(row[SavedPatternsTable.params]),
         updatedAt = row[SavedPatternsTable.updatedAt]
      )
   }
}

private fun ExposedSQLException.isUniqueViolation(): Boolean =
   (cause as? java.sql.SQLException)?.sqlState == "23505" ||
   message?.contains("unique", ignoreCase = true) == true
