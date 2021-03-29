package ru.bingosoft.teploInspector.db

import androidx.room.Database
import androidx.room.RoomDatabase
import ru.bingosoft.teploInspector.db.AddLoad.AddLoad
import ru.bingosoft.teploInspector.db.AddLoad.AddLoadDao
import ru.bingosoft.teploInspector.db.Checkup.Checkup
import ru.bingosoft.teploInspector.db.Checkup.CheckupDao
import ru.bingosoft.teploInspector.db.CheckupGuide.CheckupGuide
import ru.bingosoft.teploInspector.db.CheckupGuide.CheckupGuideDao
import ru.bingosoft.teploInspector.db.HistoryOrderState.HistoryOrderState
import ru.bingosoft.teploInspector.db.HistoryOrderState.HistoryOrderStateDao
import ru.bingosoft.teploInspector.db.Orders.Orders
import ru.bingosoft.teploInspector.db.Orders.OrdersDao
import ru.bingosoft.teploInspector.db.TechParams.TechParams
import ru.bingosoft.teploInspector.db.TechParams.TechParamsDao
import ru.bingosoft.teploInspector.db.User.TrackingUserLocation
import ru.bingosoft.teploInspector.db.User.TrackingUserLocationDao


@Database(entities=arrayOf(Orders::class, Checkup::class, CheckupGuide::class, TrackingUserLocation::class, TechParams::class, AddLoad::class, HistoryOrderState::class),version = 1,exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ordersDao(): OrdersDao
    abstract fun checkupDao(): CheckupDao
    abstract fun checkupGuideDao(): CheckupGuideDao
    abstract fun trackingUserDao(): TrackingUserLocationDao
    abstract fun techParamsDao(): TechParamsDao
    abstract fun addLoadDao(): AddLoadDao
    abstract fun historyOrderStateDao(): HistoryOrderStateDao
}