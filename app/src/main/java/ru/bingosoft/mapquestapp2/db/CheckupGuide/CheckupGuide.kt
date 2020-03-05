package ru.bingosoft.mapquestapp2.db.CheckupGuide

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.JsonObject
import ru.bingosoft.mapquestapp2.util.JsonConverter

@Entity(tableName = "CheckupGuide")
@TypeConverters(JsonConverter::class)
data class CheckupGuide (
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var guid: String,
    var kindCheckup: String,
    var text: JsonObject
)


