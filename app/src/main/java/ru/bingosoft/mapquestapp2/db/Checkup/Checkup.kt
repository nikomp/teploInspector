package ru.bingosoft.mapquestapp2.db.Checkup

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey
import ru.bingosoft.mapquestapp2.db.Orders.Orders

@Entity(tableName = "Checkup",
        foreignKeys = arrayOf(ForeignKey(entity = Orders::class, parentColumns = ["id"], childColumns = ["idOrder"], onDelete = CASCADE)),
        indices = [Index("idOrder")]
    )
data class Checkup (
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var guid: String,
    var text: String,
    var idOrder: Long? =null
)
