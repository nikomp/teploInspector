package ru.bingosoft.teploInspector.db.AddLoad

import androidx.room.*

@Dao
interface AddLoadDao {
    //@Query("SELECT * FROM addload WHERE idOrder = :id")
    @Query("SELECT * FROM addload WHERE idOrder = :id\n" +
            "order by \n" +
            "system_consumption,\n" +
            "purpose,\n" +
            "code,\n" +
            "contractor is not null")
    fun getAddLoadOrder(id: Long): List<AddLoad>

    @Query("DELETE FROM addload")
    fun clearAddLoad()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(addload: AddLoad)

    @Update
    fun update(addload: AddLoad)

    @Delete
    fun delete(addload: AddLoad)
}