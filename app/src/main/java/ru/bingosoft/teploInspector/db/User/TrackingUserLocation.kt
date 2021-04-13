package ru.bingosoft.teploInspector.db.User

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.Expose
import ru.bingosoft.teploInspector.util.DateConverter
import java.util.*

@Entity(tableName = "TrackingUserLocation")
@TypeConverters(DateConverter::class)
data class TrackingUserLocation (
    //@PrimaryKey(autoGenerate = true)
    //var id: Long = 0,
    @PrimaryKey
    @Expose var dateLocation: Date = Date(), // было Date?=null
    @Expose var lat: Double?,
    @Expose var lon: Double?,
    @Expose var provider: String="",
    @Expose var status: String="",

    @Expose(serialize = false, deserialize = false) var synced: Boolean=false
)