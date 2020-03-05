package ru.bingosoft.mapquestapp2.db.CheckupGuide

import androidx.room.*
import io.reactivex.Flowable

@Dao
interface CheckupGuideDao {
    @Query("SELECT * FROM CheckupGuide")
    fun getAll(): Flowable<List<CheckupGuide>>

    @Query("SELECT * FROM CheckupGuide WHERE id = :id")
    fun getById(id: Long): Flowable<CheckupGuide>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(guide: CheckupGuide)

    @Update
    fun update(guide: CheckupGuide)

    @Delete
    fun delete(guide: CheckupGuide)
}