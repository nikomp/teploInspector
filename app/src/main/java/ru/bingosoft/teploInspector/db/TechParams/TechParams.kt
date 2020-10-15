package ru.bingosoft.teploInspector.db.TechParams

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import ru.bingosoft.teploInspector.db.Orders.Orders

@Entity(tableName = "TechParams",
    foreignKeys = [ForeignKey(
        entity = Orders::class,
        parentColumns = ["id"],
        childColumns = ["idOrder"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("idOrder")]
)
data class TechParams (
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    @SerializedName("tech_char_guid")
    var guid: String="",
    @SerializedName("order_id")
    var idOrder: Long,
    @SerializedName("technical_characteristic")
    var technical_characteristic: String = "",
    @SerializedName("val")
    var value: String? = "",
    @SerializedName("node")
    var node: String? = null
) {
    override fun toString(): String {
        return "TechParams(id=$id, guid='$guid', idOrder=$idOrder, technical_characteristic='$technical_characteristic', value='$value', node=$node)"
    }
}