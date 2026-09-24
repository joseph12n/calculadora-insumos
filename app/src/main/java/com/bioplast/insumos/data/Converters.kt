package com.bioplast.insumos.data

import androidx.room.TypeConverter
import java.time.LocalDate

/**
 * TypeConverters de Room.
 *
 * `LocalDate` se persiste como String ISO-8601 (`yyyy-MM-dd`),
 * que es exactamente el formato de [LocalDate.toString] y [LocalDate.parse].
 */
class Converters {

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.toString() // ISO-8601: yyyy-MM-dd

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)
}
