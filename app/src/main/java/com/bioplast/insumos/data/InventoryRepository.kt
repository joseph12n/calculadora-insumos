package com.bioplast.insumos.data

import android.content.Context
import com.bioplast.insumos.model.InventoryRecord
import com.bioplast.insumos.model.WeeklySummary
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Repositorio de persistencia local: fachada delgada sobre [InventoryDao].
 *
 * Expone exactamente las mismas firmas que el DAO para que la capa de backend
 * (ViewModel) dependa de esta clase y no del DAO directamente; en tests se
 * construye con el constructor que recibe un DAO (fake o in-memory).
 */
class InventoryRepository(private val dao: InventoryDao) {

    /**
     * Inserta un registro (id autogenerado si es 0) y devuelve el rowId asignado,
     * para que la capa backend conozca el id real y pueda borrarlo (deshacer/borrar).
     */
    suspend fun insertRecord(record: InventoryRecord): Long = dao.insertRecord(record)

    /**
     * Guardado por lote atómico (transacción Room): inserta todos los registros o
     * ninguno, y devuelve los rowIds en orden de inserción.
     */
    suspend fun insertBatch(records: List<InventoryRecord>): List<Long> = dao.insertBatch(records)

    /** Elimina un registro existente (requerido por el botón 🗑 Borrar). */
    suspend fun deleteRecord(record: InventoryRecord) = dao.deleteRecord(record)

    /**
     * Borrado por lote atómico (transacción Room): borra todos los registros
     * dados o ninguno (deshacer del historial sin riesgo de borrado parcial).
     */
    suspend fun deleteBatch(records: List<InventoryRecord>) = dao.deleteBatch(records)

    /** Registros de una fecha concreta (más reciente primero). */
    fun getRecordsByDate(date: LocalDate): Flow<List<InventoryRecord>> =
        dao.getRecordsByDate(date)

    /** Registros de una semana concreta (desglose por día del historial). */
    fun getRecordsByWeek(year: Int, week: Int): Flow<List<InventoryRecord>> =
        dao.getRecordsByWeek(year, week)

    /** Acumulado de una semana; emite null si la semana no tiene registros. */
    fun getWeeklyTotal(year: Int, week: Int): Flow<WeeklySummary?> =
        dao.getWeeklyTotal(year, week)

    /** Historial de todas las semanas, de la más reciente a la más antigua. */
    fun getAllWeeklySummaries(): Flow<List<WeeklySummary>> =
        dao.getAllWeeklySummaries()

    companion object {
        @Volatile
        private var INSTANCE: InventoryRepository? = null

        /** Instancia singleton ligada al singleton de [AppDatabase]. */
        fun getInstance(context: Context): InventoryRepository {
            val existing = INSTANCE
            if (existing != null) return existing
            return synchronized(this) {
                INSTANCE ?: InventoryRepository(
                    AppDatabase.getInstance(context).inventoryDao()
                ).also { INSTANCE = it }
            }
        }
    }
}
