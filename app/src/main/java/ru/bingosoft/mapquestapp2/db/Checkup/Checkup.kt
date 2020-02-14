package ru.bingosoft.mapquestapp2.db.Checkup

import androidx.room.*
import androidx.room.ForeignKey.CASCADE
import com.google.gson.JsonObject
import ru.bingosoft.mapquestapp2.db.Orders.Orders
import ru.bingosoft.mapquestapp2.util.JsonConverter

@Entity(tableName = "Checkup",
        foreignKeys = arrayOf(ForeignKey(entity = Orders::class, parentColumns = ["id"], childColumns = ["idOrder"], onDelete = CASCADE)),
        indices = [Index("idOrder")]
    )
@TypeConverters(JsonConverter::class)
data class Checkup (
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var guid: String,
    var text: JsonObject,
    var idOrder: Long? =null,
    var textResult: JsonObject? = null
)
