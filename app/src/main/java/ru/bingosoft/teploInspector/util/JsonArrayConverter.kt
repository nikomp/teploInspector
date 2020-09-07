package ru.bingosoft.teploInspector.util

import androidx.room.TypeConverter
import com.google.gson.JsonArray
import com.google.gson.JsonParser

class JsonArrayConverter {
    @TypeConverter
    fun fromString(value: String?): JsonArray? {

        return if (value == null) null else JsonParser().parse(value).asJsonArray
    }

    @TypeConverter
    fun jsonToString(jsonObject: JsonArray?): String? {
        return jsonObject?.toString()
    }
}