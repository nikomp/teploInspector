package ru.bingosoft.teploInspector.db.HistoryOrderState

import androidx.room.*
import io.reactivex.Flowable

@Dao
interface HistoryOrderStateDao {
    @Query("SELECT * FROM HistoryOrderState")
    fun getAll(): Flowable<List<HistoryOrderState>>

    @Query("SELECT * FROM HistoryOrderState where idOrder=:id")
    fun getHistoryStateByIdOrder(id: Long): List<HistoryOrderState>

    @Query("select case when numState>1 then \n" +
            "(select stateOrder from HistoryOrderState\n" +
            "\twhere idOrder=:id\n" +
            "\torder by id DESC\n" +
            "\tlimit 1,1) else 'Открыта' END as stateOrder\n" +
            "from (\n" +
            "\tselect count() as numState from HistoryOrderState\n" +
            "\twhere idOrder=:id\n" +
            ")")
    fun getPreviousStateByIdOrder(id: Long): String

    @Query("DELETE FROM HistoryOrderState")
    fun clearHistory()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(history: HistoryOrderState): Long

    @Update
    fun update(history: HistoryOrderState)

    @Delete
    fun delete(history: HistoryOrderState)
}