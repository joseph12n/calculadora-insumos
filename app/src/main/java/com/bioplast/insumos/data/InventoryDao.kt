package com.bioplast.insumos.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.bioplast.insumos.model.InventoryRecord
import com.bioplast.insumos.model.WeeklySummary
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * DAO de registros de inventario.
 *
 * Todas las consultas frecuentes (resumen semanal por `(year, weekOfYear)` y
 * desglose por día en `date`) se benefician de los índices definidos en
 * [InventoryRecord].
 */
@Dao
interface InventoryDao {

    /**
     * Inserta un registro. Si [InventoryRecord.id] es 0 (por defecto) Room autogenera el id
     * y lo devuelve como rowId (Long), permitiendo borrarlo después por primary key
     * (el deshacer depende de conocer el id recién insertado).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: InventoryRecord): Long

    /**
     * Inserta un lote de registros en una sola sentencia y devuelve sus rowIds
     * en orden de inserción (Room garantiza el orden de la lista devuelta).
     * Los ids autogenerados permiten borrar cada registro después por primary key.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<InventoryRecord>): List<Long>

    /**
     * Guardado por lote **atómico**: ejecuta [insertAll] dentro de una transacción
     * de Room, de modo que o se insertan todos los registros o ninguno
     * (sin rollback best-effort). Devuelve los rowIds en orden de inserción.
     */
    @Transaction
    suspend fun insertBatch(records: List<InventoryRecord>): List<Long> = insertAll(records)

    /**
     * Elimina el registro indicado (botón 🗑 Borrar del historial con confirmación).
     * Coincide por primary key ([InventoryRecord.id]).
     */
    @Delete
    suspend fun deleteRecord(record: InventoryRecord)

    /**
     * Elimina un lote de registros coincidiendo por primary key ([InventoryRecord.id]).
     * Preferir [deleteBatch], que garantiza atomicidad.
     */
    @Delete
    suspend fun deleteAll(records: List<InventoryRecord>)

    /**
     * Borrado por lote **atómico**: ejecuta [deleteAll] dentro de una transacción
     * de Room, de modo que o se borran todos los registros o ninguno
     * (evita el borrado parcial del deshacer registro a registro).
     */
    @Transaction
    suspend fun deleteBatch(records: List<InventoryRecord>) = deleteAll(records)

    /**
     * Registros de una fecha concreta, ordenados del más reciente al más antiguo.
     * El parámetro se convierte a String ISO-8601 vía [Converters].
     */
    @Query("SELECT * FROM inventory_records WHERE date = :date ORDER BY id DESC")
    fun getRecordsByDate(date: LocalDate): Flow<List<InventoryRecord>>

    /**
     * Registros de una semana concreta (año + número de semana), ordenados por fecha.
     * Útil para el desglose por día de la vista de historial.
     */
    @Query(
        """
        SELECT * FROM inventory_records
        WHERE year = :year AND weekOfYear = :week
        ORDER BY date ASC, id ASC
        """
    )
    fun getRecordsByWeek(year: Int, week: Int): Flow<List<InventoryRecord>>

    /**
     * Acumulado de una semana. Devuelve `null` (Flow emite null) si la semana no tiene registros,
     * porque una consulta con GROUP BY sin filas de entrada no produce filas de salida.
     * COALESCE protege frente a SUM() sobre valores nulos.
     */
    @Query(
        """
        SELECT year,
               weekOfYear,
               COALESCE(SUM(totalCop), 0.0) AS totalCop,
               COALESCE(SUM(quantity), 0) AS totalQuantity
        FROM inventory_records
        WHERE year = :year AND weekOfYear = :week
        GROUP BY year, weekOfYear
        """
    )
    fun getWeeklyTotal(year: Int, week: Int): Flow<WeeklySummary?>

    /**
     * Historial de todas las semanas con registros, de la más reciente a la más antigua.
     */
    @Query(
        """
        SELECT year,
               weekOfYear,
               COALESCE(SUM(totalCop), 0.0) AS totalCop,
               COALESCE(SUM(quantity), 0) AS totalQuantity
        FROM inventory_records
        GROUP BY year, weekOfYear
        ORDER BY year DESC, weekOfYear DESC
        """
    )
    fun getAllWeeklySummaries(): Flow<List<WeeklySummary>>
}
