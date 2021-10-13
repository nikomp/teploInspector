package ru.bingosoft.teploInspector.ui.checkup

import android.os.Build
import android.os.Looper.getMainLooper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import ru.bingosoft.teploInspector.App
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.db.TechParams.TechParams
import ru.bingosoft.teploInspector.ui.mainactivity.MainActivity


@RunWith(RobolectricTestRunner::class)
// Отдельный DI модуль TestApplication и компонент это здорово, но возникает проблема с AndroidInjector,
// Поэтому для тестирования использую базовый DI модуль. В модуле добавил @VisibleForTesting для решения проблемы (Error#1)
@Config(application= App::class, sdk = [Build.VERSION_CODES.O_MR1])
@Ignore("Долгая отладка других тестов, убрать как будут разработаны остальные тесты")
// Error#1 InflateException: Binary XML file line #20: Error inflating class com.google.android.material.textfield.TextInputLayout
@LooperMode(LooperMode.Mode.PAUSED)
class TechnicalCharacteristicsTest {

    private lateinit var testScheduler: TestScheduler
    private lateinit var technicalCharacteristics: TechnicalCharacteristics
    private lateinit var fakeRootView: View
    private lateinit var checkupFragment: CheckupFragment
    var fakeListTechCh= listOf<TechParams>()

    @Before
    fun setUp() {

        testScheduler= TestScheduler()
        RxJavaPlugins.setComputationSchedulerHandler { testScheduler }
        RxAndroidPlugins.setMainThreadSchedulerHandler { Schedulers.trampoline()}

        val context= ApplicationProvider.getApplicationContext<App>()

        val parentView= LinearLayout(context)
        fakeRootView = LayoutInflater.from(context).inflate(
            R.layout.item_cardview_step,
            parentView as ViewGroup,
            false
        )


        fakeListTechCh= listOf(TechParams(id=1,idOrder = 1,node="Узел1"),
            TechParams(id=1,idOrder = 1,node="Узел1",long_group = "Группа1#Подгруппа1"),
            TechParams(id=1,idOrder = 1,node="Узел1",long_group = "",short_group = "Группа1"),
            TechParams(id=1,idOrder = 1,node="Узел1",long_group = "",short_group = ""),
            TechParams(id=1,idOrder = 1,long_group = "",short_group = ""),
            TechParams(id=1,idOrder = 1,long_group = "",short_group = "Группа1")
        )

        ActivityScenario.launch(MainActivity::class.java).onActivity {
            checkupFragment=CheckupFragment()
            it.supportFragmentManager
                .beginTransaction()
                .add(checkupFragment,"CheckupFragment_tag")
                .commit()
            technicalCharacteristics=TechnicalCharacteristics(checkupFragment,fakeListTechCh)
            shadowOf(getMainLooper()).idle()
        }
    }



    @Test
    fun testCreate() {
        Assert.assertTrue(fakeRootView.findViewById<LinearLayout>(R.id.llMain).childCount == 0)
        //println(fakeRootView.findViewById<LinearLayout>(R.id.llMain).childCount)
        // Проверяем, что после вызова метода в контейнер были добавлены вьюхи
        technicalCharacteristics.create(fakeRootView)

        //println(fakeRootView.findViewById<LinearLayout>(R.id.llMain).childCount)
        Assert.assertTrue(fakeRootView.findViewById<LinearLayout>(R.id.llMain).childCount > 0)
    }

    @Test
    fun testClTitleClick() {

        technicalCharacteristics.create(fakeRootView)

        val llGroup=fakeRootView.findViewById<LinearLayout>(R.id.containerTh)
        var beforeVisibility=llGroup.visibility
        Assert.assertTrue(beforeVisibility == 8) //visibility==GONE
        val clTitle=fakeRootView.findViewById<ConstraintLayout>(R.id.titleGroup)
        Assert.assertNotNull(clTitle)
        clTitle.performClick()
        Assert.assertTrue(llGroup.visibility == 0)
        Assert.assertTrue(beforeVisibility != llGroup.visibility)
        beforeVisibility=llGroup.visibility
        clTitle.performClick()
        shadowOf(getMainLooper()).idle()
        Assert.assertTrue(llGroup.visibility == 8)
        Assert.assertTrue(beforeVisibility != llGroup.visibility)
    }

    @Test
    fun testSaveUI() {
        // Проверяем, что после вызова метода в контейнер были добавлены вьюхи
        println(checkupFragment.llMainUiTX.size)
        technicalCharacteristics.create(fakeRootView)
        technicalCharacteristics.saveUI()
        println(checkupFragment.llMainUiTX.size)
    }


}