package ru.bingosoft.teploInspector.db.Orders

import androidx.room.*
import io.reactivex.Flowable
import io.reactivex.Single


@Dao
interface OrdersDao {
    @Query("SELECT * FROM orders order by number")
    fun getAll(): Single<List<Orders>>//Flowable<List<Orders>>

    @Query("SELECT * FROM orders WHERE id = :id")
    fun getById(id: Long): Flowable<Orders>

    @Query("SELECT * FROM orders WHERE number = :number")
    fun getByNumber(number: String): Flowable<Orders>

    @Query("SELECT count(*) FROM orders")
    fun getSize(): Int

    @Query("DELETE FROM orders")
    fun clearOrders()

    @Query("DELETE FROM orders where id not in (:ids)")
    fun deleteOrders(ids:List<String>)

    @Query("UPDATE orders SET questionCount=:count WHERE id = :idOrder")
    fun updateQuestionCount(idOrder: Long?, count: Int)

    @Query("UPDATE orders SET answeredCount=:count WHERE id = :idOrder")
    fun updateAnsweredCount(idOrder: Long, count: Int)

    @Query("UPDATE orders SET techParamsCount=:count WHERE id = :idOrder")
    fun updateTechParamsCount(idOrder: Long?, count: Int)

    @Query("UPDATE orders SET addLoadCount=:count WHERE id = :idOrder")
    fun updateAddLoadCount(idOrder: Long?, count: Int)

    @Query("UPDATE orders SET dateVisit=:newDate WHERE id = :idOrder")
    fun updateDateVisit(idOrder: Long?, newDate: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE) //В этом режиме будет оставлена старая запись
    fun insert(orders: Orders)

    @Update
    fun update(orders: Orders)

    @Delete
    fun delete(orders: Orders)
}