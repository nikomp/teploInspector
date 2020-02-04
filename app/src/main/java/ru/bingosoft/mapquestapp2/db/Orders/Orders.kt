package ru.bingosoft.mapquestapp2.db.Orders

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName
import ru.bingosoft.mapquestapp2.util.DateConverter
import java.util.*

@Entity(tableName = "Orders")
@TypeConverters(DateConverter::class)
data class Orders (
    @PrimaryKey(autoGenerate = true)
    @SerializedName("id")
    var id: Long = 0,
    @SerializedName("guid")
    var guid: String,
    @SerializedName("number")
    var number: String? = null,
    @SerializedName("name")
    var name: String? = null,
    @SerializedName("adress")
    var adress: String? = null,
    @SerializedName("contactFio")
    var contactFio: String? = null,
    @SerializedName("phone")
    var phone: String? = null,
    @SerializedName("state")
    var state: String? = null,
    @SerializedName("comment")
    var comment: String? = null,
    @SerializedName("dateCreate")
    var dateCreate: Date? = null,

    @SerializedName("lat")
    var lat: Double = 0.0,
    @SerializedName("lon")
    var lon: Double = 0.0,

    var checked: Boolean=false
)

