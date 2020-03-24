package ru.bingosoft.teploInspector.db

import androidx.room.Database
import androidx.room.RoomDatabase
import ru.bingosoft.teploInspector.db.Checkup.Checkup
import ru.bingosoft.teploInspector.db.Checkup.CheckupDao
import ru.bingosoft.teploInspector.db.CheckupGuide.CheckupGuide
import ru.bingosoft.teploInspector.db.CheckupGuide.CheckupGuideDao
import ru.bingosoft.teploInspector.db.Orders.Orders
import ru.bingosoft.teploInspector.db.Orders.OrdersDao


@Database(entities=arrayOf(Orders::class, Checkup::class, CheckupGuide::class),version = 1,exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ordersDao(): OrdersDao
    abstract fun checkupDao(): CheckupDao
    abstract fun checkupGuideDao(): CheckupGuideDao
}