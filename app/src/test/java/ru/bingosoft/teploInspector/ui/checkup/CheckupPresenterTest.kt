package ru.bingosoft.teploInspector.ui.checkup

import android.os.Build
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.mockito.kotlin.anyOrNull
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.db.AddLoad.AddLoad
import ru.bingosoft.teploInspector.db.AddLoad.AddLoadDao
import ru.bingosoft.teploInspector.db.AppDatabase
import ru.bingosoft.teploInspector.db.Checkup.Checkup
import ru.bingosoft.teploInspector.db.Checkup.CheckupDao
import ru.bingosoft.teploInspector.db.Orders.OrdersDao
import ru.bingosoft.teploInspector.db.TechParams.TechParams
import ru.bingosoft.teploInspector.db.TechParams.TechParamsDao
import ru.bingosoft.teploInspector.models.Models
import ru.bingosoft.teploInspector.util.UICreator
import java.lang.Thread.sleep

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
@Ignore("Долгая отладка других тестов, убрать как будут разработаны остальные тесты")
class CheckupPresenterTest {
    private lateinit var checkupPresenter: CheckupPresenter
    private lateinit var mockDb: AppDatabase
    private lateinit var mockView: CheckupContractView
    private lateinit var mockCheckupDao: CheckupDao
    private lateinit var mockTechParamsDao: TechParamsDao
    private lateinit var mockAddLoadDao: AddLoadDao
    private lateinit var mockOrdersDao: OrdersDao

    private lateinit var testScheduler: TestScheduler

    @Before
    fun setUp() {
        testScheduler= TestScheduler()
        RxJavaPlugins.setComputationSchedulerHandler { testScheduler }
        RxAndroidPlugins.setMainThreadSchedulerHandler { Schedulers.trampoline()}

        mockDb=mock(AppDatabase::class.java)
        mockCheckupDao=mock(CheckupDao::class.java)
        mockTechParamsDao=mock(TechParamsDao::class.java)
        mockAddLoadDao=mock(AddLoadDao::class.java)
        mockOrdersDao=mock(OrdersDao::class.java)
        mockView= mock(CheckupContractView::class.java)
        checkupPresenter= CheckupPresenter(mockDb)
        checkupPresenter.view=mockView
    }

    @Test
    fun testAttachView() {
        checkupPresenter.attachView(mockView)
        Assert.assertNotNull(checkupPresenter.view)
    }

    @Test
    fun testGetDb() {
        Assert.assertNotNull(checkupPresenter.db)
    }

    @Test
    fun testLoadCheckupByOrder() {
        val fakeCheckup=Checkup(idOrder = 1,numberOrder = "A-001",orderGuid = "fakeGuid",typeOrder = "Шум")
        `when`(mockDb.checkupDao()).thenReturn(mockCheckupDao)
        `when`(mockCheckupDao.getCheckupByOrder(anyLong())).thenReturn(fakeCheckup)

        checkupPresenter.loadCheckupByOrder(1)
        sleep(1000)

        verify(mockView).dataIsLoaded(fakeCheckup)

        checkupPresenter.view=null
        checkupPresenter.loadCheckupByOrder(1)
        sleep(1000)

        verify(mockView).dataIsLoaded(fakeCheckup)


    }

    @Test
    fun testLoadCheckupByOrderWithError() {
        `when`(mockDb.checkupDao()).thenReturn(mockCheckupDao)
        `when`(mockCheckupDao.getCheckupByOrder(anyLong())).thenThrow(RuntimeException())

        checkupPresenter.loadCheckupByOrder(1)
        sleep(1000)
        verify(mockView).errorReceived(anyOrNull())

        checkupPresenter.view=null
        checkupPresenter.loadCheckupByOrder(1)
        sleep(1000)

        verify(mockView).errorReceived(anyOrNull())


    }

    @Test
    fun testGetTechParams() {
        val fakeListTechParams= listOf<TechParams>(TechParams(idOrder = 1))
        `when`(mockDb.techParamsDao()).thenReturn(mockTechParamsDao)
        `when`(mockTechParamsDao.getTechParamsOrder(anyLong())).thenReturn(fakeListTechParams)

        checkupPresenter.getTechParams(1)
        sleep(1000)

        verify(mockView).techParamsLoaded(fakeListTechParams)

        checkupPresenter.view=null
        checkupPresenter.getTechParams(1)
        sleep(1000)

        verify(mockView).techParamsLoaded(fakeListTechParams)


    }

    @Test
    fun testGetTechParamsWithError() {
        `when`(mockDb.techParamsDao()).thenReturn(mockTechParamsDao)
        `when`(mockTechParamsDao.getTechParamsOrder(anyLong())).thenThrow(RuntimeException())

        checkupPresenter.getTechParams(1)
        sleep(1000)

        verify(mockView).errorReceived(anyOrNull())

        checkupPresenter.view=null
        checkupPresenter.getTechParams(1)
        sleep(1000)

        verify(mockView).errorReceived(anyOrNull())


    }

    @Test
    fun testGetAddLoads() {
        val fakeListAddLoad= listOf<AddLoad>(AddLoad(purpose = ""))
        `when`(mockDb.addLoadDao()).thenReturn(mockAddLoadDao)
        `when`(mockAddLoadDao.getAddLoadOrder(anyLong())).thenReturn(fakeListAddLoad)

        checkupPresenter.getAddLoads(1)
        sleep(1000)

        verify(mockView).addLoadsLoaded(fakeListAddLoad)

        checkupPresenter.view=null
        checkupPresenter.getAddLoads(1)
        sleep(1000)

        verify(mockView).addLoadsLoaded(fakeListAddLoad)


    }

    @Test
    fun testGetAddLoadsWithError() {
        `when`(mockDb.addLoadDao()).thenReturn(mockAddLoadDao)
        `when`(mockAddLoadDao.getAddLoadOrder(anyLong())).thenThrow(RuntimeException())

        checkupPresenter.getAddLoads(1)
        sleep(1000)

        verify(mockView).errorReceived(anyOrNull())

        checkupPresenter.view=null
        checkupPresenter.getAddLoads(1)
        sleep(1000)

        verify(mockView).errorReceived(anyOrNull())


    }

    @Test
    fun testSaveCheckup() {
        val mockUiCreator=mock(UICreator::class.java)
        val mockCheckup=mock(Checkup::class.java)
        `when`(mockDb.checkupDao()).thenReturn(mockCheckupDao)
        `when`(mockUiCreator.checkup).thenReturn(mockCheckup)

        doNothing().`when`(mockCheckupDao).update(mockCheckup)
        val fakeControlList= mutableListOf(Models.TemplateControl(answered = true),Models.TemplateControl(answered = false))
        `when`(mockUiCreator.controlList).thenReturn(fakeControlList)
        `when`(mockDb.ordersDao()).thenReturn(mockOrdersDao)
        doNothing().`when`(mockOrdersDao).updateAnsweredCount(anyOrNull(), anyInt())


        checkupPresenter.saveCheckup(mockUiCreator)
        sleep(1000)
        verify(mockView).showCheckupMessage(R.string.msgSaveCheckup)
        verify(mockView).setAnsweredCount(anyInt())

        checkupPresenter.view=null
        checkupPresenter.saveCheckup(mockUiCreator)
        sleep(1000)
        verify(mockView).showCheckupMessage(R.string.msgSaveCheckup)
        verify(mockView).setAnsweredCount(anyInt())
    }

    @Test
    fun testSaveCheckupWithFalseSend() {
        val mockUiCreator=mock(UICreator::class.java)
        val mockCheckup=mock(Checkup::class.java)
        `when`(mockDb.checkupDao()).thenReturn(mockCheckupDao)
        `when`(mockUiCreator.checkup).thenReturn(mockCheckup)

        doNothing().`when`(mockCheckupDao).update(mockCheckup)
        `when`(mockDb.ordersDao()).thenReturn(mockOrdersDao)
        doNothing().`when`(mockOrdersDao).updateAnsweredCount(anyOrNull(), anyInt())

        checkupPresenter.saveCheckup(mockUiCreator,false)
        sleep(1000)
        verify(mockView, times(0)).showCheckupMessage(R.string.msgSaveCheckup)
        verify(mockView).setAnsweredCount(anyInt())
    }

    @Test
    fun testSaveCheckupWithError() {
        val mockUiCreator=mock(UICreator::class.java)
        val mockCheckup=mock(Checkup::class.java)
        `when`(mockDb.checkupDao()).thenReturn(mockCheckupDao)
        `when`(mockUiCreator.checkup).thenReturn(mockCheckup)

        `when`(mockCheckupDao.update(mockCheckup)).thenThrow(RuntimeException())

        checkupPresenter.saveCheckup(mockUiCreator,false)
        sleep(1000)
        verify(mockView).errorReceived(anyOrNull())

        checkupPresenter.view=null
        checkupPresenter.saveCheckup(mockUiCreator,false)
        sleep(1000)
        verify(mockView).errorReceived(anyOrNull())
    }

    @Test
    fun testSaveCheckupWithErrorInUpdateAnswered() {
        val mockUiCreator=mock(UICreator::class.java)
        val mockCheckup=mock(Checkup::class.java)
        `when`(mockDb.checkupDao()).thenReturn(mockCheckupDao)
        `when`(mockUiCreator.checkup).thenReturn(mockCheckup)

        doNothing().`when`(mockCheckupDao).update(mockCheckup)
        `when`(mockDb.ordersDao()).thenReturn(mockOrdersDao)
        `when`(mockOrdersDao.updateAnsweredCount(anyOrNull(), anyInt())).thenThrow(RuntimeException())

        checkupPresenter.saveCheckup(mockUiCreator)
        sleep(1000)
        verify(mockView).errorReceived(anyOrNull())

        checkupPresenter.view=null
        checkupPresenter.saveCheckup(mockUiCreator)
        sleep(1000)
        verify(mockView).errorReceived(anyOrNull())
    }

    @Test
    fun testOnDestroy() {
        checkupPresenter.onDestroy()
        Assert.assertNull(checkupPresenter.view)
    }
}