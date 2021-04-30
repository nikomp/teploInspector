package ru.bingosoft.teploInspector.ui.order

import android.database.sqlite.SQLiteException
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.mockito.kotlin.anyOrNull
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.bingosoft.teploInspector.App
import ru.bingosoft.teploInspector.api.ApiService
import ru.bingosoft.teploInspector.db.AppDatabase
import ru.bingosoft.teploInspector.db.HistoryOrderState.HistoryOrderStateDao
import ru.bingosoft.teploInspector.db.Orders.Orders
import ru.bingosoft.teploInspector.db.Orders.OrdersDao
import ru.bingosoft.teploInspector.util.Toaster
import java.lang.Thread.sleep

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class OrderPresenterTest {
    lateinit var orderPresenter: OrderPresenter
    lateinit var mockDb: AppDatabase
    lateinit var mockView: OrderContractView
    lateinit var mockApiService: ApiService
    lateinit var mockOrdersDao: OrdersDao
    lateinit var mockHistoryOrderStateDao: HistoryOrderStateDao
    var rxJavaError="empty"


    @Before
    fun setUp() {
        // Переопределяем AndroidSchedulers.mainThread(), вместо него будет возвращаться Schedulers.trampoline из RxJava JVM
        // подробнее тут https://medium.com/@peter.tackage/overriding-rxandroid-schedulers-in-rxjava-2-5561b3d14212
        // тут https://stackoverflow.com/questions/46549405/testing-asynchronous-rxjava-code-android
        // и тут https://gist.github.com/up1/f86f43eaf629ef7e29df05686fb3ad9a
        //RxAndroidPlugins.reset()
        //RxAndroidPlugins.setInitMainThreadSchedulerHandler { TrampolineScheduler.instance() }
        //RxJavaPlugins.setIoSchedulerHandler { TrampolineScheduler.instance() }
        RxAndroidPlugins.setMainThreadSchedulerHandler { Schedulers.trampoline() }


        // Переопределяем обработчик RxJava, который гасит ошибки
        RxJavaPlugins.setErrorHandler {t ->
            //println(t.printStackTrace())
            if (t is UndeliverableException) {
                rxJavaError= t.cause.toString()
                println(rxJavaError)
            }
        }

        mockDb=mock(AppDatabase::class.java)
        mockApiService=mock(ApiService::class.java)
        mockOrdersDao=mock(OrdersDao::class.java)
        mockHistoryOrderStateDao=mock(HistoryOrderStateDao::class.java)
        val context =  ApplicationProvider.getApplicationContext<App>()
        val toaster= Toaster(context)
        orderPresenter=OrderPresenter(mockDb, mockApiService, toaster)
        mockView=mock(OrderContractView::class.java)
        orderPresenter.view=mockView
    }

    @Test
    fun testGetToaster() {
        assertNotNull(orderPresenter.toaster)
    }

    @Test
    fun testAttachView() {
        orderPresenter.attachView(mockView)
        assertNotNull(orderPresenter.view)
    }

    @Test
    fun testLoadOrders() {
        val fakeListOrders= listOf(
            Orders(id = 1, number = "A-001", status = "Открыта"),
            Orders(id = 2, number = "A-002", status = "Открыта")
        )
        val mockOrdersDao=mock(OrdersDao::class.java)
        `when`(mockDb.ordersDao()).thenReturn(mockOrdersDao)
        `when`(mockDb.ordersDao().getAll()).thenReturn(Flowable.just(fakeListOrders))
        orderPresenter.loadOrders()
        verify(orderPresenter.view)?.showOrders(fakeListOrders)
    }

    @Test
    fun testLoadOrdersWithEmptyList() {
        val emptyListOrders= listOf<Orders>()
        `when`(mockDb.ordersDao()).thenReturn(mockOrdersDao)
        `when`(mockDb.ordersDao().getAll()).thenReturn(Flowable.just(emptyListOrders))
        orderPresenter.loadOrders()
        sleep(2000)
        verify(orderPresenter.view)?.showFailure(anyInt())

    }

    @Test
    fun testAddHistoryState() {
        val fakeOrder=Orders(id = 1, status = "Открыта")
        `when`(mockDb.ordersDao()).thenReturn(mockOrdersDao)
        `when`(mockDb.historyOrderStateDao()).thenReturn(mockHistoryOrderStateDao)
        val mockUnit=mock(Unit::class.java)
        `when`(mockApiService.sendStatusOrder(anyOrNull())).thenReturn(Single.just(mockUnit))
        // try-catch не отрабатывает и не ловит ошибки
        /*try {
            orderPresenter.addHistoryState(fakeOrder)
        } catch (e: Exception) {
            fail("Не должно быть никаких Ошибок")
            println(e.printStackTrace())
        }*/

        // Пока просто запускаем метод без проверки состояний, метод должен пройти без ошибок
        orderPresenter.addHistoryState(fakeOrder)

    }

    @Test
    // Этот тест не влияет на покрытие, почему не понятно
    // Вроде кидаем thenThrow, но в имплементации в ошибку не попадаем
    fun testAddHistoryStateWithThrow() {
        val fakeOrder=Orders(id = 1, status = "Открыта")
        `when`(mockDb.ordersDao()).thenReturn(mockOrdersDao)
        `when`(mockDb.historyOrderStateDao()).thenReturn(mockHistoryOrderStateDao)
        val mockUnit=mock(Unit::class.java)
        `when`(mockApiService.sendStatusOrder(anyOrNull())).thenThrow(SQLiteException("Ошибка SQLiteException"))

        // Пока просто запускаем метод без проверки состояний, метод должен пройти без ошибок
        // чтоб заработал assertThrows см. подробнее https://stackoverflow.com/questions/59598124/android-unresolved-reference-when-using-junit-4-13
        /*val exception=assertThrows(SQLiteException::class.java) { orderPresenter.addHistoryState(
            fakeOrder
        ) }*/

        orderPresenter.addHistoryState(fakeOrder)
        // Пока так, иначе rxJavaError=empty из-за асинхронности,
        // переоперделение RxJavaPlugins.setIoSchedulerHandler вызывает ошибку lateinit var not inizialized
        sleep(2000)

        println(rxJavaError)

        assertEquals("android.database.sqlite.SQLiteException: Ошибка SQLiteException",rxJavaError)


    }

    @Test
    fun testOnDestroy() {
        orderPresenter.onDestroy()
        assertNull(orderPresenter.view)
    }

}