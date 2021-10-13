package ru.bingosoft.teploInspector.ui.checkup

import android.os.Build
import android.os.Looper.getMainLooper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.test.core.app.ApplicationProvider
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
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

    @Before
    fun setUp() {

        testScheduler= TestScheduler()
        RxJavaPlugins.setComputationSchedulerHandler { testScheduler }
        RxAndroidPlugins.setMainThreadSchedulerHandler { Schedulers.trampoline()}

        val context=ApplicationProvider.getApplicationContext<TestApplication>()

        fakeListAddLoad= listOf(AddLoad(purpose = "Назначение1",code = 1,system_consumption="Система потребления1"),
            AddLoad(purpose = "Назначение2",code = 2,system_consumption=""),
            AddLoad(purpose = "Назначение2",code = 2,system_consumption="",contractor = "Контрагент"),
            AddLoad(purpose = "",code = 2,system_consumption="",contractor = "Контрагент"),
            AddLoad(purpose = "",code = 2,system_consumption=""))

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

    @Test
    fun testCreate() {
        assertTrue(fakeRootView.findViewById<LinearLayout>(R.id.llMain).childCount==0)
        // Проверяем, что после вызова метода в контейнер были добавлены вьюхи
        additionalLoad.create()
        assertTrue(fakeRootView.findViewById<LinearLayout>(R.id.llMain).childCount>0)
    }

    @Test
    fun testClTitleClick() {


        additionalLoad.create()

        val llGroup=fakeRootView.findViewById<LinearLayout>(R.id.containerTh)
        var beforeVisibility=llGroup.visibility
        assertTrue(beforeVisibility==8) //visibility==GONE
        val clTitle=fakeRootView.findViewById<ConstraintLayout>(R.id.titleGroup)
        assertNotNull(clTitle)
        clTitle.performClick()
        assertTrue(llGroup.visibility==0)
        assertTrue(beforeVisibility!=llGroup.visibility)
        beforeVisibility=llGroup.visibility
        clTitle.performClick()
        shadowOf(getMainLooper()).idle()
        assertTrue(llGroup.visibility==8)
        assertTrue(beforeVisibility!=llGroup.visibility)
    }


}