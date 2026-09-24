package com.bioplast.insumos.data

import android.content.Context
import androidx.room.Room
import com.bioplast.insumos.model.InventoryRecord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.time.LocalDate

/**
 * Especificación de las consultas de [InventoryDao] sobre Room IN-MEMORY.
 *
 * ⚠️ IGNORADO EN LA JVM: `Room.inMemoryDatabaseBuilder` necesita un `Context` real
 * con el SQLite de Android (android.database.sqlite), que NO existe en los unit
 * tests puros de JVM; sin Robolectric (fuera del alcance de la fase 1: solo se
 * permite añadir `kotlinx-coroutines-test` al build) no puede ejecutarse aquí.
 *
 * La COPIA EJECUTABLE vive en `app/src/androidTest/java/.../data/InventoryDaoTest.kt`
 * y corre con `./gradlew :app:connectedAndroidTest` (emulador o dispositivo).
 * Este archivo se conserva como especificación compilada junto al resto de unit tests.
 */
@Ignore(
    "Room in-memory requiere el SQLite de Android (Context real). " +
        "Ejecutar la copia de app/src/androidTest/ con connectedAndroidTest, " +
        "o habilitar Robolectric en una fase posterior.",
)
class InventoryDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: InventoryDao

    private fun contextoAndroid(): Context = throw UnsupportedOperationException(
        "Los unit tests de JVM no tienen Context de Android ni SQLite real. " +
            "Usa la copia de app/src/androidTest/ (connectedAndroidTest) o Robolectric.",
    )

    @Before
    fun setUp() {
        val context = contextoAndroid()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.inventoryDao()
    }

    // Fechas fijas: las consultas del DAO filtran por los campos GUARDADOS
    // (year/weekOfYear), así que los tests son deterministas sin depender del locale.
    private fun registro(
        fecha: LocalDate = LocalDate.of(2026, 3, 2),
        anio: Int = 2026,
        semana: Int = 10,
        producto: String = "Frasco de orina",
        precio: Double = 7.18,
        cantidad: Int = 1,
        total: Double = 7.18,
    ) = InventoryRecord(
        date = fecha,
        weekOfYear = semana,
        year = anio,
        productName = producto,
        unitPriceCop = precio,
        quantity = cantidad,
        totalCop = total,
    )

    @Test
    fun insertDevuelveIdPositivoYElRegistroEsRecuperablePorId() = runTest {
        val id = dao.insertRecord(registro())
        assertTrue("insertRecord debe devolver el rowId autogenerado (> 0)", id > 0)

        val recuperado = dao.getRecordsByDate(LocalDate.of(2026, 3, 2)).first()
        assertEquals(1, recuperado.size)
        assertEquals(id, recuperado.first().id) // id real → el Deshacer puede borrar por PK
    }

    @Test
    fun getRecordsByDateFiltraPorFechaYOrdenaDesc() = runTest {
        val fecha = LocalDate.of(2026, 3, 2)
        val id1 = dao.insertRecord(registro(fecha = fecha, producto = "Primero"))
        val id2 = dao.insertRecord(registro(fecha = fecha, producto = "Segundo"))
        dao.insertRecord(registro(fecha = LocalDate.of(2026, 3, 3), producto = "Otro día"))

        val delDia = dao.getRecordsByDate(fecha).first()
        assertEquals(2, delDia.size)
        assertEquals(id2, delDia[0].id) // ORDER BY id DESC: el más reciente primero
        assertEquals(id1, delDia[1].id)
        assertEquals("Segundo", delDia[0].productName)
    }

    @Test
    fun getWeeklyTotalEsNuloParaSemanaVacia() = runTest {
        assertNull(dao.getWeeklyTotal(2026, 99).first())
    }

    @Test
    fun getWeeklyTotalSumaTotalesYCantidades() = runTest {
        dao.insertRecord(registro(cantidad = 2, total = 10.0))
        dao.insertRecord(registro(fecha = LocalDate.of(2026, 3, 3), cantidad = 3, total = 20.5))
        // Otra semana NO debe mezclarse:
        dao.insertRecord(registro(semana = 11, cantidad = 9, total = 99.0))

        val resumen = dao.getWeeklyTotal(2026, 10).first()
        assertNotNull("La semana 10 tiene registros: no debe ser null", resumen)
        resumen!!
        assertEquals(2026, resumen.year)
        assertEquals(10, resumen.weekOfYear)
        assertEquals(30.5, resumen.totalCop, 0.0)
        assertEquals(5L, resumen.totalQuantity)
    }

    @Test
    fun getAllWeeklySummariesOrdenaDeLaSemanaMasRecienteALaMasAntigua() = runTest {
        dao.insertRecord(registro(semana = 1, total = 1.0))
        dao.insertRecord(registro(semana = 10, total = 2.0))
        dao.insertRecord(registro(anio = 2025, semana = 52, total = 3.0))

        val resumenes = dao.getAllWeeklySummaries().first()
        assertEquals(3, resumenes.size)
        // ORDER BY year DESC, weekOfYear DESC:
        assertEquals(2026 to 10, resumenes[0].year to resumenes[0].weekOfYear)
        assertEquals(2026 to 1, resumenes[1].year to resumenes[1].weekOfYear)
        assertEquals(2025 to 52, resumenes[2].year to resumenes[2].weekOfYear)
    }

    @Test
    fun getRecordsByWeekFiltraAnioYSemana() = runTest {
        dao.insertRecord(registro(fecha = LocalDate.of(2026, 3, 5), producto = "De la semana"))
        dao.insertRecord(registro(fecha = LocalDate.of(2026, 3, 2), producto = "También"))
        dao.insertRecord(registro(semana = 11, producto = "Otra semana"))
        dao.insertRecord(registro(anio = 2025, producto = "Otro año"))

        val deLaSemana = dao.getRecordsByWeek(2026, 10).first()
        assertEquals(2, deLaSemana.size)
        // ORDER BY date ASC, id ASC:
        assertEquals(LocalDate.of(2026, 3, 2), deLaSemana[0].date)
        assertEquals("También", deLaSemana[0].productName)
        assertEquals(LocalDate.of(2026, 3, 5), deLaSemana[1].date)
    }

    @Test
    fun deleteRecordEliminaSoloElRegistroIndicado() = runTest {
        val fecha = LocalDate.of(2026, 3, 2)
        val idBorrable = dao.insertRecord(registro(fecha = fecha, producto = "Bórrame"))
        dao.insertRecord(registro(fecha = fecha, producto = "Déjame"))

        // Regresión del bug corregido: borrar debe usar el id REAL devuelto por insert.
        val filas = dao.getRecordsByDate(fecha).first()
        val aBorrar = filas.first { it.id == idBorrable }
        dao.deleteRecord(aBorrar)

        val restantes = dao.getRecordsByDate(fecha).first()
        assertEquals(1, restantes.size)
        assertEquals("Déjame", restantes.single().productName)
    }
}
