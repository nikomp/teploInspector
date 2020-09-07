package ru.bingosoft.teploInspector.db.HistoryOrderState

import androidx.room.*
import androidx.room.ForeignKey.CASCADE
import com.google.gson.annotations.SerializedName
import ru.bingosoft.teploInspector.db.Orders.Orders
import ru.bingosoft.teploInspector.util.DateConverter
import java.util.*

@Entity(tableName = "HistoryOrderState",
    foreignKeys = arrayOf(
        ForeignKey(
            entity = Orders::class,
            parentColumns = ["id"],
            childColumns = ["idOrder"],
            onDelete = CASCADE)),
    indices = [Index("idOrder")]
)

@TypeConverters(DateConverter::class)
data class HistoryOrderState (
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    @SerializedName("id_order")
    var idOrder: Long? =null,
    @SerializedName("state_order")
    var stateOrder: String="",
    @SerializedName("date")
    var dateChange: Date=Date()
)