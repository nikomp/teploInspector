package ru.bingosoft.mapquestapp2.db.Checkup

import androidx.room.*
import io.reactivex.Flowable

@Dao
interface CheckupDao {
    @Query("SELECT * FROM checkup")
    fun getAll(): Flowable<List<Checkup>>

    @Query("SELECT * FROM checkup WHERE id = :id")
    fun getById(id: Long): Flowable<Checkup>

    @Query("SELECT * FROM checkup WHERE idOrder = :id")
    fun getCheckupsOrder(id: Long): Flowable<List<Checkup>>

    @Query("SELECT * FROM checkup where textResult is not null and sync=0 and textResult not like '%\"checked\":false%'")
    fun getResultAll(): Flowable<List<Checkup>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(checkup: Checkup)

    @Update
    fun update(checkup: Checkup)

    @Delete
    fun delete(checkup: Checkup)
}