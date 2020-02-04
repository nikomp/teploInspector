package ru.bingosoft.mapquestapp2.db

import androidx.room.Database
import androidx.room.RoomDatabase
import ru.bingosoft.mapquestapp2.db.Checkup.Checkup
import ru.bingosoft.mapquestapp2.db.Checkup.CheckupDao
import ru.bingosoft.mapquestapp2.db.Orders.Orders
import ru.bingosoft.mapquestapp2.db.Orders.OrdersDao


@Database(entities=arrayOf(Orders::class, Checkup::class),version = 1,exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ordersDao(): OrdersDao?
    abstract fun checkupDao(): CheckupDao?
}