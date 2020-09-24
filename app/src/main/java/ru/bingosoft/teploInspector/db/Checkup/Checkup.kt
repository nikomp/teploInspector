package ru.bingosoft.teploInspector.db.Checkup

import androidx.room.*
import androidx.room.ForeignKey.CASCADE
import com.google.gson.JsonArray
import com.google.gson.annotations.SerializedName
import ru.bingosoft.teploInspector.db.Orders.Orders
import ru.bingosoft.teploInspector.util.JsonArrayConverter

@Entity(tableName = "Checkup",
        foreignKeys = arrayOf(
            ForeignKey(
                entity = Orders::class,
                parentColumns = ["id"],
                childColumns = ["idOrder"],
                onDelete = CASCADE)),
        indices = [Index("idOrder")]
    )
@TypeConverters(JsonArrayConverter::class)
data class Checkup (
    @PrimaryKey(autoGenerate = true)
    //var id: Long = 0,
    @SerializedName("id_order")
    var idOrder: Long =0,
    @SerializedName("number_order")
    var numberOrder: String,
    @SerializedName("order_guid")
    var orderGuid: String,
    @SerializedName("type_order")
    var typeOrder: String="",
    @SerializedName("controls")
    var text: JsonArray? = null,
    var textResult: JsonArray? = null,
    var sync: Boolean = false
) {

    /**
     * Возвращает сотроку с описанием чеклиста. Поля могут меняться
     */
    override fun toString(): String {
        return "Checkup(idOrder=$idOrder, numberOrder='$numberOrder', orderGuid='$orderGuid', typeOrder='$typeOrder', text=$text, textResult=$textResult, sync=$sync)"
    }


}
