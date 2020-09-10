package ru.bingosoft.teploInspector.db.Checkup

import androidx.room.*
import io.reactivex.Flowable
import ru.bingosoft.teploInspector.models.Models

@Dao
interface CheckupDao {
    @Query("SELECT * FROM checkup")
    fun getAll(): Flowable<List<Checkup>>

    @Query("SELECT * FROM checkup WHERE id = :id")
    fun getById(id: Long): Flowable<Checkup>

    @Query("SELECT * FROM checkup WHERE idOrder = :id")
    fun getCheckupsOrder(id: Long): Flowable<List<Checkup>>

    @Query("SELECT * FROM checkup WHERE idOrder = :id")
    fun getCheckupByOrder(id: Long): Checkup

    @Query("SELECT id FROM checkup WHERE idOrder = :id")
    fun getCheckupIdByOrder(id: Long): Long

    @Query("SELECT * FROM checkup where textResult is not null and sync=0 and textResult not like '%\"checked\":false%'")
    fun getResultAll(): Flowable<List<Checkup>>

    /*@Query("SELECT idOrder as id_order, textResult as controls from Checkup \n" +
            "where textResult is not null and sync=0")*/
    @Query("SELECT c.idOrder id_order, c.textResult as controls,\n" +
            "'['||group_concat('{\"unique_id\":'||h.id||', \"idOrder\":'||h.idOrder||', \"stateOrder\":\"'||h.stateOrder||'\", \"dateChange\":'||h.dateChange||'}')||']' as history_order_state from Checkup c\n" +
            "left join HistoryOrderState h on h.idOrder=c.idOrder\n" +
            "where textResult is not null and sync=0")
    fun getResultAll2(): Flowable<List<Models.Result>>

    @Query("SELECT c.idOrder id_order, c.textResult as controls,'['||group_concat('{\"unique_id\":'||h.id||', \"idOrder\":'||h.idOrder||', \"stateOrder\":\"'||h.stateOrder||'\", \"dateChange\":'||h.dateChange||'}')||']' as history_order_state from Checkup c\n" +
            "left join HistoryOrderState h on h.idOrder=c.idOrder \n" +
            "where textResult is not null and c.idOrder=:id")
    fun getResultByOrderId(id: Long): Flowable<List<Models.Result>>

    @Query("SELECT count(*) FROM checkup where textResult is not null and sync=0")
    fun existCheckupWithResult(): Int

    @Query("Update checkup set sync=1 where idOrder=:id")
    fun updateSync(id:Int)

    @Query("DELETE FROM checkup")
    fun clearCheckup()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(checkup: Checkup)

    @Update
    fun update(checkup: Checkup)

    @Delete
    fun delete(checkup: Checkup)
}