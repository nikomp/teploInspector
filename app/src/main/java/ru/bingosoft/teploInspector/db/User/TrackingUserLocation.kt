package ru.bingosoft.teploInspector.db.User

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import ru.bingosoft.teploInspector.util.DateConverter
import java.util.*

@Entity(tableName = "TrackingUserLocation")
@TypeConverters(DateConverter::class)
data class TrackingUserLocation (
    //@PrimaryKey(autoGenerate = true)
    //var id: Long = 0,
    @PrimaryKey
    var dateLocation: Date? = null,
    var lat: Double?,
    var lon: Double?,
    var provider: String="",
    var status: String=""
)