package com.bioplast.insumos.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.bioplast.insumos.model.InventoryRecord

/**
 * Base de datos Room local de la app (100% local, sin backend).
 *
 * - Esquema exportado a `app/schemas/` (versionado en el repo) gracias a `exportSchema = true`
 *   y `room.schemaDirectory` en `app/build.gradle.kts`.
 * - Migraciones: **siempre explícitas**. Al subir [VERSION] hay que añadir un `Migration`
 *   en [MIGRATIONS] (nunca usar `fallbackToDestructiveMigration()` sobre datos reales).
 */
@Database(
    entities = [InventoryRecord::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun inventoryDao(): InventoryDao

    companion object {
        const val DATABASE_NAME = "calculadora_insumos.db"

        /** Migraciones explícitas por cada salto de versión (vacío en la versión 1). */
        private val MIGRATIONS: Array<androidx.room.migration.Migration> = emptyArray()

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Instancia singleton thread-safe (double-checked lock con @Volatile).
         */
        fun getInstance(context: Context): AppDatabase {
            val existing = INSTANCE
            if (existing != null) return existing
            return synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(*MIGRATIONS)
                // Sin fallbackToDestructiveMigration(): los datos reales del usuario nunca se destruyen.
                .build()
        }
    }
}
