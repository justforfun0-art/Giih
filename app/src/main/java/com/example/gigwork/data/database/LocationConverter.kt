// data/database/converters/LocationConverter.kt
package com.example.gigwork.data.database

import androidx.room.TypeConverter
import com.example.gigwork.domain.models.Location
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class LocationConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromLocation(location: Location): String {
        return gson.toJson(location)
    }

    @TypeConverter
    fun toLocation(locationString: String): Location {
        val type = object : TypeToken<Location>() {}.type
        return gson.fromJson(locationString, type)
    }
}