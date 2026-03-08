package com.timberglund.ledhost.db

import com.timberglund.ledhost.config.Configuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

private val logger = KotlinLogging.logger {}

// ──────────────────────────────────────────────
// Table definitions
// ──────────────────────────────────────────────

object SettingsTable : Table("settings") {
   val key = varchar("key", 255)
   val value = text("value")
   override val primaryKey = PrimaryKey(key)
}

object StripsTable : IntIdTable("strips") {
   val btName = varchar("bt_name", 255)
   val length = integer("length").nullable()
   val startX = integer("start_x").nullable()
   val startY = integer("start_y").nullable()
   val endX = integer("end_x").nullable()
   val endY = integer("end_y").nullable()
   val reverse = bool("reverse").default(false)
}

object BackgroundImageTable : Table("background_image") {
   val id = integer("id").default(1)
   val data = binary("data")
   val mimeType = varchar("mime_type", 100)
   val updatedAt = long("updated_at")
   override val primaryKey = PrimaryKey(id)
}

// ──────────────────────────────────────────────
// Data model
// ──────────────────────────────────────────────

data class StripRow(
   val id: Int,
   val btName: String,
   val length: Int?,
   val startX: Int?,
   val startY: Int?,
   val endX: Int?,
   val endY: Int?,
   val reverse: Boolean
)

// ──────────────────────────────────────────────
// Repository
// ──────────────────────────────────────────────

class SettingsRepository {
   private lateinit var database: Database
   private var cacheFilePath: String = ""

   // ── Connection & schema creation ──────────────────────────────────────

   fun connect(url: String, user: String, password: String) {
      try {
         database = Database.connect(url, driver = "org.postgresql.Driver", user = user, password = password)
         transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(SettingsTable, StripsTable, BackgroundImageTable)
         }
         logger.info { "Database connected: $url" }
         regenerateCacheIfNeeded()
      }
      catch(e: Exception) {
         logger.error { "Failed to connect to database at $url: ${e.message}" }
         throw e
      }
   }

   // ── Scalar settings ───────────────────────────────────────────────────

   suspend fun getSetting(key: String): String? = withContext(Dispatchers.IO) {
      transaction(database) {
         SettingsTable
            .selectAll().where { SettingsTable.key eq key }
            .firstOrNull()
            ?.get(SettingsTable.value)
      }
   }

   suspend fun setSetting(key: String, value: String) = withContext(Dispatchers.IO) {
      transaction(database) {
         SettingsTable.upsert {
            it[SettingsTable.key] = key
            it[SettingsTable.value] = value
         }
      }
   }

   // ── Strips CRUD ───────────────────────────────────────────────────────

   suspend fun getAllStrips(): List<StripRow> = withContext(Dispatchers.IO) {
      transaction(database) {
         StripsTable
            .selectAll()
            .orderBy(StripsTable.id)
            .map { row ->
               StripRow(
                  id = row[StripsTable.id].value,
                  btName = row[StripsTable.btName],
                  length = row[StripsTable.length],
                  startX = row[StripsTable.startX],
                  startY = row[StripsTable.startY],
                  endX = row[StripsTable.endX],
                  endY = row[StripsTable.endY],
                  reverse = row[StripsTable.reverse]
               )
            }
      }
   }

   suspend fun createStrip(
      btName: String,
      length: Int?,
      startX: Int?,
      startY: Int?,
      endX: Int?,
      endY: Int?,
      reverse: Boolean
   ): Int = withContext(Dispatchers.IO) {
      transaction(database) {
         StripsTable.insertAndGetId {
            it[StripsTable.btName] = btName
            it[StripsTable.length] = length
            it[StripsTable.startX] = startX
            it[StripsTable.startY] = startY
            it[StripsTable.endX] = endX
            it[StripsTable.endY] = endY
            it[StripsTable.reverse] = reverse
         }.value
      }
   }

   suspend fun updateStrip(
      id: Int,
      btName: String?,
      length: Int?,
      startX: Int?,
      startY: Int?,
      endX: Int?,
      endY: Int?,
      reverse: Boolean?
   ): Boolean = withContext(Dispatchers.IO) {
      transaction(database) {
         val updated = StripsTable.update({ StripsTable.id eq id }) {
            if(btName != null) it[StripsTable.btName] = btName
            if(length != null) it[StripsTable.length] = length
            if(startX != null) it[StripsTable.startX] = startX
            if(startY != null) it[StripsTable.startY] = startY
            if(endX != null) it[StripsTable.endX] = endX
            if(endY != null) it[StripsTable.endY] = endY
            if(reverse != null) it[StripsTable.reverse] = reverse
         }
         updated > 0
      }
   }

   suspend fun deleteStrip(id: Int): Boolean = withContext(Dispatchers.IO) {
      transaction(database) {
         StripsTable.deleteWhere { StripsTable.id eq id } > 0
      }
   }

   // ── Background image ──────────────────────────────────────────────────

   suspend fun setBackgroundImage(bytes: ByteArray, mimeType: String) {
      val ext = mimeTypeToExtension(mimeType)
      val newCachePath = "./bg-image-cache.$ext"
      withContext(Dispatchers.IO) {
         transaction(database) {
            BackgroundImageTable.upsert {
               it[id] = 1
               it[data] = bytes
               it[BackgroundImageTable.mimeType] = mimeType
               it[updatedAt] = System.currentTimeMillis()
            }
         }
         writeCacheFile(bytes, newCachePath)
      }
      cacheFilePath = newCachePath
   }

   suspend fun getBackgroundImage(): Pair<ByteArray, String>? = withContext(Dispatchers.IO) {
      transaction(database) {
         BackgroundImageTable
            .selectAll().where { BackgroundImageTable.id eq 1 }
            .firstOrNull()
            ?.let { row ->
               Pair(row[BackgroundImageTable.data], row[BackgroundImageTable.mimeType])
            }
      }
   }

   suspend fun deleteBackgroundImage() {
      withContext(Dispatchers.IO) {
         transaction(database) {
            BackgroundImageTable.deleteWhere { id eq 1 }
         }
         if(cacheFilePath.isNotEmpty()) {
            val file = File(cacheFilePath)
            if(file.exists()) file.delete()
         }
      }
      cacheFilePath = ""
   }

   fun getCacheFilePath(): String? =
      cacheFilePath.takeIf { it.isNotEmpty() && File(it).exists() }

   // ── Cache regeneration (called at connect() time) ─────────────────────

   private fun regenerateCacheIfNeeded() {
      data class ImageRow(val bytes: ByteArray, val mimeType: String, val updatedAt: Long)

      val row = transaction(database) {
         BackgroundImageTable
            .selectAll().where { BackgroundImageTable.id eq 1 }
            .firstOrNull()
            ?.let { r ->
               ImageRow(
                  bytes = r[BackgroundImageTable.data],
                  mimeType = r[BackgroundImageTable.mimeType],
                  updatedAt = r[BackgroundImageTable.updatedAt]
               )
            }
      } ?: return

      val ext = mimeTypeToExtension(row.mimeType)
      val cachePath = "./bg-image-cache.$ext"
      val cacheFile = File(cachePath)

      if(!cacheFile.exists() || cacheFile.lastModified() < row.updatedAt) {
         writeCacheFile(row.bytes, cachePath)
         logger.info { "Background image cache regenerated from database at $cachePath" }
      }
      cacheFilePath = cachePath
   }

   private fun writeCacheFile(bytes: ByteArray, path: String) {
      File(path).writeBytes(bytes)
      logger.info { "Background image cache written to $path" }
   }

   // ── Seed from config.yaml on first run ────────────────────────────────

   fun seedFromConfig(config: Configuration) {
      val settingsEmpty = transaction(database) { SettingsTable.selectAll().empty() }
      val stripsEmpty = transaction(database) { StripsTable.selectAll().empty() }

      if(!settingsEmpty || !stripsEmpty) return

      logger.warn {
         "Empty database detected — seeding from config.yaml. " +
         "Non-connection fields in config.yaml will be ignored on future starts."
      }

      transaction(database) {
         SettingsTable.insert { it[key] = "viewportWidth"; it[value] = config.viewport.width.toString() }
         SettingsTable.insert { it[key] = "viewportHeight"; it[value] = config.viewport.height.toString() }
         SettingsTable.insert { it[key] = "targetFPS"; it[value] = config.targetFPS.toString() }
         SettingsTable.insert { it[key] = "scanIntervalSeconds"; it[value] = config.scanIntervalSeconds.toString() }
      }

      for(strip in config.strips) {
         transaction(database) {
            StripsTable.insert {
               it[btName] = "strip${strip.id}"
               it[length] = strip.length
               it[startX] = strip.position.start.x
               it[startY] = strip.position.start.y
               it[endX] = strip.position.end.x
               it[endY] = strip.position.end.y
               it[reverse] = strip.reverse
            }
         }
      }

      if(config.backgroundImage.isNotEmpty()) {
         val bgFile = File(config.backgroundImage)
         if(bgFile.exists() && bgFile.isFile) {
            val bytes = bgFile.readBytes()
            val ext = bgFile.extension.lowercase()
            val mimeType = when(ext) {
               "jpg", "jpeg" -> "image/jpeg"
               "png" -> "image/png"
               "gif" -> "image/gif"
               "webp" -> "image/webp"
               else -> "image/jpeg"
            }
            transaction(database) {
               BackgroundImageTable.upsert {
                  it[id] = 1
                  it[data] = bytes
                  it[BackgroundImageTable.mimeType] = mimeType
                  it[updatedAt] = System.currentTimeMillis()
               }
            }
            writeCacheFile(bytes, "./bg-image-cache.$ext")
            cacheFilePath = "./bg-image-cache.$ext"
            logger.info { "Seeded background image from ${config.backgroundImage}" }
         }
      }
   }

   // ── Helpers ───────────────────────────────────────────────────────────

   private fun mimeTypeToExtension(mimeType: String): String =
      when(mimeType.lowercase()) {
         "image/jpeg", "image/jpg" -> "jpg"
         "image/png" -> "png"
         "image/gif" -> "gif"
         "image/webp" -> "webp"
         else -> mimeType.substringAfter("/").lowercase().replace("jpeg", "jpg")
      }
}
