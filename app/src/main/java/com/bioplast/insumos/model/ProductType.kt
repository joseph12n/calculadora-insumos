package com.bioplast.insumos.model

/**
 * Catálogo cerrado de los 5 insumos de laboratorio.
 *
 * Los precios son literales en COP y viven solo en memoria: al guardar un registro
 * se persiste una copia del precio (campo `unitPriceCop` de InventoryRecord), de modo
 * que un cambio futuro del catálogo no altere los registros ya guardados.
 *
 * **Persistencia de producto:** guarda [displayName] en el campo `productName`
 * (ej. `"Frasco de orina"`) y reviértelo con [fromNombre]. Como alternativa estable
 * también existe [byId], que usa el nombre de la constante del enum (ej. `"FRASCO_ORINA"`).
 */
enum class ProductType(
    val displayName: String,
    val unitPriceCop: Double,
    val emoji: String,
) {
    FRASCO_ORINA("Frasco de orina", 7.18, "🧪"),
    CRYOTUBO("Cryotubo", 7.90, "🔬"),
    CAJA_COPROLOGICA("Caja coprológica", 5.00, "📦"),
    BACILOSCOPIA("Baciloscopia", 9.88, "🔎"),
    FALCON("Falcon", 7.90, "🧫"),
    ;

    /**
     * Regla de negocio: `totalCop = cantidad × precioUnitario`.
     *
     * El resultado puede traer ruido binario de Double (p. ej. `11 × 7.18 =
     * 78.97999999999999`); el redondeo a 2 decimales se aplica al mostrar,
     * con [CurrencyFormat.formatConDecimalesRedondeados], y al persistir el total.
     */
    fun totalPara(cantidad: Int): Double = cantidad * unitPriceCop

    companion object {

        /**
         * Recupera el insumo a partir del texto persistido en `productName`.
         *
         * Acepta tanto [displayName] (caso principal) como el nombre de la constante
         * (`"FRASCO_ORINA"`), ignorando mayúsculas/minúsculas y espacios sobrantes.
         * Devuelve `null` si no hay coincidencia (p. ej. registro de una versión vieja).
         */
        fun fromNombre(nombre: String?): ProductType? {
            val limpio = nombre?.trim().orEmpty()
            if (limpio.isEmpty()) return null
            return entries.firstOrNull { it.displayName.equals(limpio, ignoreCase = true) }
                ?: entries.firstOrNull { it.name.equals(limpio, ignoreCase = true) }
        }

        /**
         * Busca por id estable: el nombre de la constante del enum
         * (p. ej. `"FRASCO_ORINA"`, `"CAJA_COPROLOGICA"`).
         * Devuelve `null` si el id no existe; no relanza excepciones.
         */
        fun byId(id: String?): ProductType? {
            val limpio = id?.trim().orEmpty()
            if (limpio.isEmpty()) return null
            return entries.firstOrNull { it.name.equals(limpio, ignoreCase = true) }
        }
    }
}
