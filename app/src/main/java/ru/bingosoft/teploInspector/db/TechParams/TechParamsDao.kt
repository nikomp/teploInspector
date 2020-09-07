package ru.bingosoft.teploInspector.db.TechParams

import androidx.room.*

@Dao
interface TechParamsDao {

    @Query("SELECT * FROM techparams WHERE idOrder = :id")
    fun getTechParamsOrder(id: Long): List<TechParams>

    @Query("DELETE FROM techparams")
    fun clearTechParams()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(techParams: TechParams)

    @Update
    fun update(techParams: TechParams)

    @Delete
    fun delete(techParams: TechParams)
}