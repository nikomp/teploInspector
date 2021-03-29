package ru.bingosoft.teploInspector.db.AddLoad

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import ru.bingosoft.teploInspector.db.Orders.Orders

@Entity(tableName = "AddLoad",
    foreignKeys = [ForeignKey(
        entity = Orders::class,
        parentColumns = ["id"],
        childColumns = ["idOrder"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("idOrder")]
)
data class AddLoad (
    @PrimaryKey
    @SerializedName("id")
    var id: Long=0,
    @SerializedName("guid")
    var guid: String="",
    @SerializedName("code")
    var code: Int=0,
    @SerializedName("address")
    var address: String="",
    @SerializedName("purpose")
    var purpose: String,
    @SerializedName("contractor")
    var contractor: String?=null,
    @SerializedName("affiliation")
    var affiliation: String?=null,
    @SerializedName("system_consumption")
    var system_consumption: String="",
    @SerializedName("loading")
    var loading: Double=0.0,
    @SerializedName("order_id")
    var idOrder: Long=0,
    @SerializedName("order_guid")
    var guidOrder: String=""

)
