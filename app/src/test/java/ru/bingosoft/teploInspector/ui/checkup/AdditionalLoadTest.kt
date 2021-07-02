package ru.bingosoft.teploInspector.ui.checkup

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.test.core.app.ApplicationProvider
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.TestApplication
import ru.bingosoft.teploInspector.db.AddLoad.AddLoad

@RunWith(RobolectricTestRunner::class)
@Config(application=TestApplication::class, sdk = [Build.VERSION_CODES.O_MR1])
@Ignore("Долгая отладка других тестов, убрать как будут разработаны остальные тесты")
//InflateException: Binary XML file line #20: Error inflating class com.google.android.material.textfield.TextInputLayout
class AdditionalLoadTest {

    private lateinit var testScheduler: TestScheduler
    private lateinit var additionalLoad: AdditionalLoad
    private lateinit var fakeRootView: View
    private lateinit var fakeListAddLoad: List<AddLoad>
    lateinit var mockListAddLoad: List<AddLoad>

    @Before
    fun setUp() {

        testScheduler= TestScheduler()
        RxJavaPlugins.setComputationSchedulerHandler { testScheduler }
        RxAndroidPlugins.setMainThreadSchedulerHandler { Schedulers.trampoline()}

        //val context =  ApplicationProvider.getApplicationContext<TestApplication>()
        val context=ApplicationProvider.getApplicationContext<TestApplication>()

        fakeListAddLoad= listOf(AddLoad(purpose = "Назначение1",code = 1,system_consumption="Система потребления1"),
            AddLoad(purpose = "Назначение2",code = 2,system_consumption="Система потребления2"))
        //mockListAddLoad=mock(List::class.java) as List<AddLoad>

        //mockRootView=mock(View::class.java)
        //fakeRootView=View(context)
        val parentView=LinearLayout(context)
        fakeRootView = LayoutInflater.from(context).inflate(
            R.layout.item_cardview_step,
            parentView as ViewGroup,
            false
        )
        additionalLoad=AdditionalLoad(fakeListAddLoad, fakeRootView)
    }

    @Test
    fun testGetCtx() {
        assertNotNull(additionalLoad.ctx)
    }

    /*@Test
    fun testCreate() {
        //`when`(fakeRootView.parent).thenReturn(null)
        val spyList=spy(fakeListAddLoad)
        //`when`(mockListAddLoad.forEach(anyOrNull())).thenReturn(a)
        //doNothing().`when`(mockListAddLoad).forEach(anyOrNull())

        additionalLoad.create()
        //verify(mockListAddLoad).forEach(anyOrNull())
        //verify(spyList).forEach(anyOrNull())

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity: MainActivity ->

                verify(spyList).forEach(anyOrNull())
            }
        }

    }*/
}