package com.jay.nyxplay.data

import androidx.room.TypeConverter

class MediaTypeConverters {
    @TypeConverter
    fun fromMediaType(type: MediaType): String = type.name

    @TypeConverter
    fun toMediaType(value: String): MediaType = MediaType.valueOf(value)
}
