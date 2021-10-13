package ru.bingosoft.teploInspector.ui.order

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObjectNotFoundException
import androidx.test.uiautomator.UiSelector
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.api.ApiService
import ru.bingosoft.teploInspector.db.AppDatabase
import ru.bingosoft.teploInspector.ui.mainactivity.MainActivity
import ru.bingosoft.teploInspector.util.Const
import ru.bingosoft.teploInspector.util.Toaster
import java.lang.Thread.sleep


@RunWith(AndroidJUnit4::class)
//@Ignore("Долгая отладка других тестов, убрать как будут разработаны остальные тесты")
class OrderListAdapterInstTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @get: Rule
    val permissionsRule = GrantPermissionRule.grant(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    lateinit var sharedPreferences: SharedPreferences
    lateinit var preferencesEditor: SharedPreferences.Editor
    lateinit var mockDb: AppDatabase
    lateinit var mockApiService: ApiService
    lateinit var orderPresenter: OrderPresenter

    @Before
    fun setUp() {
        // Проходим экран авторизации
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        sharedPreferences=targetContext.getSharedPreferences(
            Const.SharedPrefConst.APP_PREFERENCES,
            Context.MODE_PRIVATE
        )
        preferencesEditor = sharedPreferences.edit()

        mockDb= Mockito.mock(AppDatabase::class.java)
        mockApiService= Mockito.mock(ApiService::class.java)

        val toaster= Toaster(targetContext)
        orderPresenter=OrderPresenter(mockDb, mockApiService, toaster)

        //#UiAutomator #Allow_dialog
        // подробнее тут https://developer.android.com/reference/androidx/test/uiautomator/package-summary
        // примеры тут https://www.tabnine.com/code/java/methods/android.support.test.uiautomator.UiDevice/findObject
        val uiDevice= UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        if (Build.VERSION.SDK_INT >= 23) {
            val btnAllow=uiDevice.findObject(UiSelector().className("android.widget.Button").text("ALLOW"))
            if (btnAllow.exists()) {
                try {
                    btnAllow.click()
                } catch (e: UiObjectNotFoundException) {
                    Log.d("myLogs","Диалог разрешений фоновой работы приложения не загружен")
                }
            }
        }


        Log.d("myLogs", "auth_${sharedPreferences.getBoolean(Const.SharedPrefConst.IS_AUTH, false)}")
        if (!sharedPreferences.getBoolean(Const.SharedPrefConst.IS_AUTH, false)) {
            onView(withId(R.id.btnGo)).perform(click())
        }



    }

    // подробнее тут https://developer.android.com/guide/navigation/navigation-testing
    // https://stackoverflow.com/questions/28476507/using-espresso-to-click-view-inside-recyclerview-item
    // #Test #NavController #RecyclerView
    @Test
    fun testNavigationToMapFragment() {
        onView(withId(R.id.buttonOK))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
            .perform(click())

        sleep(3000)

        onView(withId(R.id.orders_recycler_view))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0,MyViewAction().clickChildViewWithId(R.id.btnRoute)))

        activityRule.scenario.onActivity {
            val navController=it.navController
            Assert.assertTrue(navController.currentDestination?.id==R.id.nav_slideshow)
        }

    }

    @Test
    fun testNavigationToCheckupFragment() {
        onView(withId(R.id.buttonOK))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
            .perform(click())

        sleep(3000)

        onView(withId(R.id.orders_recycler_view))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, MyViewAction().clickChildViewWithId(R.id.llMainCard)))

        sleep(3000)
        activityRule.scenario.onActivity {
            val navController=it.navController
            Log.d("myLogs","${navController.currentDestination?.id}")
            Log.d("myLogs","${R.id.nav_checkup}")
            Assert.assertTrue(navController.currentDestination?.id==R.id.nav_checkup)
        }

    }

}