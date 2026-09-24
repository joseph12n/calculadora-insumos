package com.bioplast.insumos.model

/**
 * Pantallas del flujo, gestionado por la ViewModel.
 *
 * Orden del registro: [START] → [CALCULATOR] (insumo + cantidad + total en
 * vivo, una sola pantalla tipo calculadora) → [CONFIRM] (revisar y guardar)
 * → [SUCCESS] (con Deshacer ~5 s) → [START].
 * Además: [HISTORY] → [DAY_DETAIL] (borrado por fila).
 */
enum class Screen {
    /** Inicio: resumen de la semana actual. */
    START,

    /** Captura: insumo + cantidad con teclado propio y total en vivo. */
    CALCULATOR,

    /** Revisión del lote; solo aquí se persiste. */
    CONFIRM,

    /** Éxito tras guardar, con opción "Deshacer" (~5 s). */
    SUCCESS,

    /** Historial de semanas anteriores. */
    HISTORY,

    /** Detalle de una semana concreta (borrado por fila). */
    DAY_DETAIL,
}
