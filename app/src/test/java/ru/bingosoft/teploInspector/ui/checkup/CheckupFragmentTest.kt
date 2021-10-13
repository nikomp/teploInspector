package ru.bingosoft.teploInspector.ui.checkup

import android.os.Build
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import ru.bingosoft.teploInspector.App
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.db.AppDatabase
import ru.bingosoft.teploInspector.db.Checkup.Checkup
import ru.bingosoft.teploInspector.db.Checkup.CheckupDao
import ru.bingosoft.teploInspector.db.Orders.Orders
import ru.bingosoft.teploInspector.db.TechParams.TechParams
import ru.bingosoft.teploInspector.ui.mainactivity.MainActivity
import java.lang.Thread.sleep

@RunWith(RobolectricTestRunner::class)
@Config(application= App::class, sdk = [Build.VERSION_CODES.O_MR1])
@Ignore("Долгая отладка других тестов, убрать как будут разработаны остальные тесты")
@LooperMode(LooperMode.Mode.PAUSED)
class CheckupFragmentTest {

    private lateinit var testScheduler: TestScheduler
    private lateinit var technicalCharacteristics: TechnicalCharacteristics
    private lateinit var fakeRootView: View
    private lateinit var checkupFragment: CheckupFragment
    private lateinit var scenario: ActivityScenario<MainActivity>
    var fakeListTechCh= listOf<TechParams>()
    lateinit var mockCheckUpPresenter: CheckupPresenter
    lateinit var mockDb: AppDatabase
    private lateinit var mockView: CheckupContractView
    var rxJavaError="empty"
    private lateinit var fakeCheckupPresenter: CheckupPresenter
    private lateinit var mockCheckupDao: CheckupDao


    @Before
    fun setUp() {
        testScheduler= TestScheduler()
        RxJavaPlugins.setComputationSchedulerHandler { testScheduler }
        RxAndroidPlugins.setMainThreadSchedulerHandler { Schedulers.trampoline()}

        // Переопределяем обработчик RxJava, который гасит ошибки
        RxJavaPlugins.setErrorHandler {t ->
            //println(t.printStackTrace())
            if (t is UndeliverableException) {
                rxJavaError= t.cause.toString()
                //println(rxJavaError)
            }
        }

        mockCheckUpPresenter=mock(CheckupPresenter::class.java)
        mockDb=mock(AppDatabase::class.java)
        mockCheckupDao=mock(CheckupDao::class.java)
        mockView= mock(CheckupContractView::class.java)
        fakeCheckupPresenter= CheckupPresenter(mockDb)

    }

    @Test
    fun testFillOrderData() {
        val fakeCurrentOrder=Orders(id=1)
        val fakeCheckup=Checkup(idOrder = 1,numberOrder = "A-001",orderGuid = "fakeGuid",typeOrder = "Шум")
        `when`(mockDb.checkupDao()).thenReturn(mockCheckupDao)
        //`when`(mockCheckupDao.getCheckupByOrder(anyLong())).thenReturn(null)
        //doNothing().`when`(mockCheckupDao).getCheckupByOrder(anyLong())
        //doReturn(fakeCheckup).`when`(mockCheckupDao).getCheckupByOrder(anyLong())


        //checkupPresenter.loadCheckupByOrder(1)
        //sleep(1000)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity: MainActivity ->
                activity.currentOrder=fakeCurrentOrder
                checkupFragment=CheckupFragment()
                checkupFragment.checkupPresenter=mockCheckUpPresenter
                val spyCH=spy(checkupFragment)

                //val mockCh=mock(CheckupFragment::class.java)
                `when`(spyCH.checkupPresenter).thenReturn(mockCheckUpPresenter)
                `when`(mockCheckUpPresenter.db).thenReturn(mockDb)
                `when`(mockDb.checkupDao()).thenReturn(mockCheckupDao)
                `when`(mockCheckupDao.getCheckupByOrder(anyLong())).thenReturn(fakeCheckup)

                println("GHFHGF")

                val ft=activity.supportFragmentManager
                    .beginTransaction()
                    .add(checkupFragment,"CheckupFragment_tag222")
                    .commitNow()

                sleep(2000)

                println(activity.supportFragmentManager.findFragmentByTag("CheckupFragment_tag222"))
                Assert.assertNotNull(activity.supportFragmentManager.findFragmentByTag("CheckupFragment_tag222"))

            }
        }

    }


    @Test
    fun testStepsAdapterNotEmpty() {
        val checkupFragment=CheckupFragment()
        val activityController=Robolectric.buildActivity(MainActivity::class.java)
        activityController.create().start().resume()
        activityController.get()
            .supportFragmentManager
            .beginTransaction()
            .add(checkupFragment,"CheckupFragment_tag222")
            .commitNow()

        val recycler=checkupFragment.view!!.findViewById<RecyclerView>(R.id.steps_recycler_view)
        recycler.measure(0,0)
        recycler.layout(0,0,100,1000)

        /*val rcView=recycler.findViewHolderForAdapterPosition(0)?.itemView
        rcView?.performClick()

        shadowOf(Looper.getMainLooper()).idle()
        println(recycler)
        println(recycler.adapter)*/
        Assert.assertTrue(recycler.adapter?.itemCount!! >0)
    }
}