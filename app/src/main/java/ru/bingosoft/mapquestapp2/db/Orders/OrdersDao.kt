package ru.bingosoft.mapquestapp2.db.Orders

import androidx.room.*
import io.reactivex.Flowable


@Dao
interface OrdersDao {
    @Query("SELECT * FROM orders")
    fun getAll(): Flowable<List<Orders>>

    @Query("SELECT * FROM orders WHERE id = :id")
    fun getById(id: Long): Orders

    @Query("SELECT count(*) FROM orders")
    fun getSize(): Int

    @Query("DELETE FROM orders")
    fun clearOrders()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(orders: Orders)

    @Update
    fun update(orders: Orders)

    @Delete
    fun delete(orders: Orders)
}