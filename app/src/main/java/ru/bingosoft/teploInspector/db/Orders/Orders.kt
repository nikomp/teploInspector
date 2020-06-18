package ru.bingosoft.teploInspector.db.Orders

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName
import ru.bingosoft.teploInspector.util.DateConverter
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
    var address: String? = null,
    @SerializedName("contactFio")
    var contactFio: String? = null,
    @SerializedName("phone")
    var phone: String? = null,
    @SerializedName("state")
    var state: String = "В работе",
    @SerializedName("comment")
    var comment: String? = null,
    @SerializedName("dateCreate")
    var dateCreate: Date? = null,

    @SerializedName("typeOrder")
    var typeOrder: String? = "Тип заявки",

    @SerializedName("typeTransportation")
    var typeTransportation: String? = "Транспортировка выполняется заказчиком",

    @SerializedName("lat")
    var lat: Double = 0.0,
    @SerializedName("lon")
    var lon: Double = 0.0,

    var checked: Boolean=false
)

