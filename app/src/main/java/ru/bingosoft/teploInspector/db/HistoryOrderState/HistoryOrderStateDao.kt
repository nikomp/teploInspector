package ru.bingosoft.teploInspector.db.HistoryOrderState

import androidx.room.*
import io.reactivex.Flowable

@Dao
interface HistoryOrderStateDao {
    @Query("SELECT * FROM HistoryOrderState")
    fun getAll(): Flowable<List<HistoryOrderState>>

    @Query("SELECT * FROM HistoryOrderState where idOrder=:id")
    fun getHistoryStateByIdOrder(id: Long): List<HistoryOrderState>

    @Query("DELETE FROM HistoryOrderState")
    fun clearHistory()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(history: HistoryOrderState): Long

    @Update
    fun update(history: HistoryOrderState)

    @Delete
    fun delete(history: HistoryOrderState)
}