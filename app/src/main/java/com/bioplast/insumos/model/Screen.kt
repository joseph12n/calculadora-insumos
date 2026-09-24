package com.bioplast.insumos.model

/**
 * Pantallas del flujo lineal de 7 pasos, gestionado por la ViewModel.
 *
 * Orden del registro: [START] → [PICK_PRODUCT] (Paso 1) → [ENTER_QUANTITY] (Paso 2)
 * → [CONFIRM] (Paso 3, única vía de guardar) → [SUCCESS] (con Deshacer ~5 s) → [START].
 * Además: [HISTORY] → [DAY_DETAIL] (borrado por fila).
 */
enum class Screen(val paso: Int) {
    /** Inicio: resumen de la semana actual. */
    START(0),

    /** Paso 1: selección del insumo. */
    PICK_PRODUCT(1),

    /** Paso 2: cantidad con teclado numérico propio (y OCR opcional). */
    ENTER_QUANTITY(2),

    /** Paso 3: confirmación; solo aquí se persiste el registro. */
    CONFIRM(3),

    /** Éxito tras guardar, con opción "Deshacer" (~5 s). */
    SUCCESS(0),

    /** Historial de semanas anteriores. */
    HISTORY(0),

    /** Detalle de un día concreto (borrado por fila). */
    DAY_DETAIL(0),
    ;

    /** true si la pantalla pertenece al registro (Pasos 1–3) → mostrar "Paso X de 3". */
    val mostrarIndicadorPaso: Boolean
        get() = paso in 1..3
}
