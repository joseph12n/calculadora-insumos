package com.bioplast.insumos.model

/**
 * POJO de proyección para el resumen semanal.
 *
 * Resultado de:
 * `SELECT year, weekOfYear, SUM(totalCop), SUM(quantity) ... GROUP BY year, weekOfYear`
 *
 * @property totalQuantity Long porque SUM() en SQLite devuelve INTEGER de 64 bits.
 */
data class WeeklySummary(
    val year: Int,
    val weekOfYear: Int,
    val totalCop: Double,
    val totalQuantity: Long
)
