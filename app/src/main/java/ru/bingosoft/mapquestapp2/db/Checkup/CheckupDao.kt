package ru.bingosoft.mapquestapp2.db.Checkup

import androidx.room.*
import io.reactivex.Flowable

@Dao
interface CheckupDao {
    @Query("SELECT * FROM checkup")
    fun getAll(): Flowable<List<Checkup>>

    @Query("SELECT * FROM checkup WHERE id = :id")
    fun getById(id: Long): Checkup

    @Insert
    fun insert(orders: Checkup)

    @Update
    fun update(orders: Checkup)

    @Delete
    fun delete(orders: Checkup)
}