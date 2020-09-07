package ru.bingosoft.teploInspector.db.Orders

import androidx.room.*
import io.reactivex.Flowable


@Dao
interface OrdersDao {
    @Query("SELECT * FROM orders order by number")
    fun getAll(): Flowable<List<Orders>>

    @Query("SELECT * FROM orders WHERE id = :id")
    fun getById(id: Long): Orders

    @Query("SELECT * FROM orders WHERE number = :number")
    fun getByNumber(number: String): Flowable<Orders>

    @Query("SELECT count(*) FROM orders")
    fun getSize(): Int

    @Query("DELETE FROM orders")
    fun clearOrders()

    @Query("UPDATE orders SET questionCount=:count WHERE id = :idOrder")
    fun updateQuestionCount(idOrder: Long?, count: Int)

    @Query("UPDATE orders SET answeredCount=:count WHERE id = :idOrder")
    fun updateAnsweredCount(idOrder: Long?, count: Int)

    @Query("UPDATE orders SET techParamsCount=:count WHERE id = :idOrder")
    fun updateTechParamsCount(idOrder: Long?, count: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(orders: Orders)

    @Update
    fun update(orders: Orders)

    @Delete
    fun delete(orders: Orders)
}