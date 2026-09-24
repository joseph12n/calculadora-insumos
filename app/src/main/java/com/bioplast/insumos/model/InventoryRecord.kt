package com.bioplast.insumos.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Registro de consumo de un insumo en una fecha concreta.
 *
 * @property id PK autogenerado.
 * @property date Fecha del registro (se persiste como String ISO-8601 `yyyy-MM-dd` vía [com.bioplast.insumos.data.Converters]).
 * @property weekOfYear Semana del año (calendario de [java.time.temporal.WeekFields] del locale por defecto).
 * @property year Año del registro, usado junto con [weekOfYear] para el resumen semanal.
 * @property productName Nombre del insumo (catálogo preestablecido).
 * @property unitPriceCop Precio unitario en pesos colombianos.
 * @property quantity Cantidad registrada.
 * @property totalCop Total = cantidad × precio unitario, en COP.
 */
@Entity(
    tableName = "inventory_records",
    indices = [
        Index(value = ["year", "weekOfYear"]),
        Index(value = ["date"])
    ]
)
data class InventoryRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val date: LocalDate,
    val weekOfYear: Int,
    val year: Int,
    val productName: String,
    val unitPriceCop: Double,
    val quantity: Int,
    val totalCop: Double
)
